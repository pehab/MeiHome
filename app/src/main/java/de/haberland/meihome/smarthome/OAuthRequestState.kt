package de.haberland.meihome.smarthome

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

enum class OAuthFlow { NEST, PUBSUB }

data class PendingOAuthRequest(val state: String, val flow: OAuthFlow, val startedAtMillis: Long)

/** Persisted by the caller so browser round trips survive activity/process recreation. */
class OAuthRequestState(
    private val load: () -> PendingOAuthRequest?,
    private val save: (PendingOAuthRequest?) -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
) {
    @Synchronized
    fun begin(flow: OAuthFlow): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        // Only the latest connection attempt is valid, regardless of its flow.
        save(PendingOAuthRequest(state, flow, now()))
        return state
    }

    @Synchronized
    fun consume(state: String?): OAuthFlow? {
        val pending = load() ?: return null
        val age = now() - pending.startedAtMillis
        if (age < 0 || age >= MAX_AGE_MILLIS) {
            save(null)
            return null
        }
        if (state == null || state.length != pending.state.length ||
            !MessageDigest.isEqual(state.toByteArray(Charsets.UTF_8), pending.state.toByteArray(Charsets.UTF_8))
        ) return null

        // Consume before errors or token exchange: neither retries nor replay may reuse it.
        save(null)
        return pending.flow
    }

    companion object {
        const val MAX_AGE_MILLIS = 10 * 60 * 1000L
    }
}
