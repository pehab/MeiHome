package de.haberland.meihome.smarthome

import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val PUBSUB_PROJECT_ID = "meicaller-489908"
private const val PUBSUB_TOPIC_ID = "meihome-doorbell-events"
private const val PUBSUB_SUBSCRIPTION_ID = "meihome-doorbell-events-meihome"
private const val CHIME_EVENT = "sdm.devices.events.DoorbellChime.Chime"

class DoorbellEventClient(
    private val accessTokenProvider: suspend () -> String,
) {
    suspend fun ensureSubscription() = withContext(Dispatchers.IO) {
        val token = accessTokenProvider()
        val subscription =
            "projects/$PUBSUB_PROJECT_ID/subscriptions/$PUBSUB_SUBSCRIPTION_ID"
        val topic = "projects/$PUBSUB_PROJECT_ID/topics/$PUBSUB_TOPIC_ID"

        request(
            method = "PUT",
            url = "https://pubsub.googleapis.com/v1/$subscription",
            accessToken = token,
            body = JSONObject()
                .put("topic", topic)
                .put("ackDeadlineSeconds", 20)
                .toString(),
            acceptedStatusCodes = setOf(200, 201, 409),
        )
    }

    suspend fun pullChime(deviceName: String): Boolean = withContext(Dispatchers.IO) {
        val token = accessTokenProvider()
        val subscription =
            "projects/$PUBSUB_PROJECT_ID/subscriptions/$PUBSUB_SUBSCRIPTION_ID"

        val result = request(
            method = "POST",
            url = "https://pubsub.googleapis.com/v1/$subscription:pull",
            accessToken = token,
            body = JSONObject()
                .put("maxMessages", 10)
                .toString(),
        )

        val received = result.optJSONArray("receivedMessages") ?: return@withContext false
        val ackIds = mutableListOf<String>()
        var chimeFound = false

        for (index in 0 until received.length()) {
            val item = received.optJSONObject(index) ?: continue
            item.optString("ackId").takeIf { it.isNotBlank() }?.let(ackIds::add)

            val encoded = item
                .optJSONObject("message")
                ?.optString("data")
                .orEmpty()
            if (encoded.isBlank()) continue

            val payload = runCatching {
                val decoded = Base64.decode(encoded, Base64.DEFAULT).toString(Charsets.UTF_8)
                JSONObject(decoded)
            }.getOrNull() ?: continue

            val resourceUpdate = payload.optJSONObject("resourceUpdate") ?: continue
            if (resourceUpdate.optString("name") != deviceName) continue
            val events = resourceUpdate.optJSONObject("events") ?: continue
            if (events.has(CHIME_EVENT)) {
                chimeFound = true
            }
        }

        if (ackIds.isNotEmpty()) {
            val ackBody = JSONObject().put("ackIds", org.json.JSONArray(ackIds))
            request(
                method = "POST",
                url = "https://pubsub.googleapis.com/v1/$subscription:acknowledge",
                accessToken = token,
                body = ackBody.toString(),
            )
        }

        chimeFound
    }

    private fun request(
        method: String,
        url: String,
        accessToken: String,
        body: String? = null,
        acceptedStatusCodes: Set<Int> = emptySet(),
    ): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 25_000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            }

            val status = connection.responseCode
            val stream =
                if (status in 200..299 || status in acceptedStatusCodes) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (status !in 200..299 && status !in acceptedStatusCodes) {
                val message = runCatching {
                    JSONObject(text).optJSONObject("error")?.optString("message")
                }.getOrNull().orEmpty()
                error(message.ifBlank { "Google Pub/Sub HTTP $status" })
            }

            return if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}
