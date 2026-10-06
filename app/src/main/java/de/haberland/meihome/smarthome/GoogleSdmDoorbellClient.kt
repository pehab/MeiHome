package de.haberland.meihome.smarthome

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GoogleSdmDoorbellClient(
    private val configProvider: () -> FrontDoorConfig,
    private val accessTokenProvider: suspend () -> String,
) : DoorbellClient {

    override suspend fun listDoorbells(): List<DoorbellDevice> = withContext(Dispatchers.IO) {
        val config = configProvider()
        require(config.googleProjectId.isNotBlank()) { "Google Project ID fehlt" }
        val accessToken = accessTokenProvider()
        require(accessToken.isNotBlank()) { "Google Access Token fehlt" }

        val json = request(
            method = "GET",
            url = "https://smartdevicemanagement.googleapis.com/v1/enterprises/" +
                config.googleProjectId + "/devices",
            accessToken = accessToken,
        )
        val devices = json.optJSONArray("devices") ?: return@withContext emptyList()

        buildList {
            for (index in 0 until devices.length()) {
                val device = devices.getJSONObject(index)
                if (device.optString("type") != "sdm.devices.types.DOORBELL") continue
                val traits = device.optJSONObject("traits")
                val liveStream = traits?.optJSONObject("sdm.devices.traits.CameraLiveStream")
                val protocols = liveStream?.optJSONArray("supportedProtocols")
                val supportsWebRtc = protocols?.let { array ->
                    (0 until array.length()).any { array.optString(it) == "WEB_RTC" }
                } == true
                add(
                    DoorbellDevice(
                        id = device.optString("name"),
                        name = traits
                            ?.optJSONObject("sdm.devices.traits.Info")
                            ?.optString("customName")
                            .orEmpty()
                            .ifBlank { "Google Doorbell" },
                        supportsWebRtc = supportsWebRtc,
                    ),
                )
            }
        }
    }

    override suspend fun createWebRtcSession(offerSdp: String): WebRtcSession =
        withContext(Dispatchers.IO) {
            val config = configProvider()
            val deviceName = normalizedDeviceName(config)
            val result = executeCommand(
                deviceName = deviceName,
                command = "sdm.devices.commands.CameraLiveStream.GenerateWebRtcStream",
                params = JSONObject().put("offerSdp", offerSdp),
                accessToken = accessTokenProvider(),
            ).getJSONObject("results")

            WebRtcSession(
                answerSdp = result.getString("answerSdp"),
                mediaSessionId = result.getString("mediaSessionId"),
                expiresAt = result.optString("expiresAt"),
            )
        }

    override suspend fun extendWebRtcSession(mediaSessionId: String): String =
        withContext(Dispatchers.IO) {
            val config = configProvider()
            executeCommand(
                deviceName = normalizedDeviceName(config),
                command = "sdm.devices.commands.CameraLiveStream.ExtendWebRtcStream",
                params = JSONObject().put("mediaSessionId", mediaSessionId),
                accessToken = accessTokenProvider(),
            ).getJSONObject("results").optString("expiresAt")
        }

    override suspend fun stopWebRtcSession(mediaSessionId: String) {
        withContext(Dispatchers.IO) {
            val config = configProvider()
            executeCommand(
                deviceName = normalizedDeviceName(config),
                command = "sdm.devices.commands.CameraLiveStream.StopWebRtcStream",
                params = JSONObject().put("mediaSessionId", mediaSessionId),
                accessToken = accessTokenProvider(),
            )
        }
    }

    private fun normalizedDeviceName(config: FrontDoorConfig): String {
        require(config.googleDeviceId.isNotBlank()) { "Google Doorbell Device fehlt" }
        return if (config.googleDeviceId.startsWith("enterprises/")) {
            config.googleDeviceId
        } else {
            "enterprises/${config.googleProjectId}/devices/${config.googleDeviceId}"
        }
    }

    private fun executeCommand(
        deviceName: String,
        command: String,
        params: JSONObject,
        accessToken: String,
    ): JSONObject = request(
        method = "POST",
        url = "https://smartdevicemanagement.googleapis.com/v1/$deviceName:executeCommand",
        accessToken = accessToken,
        body = JSONObject()
            .put("command", command)
            .put("params", params)
            .toString(),
    )

    private fun request(
        method: String,
        url: String,
        accessToken: String,
        body: String? = null,
    ): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                val message = runCatching {
                    JSONObject(responseText)
                        .optJSONObject("error")
                        ?.optString("message")
                }.getOrNull().orEmpty()
                error(message.ifBlank { "Google SDM HTTP $responseCode" })
            }
            return if (responseText.isBlank()) JSONObject() else JSONObject(responseText)
        } finally {
            connection.disconnect()
        }
    }
}
