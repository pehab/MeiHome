package de.haberland.meihome.smarthome

/**
 * Boundary around Google Device Access. The concrete implementation is responsible
 * for OAuth token refresh and SDM CameraLiveStream commands.
 */
interface DoorbellClient {
    suspend fun listDoorbells(): List<DoorbellDevice>
    suspend fun createWebRtcSession(offerSdp: String): WebRtcSession
    suspend fun stopWebRtcSession(mediaSessionId: String)
}

data class DoorbellDevice(
    val id: String,
    val name: String,
    val supportsWebRtc: Boolean,
)

data class WebRtcSession(
    val answerSdp: String,
    val mediaSessionId: String,
    val expiresAt: String,
)

/**
 * Boundary around Nuki Web so the UI never needs to know about HTTP endpoints,
 * authentication details or Nuki action codes.
 */
interface NukiClient {
    suspend fun getState(): DoorLockState
    suspend fun execute(action: DoorAction)
}
