package de.haberland.meihome.smarthome

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class NukiDevice(
    val id: String,
    val name: String,
    val stateName: String,
)

class NukiWebClient(
    private val tokenProvider: () -> String,
    private val deviceIdProvider: () -> String,
) : NukiClient {

    suspend fun listLocks(): List<NukiDevice> = withContext(Dispatchers.IO) {
        val token = requireToken()
        val response = request(
            method = "GET",
            url = "https://api.nuki.io/smartlock",
            token = token,
        )
        val array = JSONArray(response)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val type = item.optInt("type", item.optInt("deviceType", -1))
                if (type == 2) continue
                val id = item.opt("smartlockId")?.toString()
                    ?: item.opt("nukiId")?.toString()
                    ?: continue
                val state = item.optJSONObject("state")
                    ?: item.optJSONObject("lastKnownState")
                    ?: JSONObject()
                add(
                    NukiDevice(
                        id = id,
                        name = item.optString("name").ifBlank { "Nuki Smart Lock" },
                        stateName = state.optString("stateName"),
                    ),
                )
            }
        }
    }

    override suspend fun getState(): DoorLockState = withContext(Dispatchers.IO) {
        val token = requireToken()
        val id = requireDeviceId()
        val response = request(
            method = "GET",
            url = "https://api.nuki.io/smartlock/$id",
            token = token,
        )
        val json = JSONObject(response)
        val state = json.optJSONObject("state")
            ?: json.optJSONObject("lastKnownState")
            ?: JSONObject()
        DoorLockState(
            connected = true,
            label = localizedState(state.optString("stateName")),
        )
    }

    override suspend fun execute(action: DoorAction) {
        withContext(Dispatchers.IO) {
            val actionId = when (action) {
                DoorAction.UNLOCK -> 1
                DoorAction.LOCK -> 2
                DoorAction.UNLATCH -> 3
                DoorAction.LOCK_N_GO -> 4
            }
            request(
                method = "POST",
                url = "https://api.nuki.io/smartlock/${requireDeviceId()}/action",
                token = requireToken(),
                body = JSONObject().put("action", actionId).toString(),
                acceptedStatusCodes = setOf(204),
            )
        }
    }

    private fun requireToken(): String =
        tokenProvider().takeIf { it.isNotBlank() }
            ?: error("Nuki API Token fehlt")

    private fun requireDeviceId(): String =
        deviceIdProvider().takeIf { it.isNotBlank() }
            ?: error("Nuki Smart Lock ist noch nicht ausgewählt")

    private fun localizedState(stateName: String): String =
        when (stateName.lowercase()) {
            "locked" -> "Abgeschlossen"
            "unlocking" -> "Wird aufgesperrt..."
            "unlocked" -> "Aufgesperrt"
            "locking" -> "Wird abgeschlossen..."
            "unlatched" -> "Tür geöffnet"
            "unlatching" -> "Tür wird geöffnet..."
            "motor blocked" -> "Motor blockiert"
            "undefined" -> "Unbekannt"
            else -> stateName.ifBlank { "Status unbekannt" }
        }

    private fun request(
        method: String,
        url: String,
        token: String,
        body: String? = null,
        acceptedStatusCodes: Set<Int> = emptySet(),
    ): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 25_000
            connection.setRequestProperty("Authorization", "Bearer $token")
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
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (status !in 200..299 && status !in acceptedStatusCodes) {
                val message = runCatching {
                    JSONObject(response).optString("message")
                }.getOrNull().orEmpty()
                error(message.ifBlank { "Nuki Web API HTTP $status" })
            }
            return response
        } finally {
            connection.disconnect()
        }
    }
}
