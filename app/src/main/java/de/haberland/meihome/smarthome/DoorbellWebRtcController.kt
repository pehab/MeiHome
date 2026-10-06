package de.haberland.meihome.smarthome

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

class DoorbellWebRtcController(
    context: Context,
    private val client: GoogleSdmDoorbellClient,
    private val scope: CoroutineScope,
    private val onStatusChanged: (DoorbellStreamStatus) -> Unit,
) {
    private val appContext = context.applicationContext
    private val eglBase = EglBase.create()
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var remoteAudioTrack: AudioTrack? = null
    private var renderer: SurfaceViewRenderer? = null
    private var mediaSessionId: String? = null
    private var refreshJob: Job? = null
    private var started = false

    val eglContext: EglBase.Context
        get() = eglBase.eglBaseContext

    fun attachRenderer(view: SurfaceViewRenderer) {
        if (renderer === view) return
        renderer?.let { old ->
            remoteVideoTrack?.removeSink(old)
        }
        renderer = view
        remoteVideoTrack?.addSink(view)
    }

    fun detachRenderer(view: SurfaceViewRenderer) {
        if (renderer !== view) return
        remoteVideoTrack?.removeSink(view)
        renderer = null
    }

    fun start() {
        if (started) return
        started = true
        scope.launch {
            runCatching { connect() }
                .onFailure {
                    started = false
                    onStatusChanged(DoorbellStreamStatus.Error(it.message ?: "Stream konnte nicht gestartet werden"))
                }
        }
    }

    fun stop() {
        started = false
        refreshJob?.cancel()
        refreshJob = null
        val sessionId = mediaSessionId
        mediaSessionId = null
        scope.launch(Dispatchers.IO) {
            if (sessionId != null) runCatching { client.stopWebRtcSession(sessionId) }
        }
        closePeerConnection()
        onStatusChanged(DoorbellStreamStatus.Idle)
    }

    fun release() {
        stop()
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        eglBase.release()
    }

    private suspend fun connect() {
        onStatusChanged(DoorbellStreamStatus.Connecting)
        val factory = getFactory()
        closePeerConnection()

        val rtcConfig = PeerConnection.RTCConfiguration(emptyList()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        val peer = requireNotNull(factory.createPeerConnection(rtcConfig, observer)) {
            "WebRTC PeerConnection konnte nicht erstellt werden"
        }
        peerConnection = peer

        // Google SDM requires m-lines in exactly this order:
        // audio, video, application.
        peer.addTransceiver(
            org.webrtc.MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY),
        )
        peer.addTransceiver(
            org.webrtc.MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY),
        )
        peer.createDataChannel("sdm", DataChannel.Init())

        val offer = createOffer(peer)
        setLocalDescription(peer, offer)
        val session = client.createWebRtcSession(offer.description)
        mediaSessionId = session.mediaSessionId
        setRemoteDescription(
            peer,
            SessionDescription(SessionDescription.Type.ANSWER, session.answerSdp),
        )
        onStatusChanged(DoorbellStreamStatus.Streaming)
        scheduleSessionRefresh(session.mediaSessionId)
    }

    private fun scheduleSessionRefresh(sessionId: String) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            delay(4 * 60_000L)
            if (!started || mediaSessionId != sessionId) return@launch

            val extended = runCatching { client.extendWebRtcSession(sessionId) }.isSuccess
            if (extended) {
                scheduleSessionRefresh(sessionId)
            } else {
                val oldId = mediaSessionId
                mediaSessionId = null
                if (oldId != null) runCatching { client.stopWebRtcSession(oldId) }
                closePeerConnection()
                if (started) {
                    runCatching { connect() }.onFailure {
                        started = false
                        onStatusChanged(
                            DoorbellStreamStatus.Error(
                                it.message ?: "Stream konnte nicht erneuert werden",
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun getFactory(): PeerConnectionFactory {
        peerConnectionFactory?.let { return it }

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(appContext)
                .createInitializationOptions(),
        )
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(eglContext, true, true),
            )
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext))
            .createPeerConnectionFactory()
            .also { peerConnectionFactory = it }
    }

    private suspend fun createOffer(peer: PeerConnection): SessionDescription {
        val deferred = CompletableDeferred<SessionDescription>()
        peer.createOffer(
            object : SdpObserverAdapter() {
                override fun onCreateSuccess(description: SessionDescription) {
                    deferred.complete(description)
                }

                override fun onCreateFailure(error: String) {
                    deferred.completeExceptionally(IllegalStateException(error))
                }
            },
            MediaConstraints(),
        )
        return deferred.await()
    }

    private suspend fun setLocalDescription(
        peer: PeerConnection,
        description: SessionDescription,
    ) {
        val deferred = CompletableDeferred<Unit>()
        peer.setLocalDescription(
            object : SdpObserverAdapter() {
                override fun onSetSuccess() {
                    deferred.complete(Unit)
                }

                override fun onSetFailure(error: String) {
                    deferred.completeExceptionally(IllegalStateException(error))
                }
            },
            description,
        )
        deferred.await()
    }

    private suspend fun setRemoteDescription(
        peer: PeerConnection,
        description: SessionDescription,
    ) {
        val deferred = CompletableDeferred<Unit>()
        peer.setRemoteDescription(
            object : SdpObserverAdapter() {
                override fun onSetSuccess() {
                    deferred.complete(Unit)
                }

                override fun onSetFailure(error: String) {
                    deferred.completeExceptionally(IllegalStateException(error))
                }
            },
            description,
        )
        deferred.await()
    }

    private fun closePeerConnection() {
        remoteVideoTrack?.let { track ->
            renderer?.let(track::removeSink)
        }
        remoteVideoTrack = null
        remoteAudioTrack = null
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
    }

    private val observer = object : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) = Unit
        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) = Unit
        override fun onIceCandidate(candidate: IceCandidate) = Unit
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
        override fun onAddStream(stream: MediaStream) = Unit
        override fun onRemoveStream(stream: MediaStream) = Unit
        override fun onDataChannel(channel: org.webrtc.DataChannel) = Unit
        override fun onRenegotiationNeeded() = Unit

        override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {
            when (val track = receiver.track()) {
                is VideoTrack -> {
                    remoteVideoTrack?.let { old -> renderer?.let(old::removeSink) }
                    remoteVideoTrack = track
                    renderer?.let(track::addSink)
                }

                is AudioTrack -> {
                    remoteAudioTrack = track
                    track.setEnabled(true)
                }
            }
        }

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
            if (newState == PeerConnection.PeerConnectionState.FAILED && started) {
                onStatusChanged(DoorbellStreamStatus.Error("WebRTC-Verbindung fehlgeschlagen"))
            }
        }
    }
}

sealed interface DoorbellStreamStatus {
    data object Idle : DoorbellStreamStatus
    data object Connecting : DoorbellStreamStatus
    data object Streaming : DoorbellStreamStatus
    data class Error(val message: String) : DoorbellStreamStatus
}

private open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(description: SessionDescription) = Unit
    override fun onSetSuccess() = Unit
    override fun onCreateFailure(error: String) = Unit
    override fun onSetFailure(error: String) = Unit
}
