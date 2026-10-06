package de.haberland.meihome.smarthome

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class FirebaseDoorbellClient(
    private val configProvider: () -> FrontDoorConfig,
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("europe-west1"),
) : DoorbellClient {

    override suspend fun listDoorbells(): List<DoorbellDevice> {
        val result = functions
            .getHttpsCallable("nestListDoorbells")
            .call()
            .await()
            .data as? Map<*, *> ?: emptyMap<Any, Any>()

        val doorbells = result["doorbells"] as? List<*> ?: emptyList<Any>()
        return doorbells.mapNotNull { raw ->
            val item = raw as? Map<*, *> ?: return@mapNotNull null
            val id = item["id"] as? String ?: return@mapNotNull null
            DoorbellDevice(
                id = id,
                name = (item["name"] as? String).orEmpty().ifBlank { "Google Doorbell" },
                supportsWebRtc = item["supportsWebRtc"] as? Boolean ?: false,
            )
        }
    }

    override suspend fun createWebRtcSession(offerSdp: String): WebRtcSession {
        val result = functions
            .getHttpsCallable("nestCreateWebRtcSession")
            .call(
                mapOf(
                    "deviceName" to configuredDeviceName(),
                    "offerSdp" to offerSdp,
                ),
            )
            .await()
            .data as? Map<*, *> ?: error("Ungültige Antwort vom Nest-Backend")

        return WebRtcSession(
            answerSdp = result["answerSdp"] as? String
                ?: error("Nest-Backend lieferte kein answerSdp"),
            mediaSessionId = result["mediaSessionId"] as? String
                ?: error("Nest-Backend lieferte keine mediaSessionId"),
            expiresAt = (result["expiresAt"] as? String).orEmpty(),
        )
    }

    override suspend fun extendWebRtcSession(mediaSessionId: String): String {
        val result = functions
            .getHttpsCallable("nestExtendWebRtcSession")
            .call(
                mapOf(
                    "deviceName" to configuredDeviceName(),
                    "mediaSessionId" to mediaSessionId,
                ),
            )
            .await()
            .data as? Map<*, *> ?: emptyMap<Any, Any>()

        return (result["expiresAt"] as? String).orEmpty()
    }

    override suspend fun stopWebRtcSession(mediaSessionId: String) {
        functions
            .getHttpsCallable("nestStopWebRtcSession")
            .call(
                mapOf(
                    "deviceName" to configuredDeviceName(),
                    "mediaSessionId" to mediaSessionId,
                ),
            )
            .await()
    }

    private fun configuredDeviceName(): String =
        configProvider().googleDeviceId.takeIf { it.isNotBlank() }
            ?: error("Google Doorbell ist noch nicht eingerichtet")
}
