package de.haberland.meihome.smarthome

import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class NestTokenResponse(
    val accessToken: String,
    val expiresInSeconds: Long,
    val refreshToken: String?,
)

class NestOAuthManager(
    private val preferences: FrontDoorPreferences,
) {
    companion object {
        const val STATE = "meihome_nest"
    }
    fun authorizationUri(): Uri {
        val params = linkedMapOf(
            "redirect_uri" to GOOGLE_NEST_REDIRECT_URI,
            "access_type" to "offline",
            "prompt" to "consent",
            "client_id" to GOOGLE_NEST_OAUTH_CLIENT_ID,
            "response_type" to "code",
            "scope" to "https://www.googleapis.com/auth/sdm.service",
            "state" to STATE,
        )
        val query = params.entries.joinToString("&") {
            encode(it.key) + "=" + encode(it.value)
        }
        return Uri.parse(
            "https://nestservices.google.com/partnerconnections/" +
                GOOGLE_DEVICE_ACCESS_PROJECT_ID + "/auth?" + query,
        )
    }

    suspend fun exchangeAuthorizationCode(code: String): NestTokenResponse =
        tokenRequest(
            mapOf(
                "client_id" to GOOGLE_NEST_OAUTH_CLIENT_ID,
                "client_secret" to requireClientSecret(),
                "code" to code,
                "grant_type" to "authorization_code",
                "redirect_uri" to GOOGLE_NEST_REDIRECT_URI,
            ),
        ).also { response ->
            response.refreshToken?.takeIf { it.isNotBlank() }?.let(preferences::setNestRefreshToken)
        }

    suspend fun refreshAccessToken(): NestTokenResponse =
        tokenRequest(
            mapOf(
                "client_id" to GOOGLE_NEST_OAUTH_CLIENT_ID,
                "client_secret" to requireClientSecret(),
                "refresh_token" to preferences.getNestRefreshToken()
                    .takeIf { it.isNotBlank() }
                    .orEmpty(),
                "grant_type" to "refresh_token",
            ),
        )

    fun isLinked(): Boolean =
        preferences.getNestClientSecret().isNotBlank() &&
            preferences.getNestRefreshToken().isNotBlank()

    private fun requireClientSecret(): String =
        preferences.getNestClientSecret()
            .takeIf { it.isNotBlank() }
            ?: error("Google Client Secret fehlt")

    private suspend fun tokenRequest(fields: Map<String, String>): NestTokenResponse =
        withContext(Dispatchers.IO) {
            require(fields["refresh_token"] != "") { "Google Home ist noch nicht verbunden" }
            val body = fields.entries.joinToString("&") {
                encode(it.key) + "=" + encode(it.value)
            }
            val connection = URL("https://oauth2.googleapis.com/token")
                .openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 15_000
                connection.readTimeout = 20_000
                connection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded",
                )
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }

                val responseCode = connection.responseCode
                val stream =
                    if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val responseText =
                    stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = if (responseText.isBlank()) JSONObject() else JSONObject(responseText)

                if (responseCode !in 200..299) {
                    error(
                        json.optString("error_description")
                            .ifBlank { json.optString("error") }
                            .ifBlank { "Google OAuth HTTP $responseCode" },
                    )
                }

                NestTokenResponse(
                    accessToken = json.getString("access_token"),
                    expiresInSeconds = json.optLong("expires_in", 3600L),
                    refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() },
                )
            } finally {
                connection.disconnect()
            }
        }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())
}

class NestTokenManager(
    private val oauth: NestOAuthManager,
) {
    private var accessToken: String? = null
    private var expiresAtMillis: Long = 0L

    suspend fun accessToken(): String {
        val now = System.currentTimeMillis()
        accessToken?.takeIf { now < expiresAtMillis - 60_000L }?.let { return it }

        val refreshed = oauth.refreshAccessToken()
        accessToken = refreshed.accessToken
        expiresAtMillis = now + refreshed.expiresInSeconds * 1000L
        return refreshed.accessToken
    }

    fun acceptInitial(response: NestTokenResponse) {
        accessToken = response.accessToken
        expiresAtMillis =
            System.currentTimeMillis() + response.expiresInSeconds * 1000L
    }
}
