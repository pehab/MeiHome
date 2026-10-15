package de.haberland.meihome.smarthome

import org.junit.Assert.*
import org.junit.Test

class OAuthRequestStateTest {
    private var saved: PendingOAuthRequest? = null
    private var time = 1_000_000L
    private fun store() = OAuthRequestState({ saved }, { saved = it }, { time })

    @Test fun unsolicitedAndLegacyCallbacksAreRejected() {
        assertNull(store().consume("meihome_nest"))
        val request = store()
        val state = request.begin(OAuthFlow.NEST)
        assertNull(request.consume(null))
        assertNull(request.consume("meihome_nest"))
        assertNull(request.consume("meihome_pubsub"))
        assertNull(request.consume("unknown"))
        // An invalid callback must not cancel a legitimate connection attempt.
        assertEquals(OAuthFlow.NEST, request.consume(state))
    }

    @Test fun validStateSurvivesRecreationAndCanOnlyBeConsumedOnce() {
        val state = store().begin(OAuthFlow.PUBSUB)
        assertEquals(OAuthFlow.PUBSUB, store().consume(state))
        assertNull(store().consume(state))
    }

    @Test fun aNewAttemptInvalidatesThePreviousAttemptAcrossFlows() {
        val request = store()
        val old = request.begin(OAuthFlow.NEST)
        val current = request.begin(OAuthFlow.PUBSUB)
        assertNotEquals(old, current)
        assertEquals(43, current.length)
        assertTrue(current.matches(Regex("[A-Za-z0-9_-]+")))
        assertNull(request.consume(old))
        assertEquals(OAuthFlow.PUBSUB, request.consume(current))
    }

    @Test fun expiredAndFutureDatedRequestsAreRejectedAndCleared() {
        val request = store()
        val expired = request.begin(OAuthFlow.NEST)
        time += OAuthRequestState.MAX_AGE_MILLIS
        assertNull(request.consume(expired))
        assertNull(saved)
        val future = request.begin(OAuthFlow.NEST)
        time--
        assertNull(request.consume(future))
        assertNull(saved)
    }

    @Test fun callbacksJustBeforeExpiryStillWork() {
        val request = store()
        val state = request.begin(OAuthFlow.NEST)
        time += OAuthRequestState.MAX_AGE_MILLIS - 1
        assertEquals(OAuthFlow.NEST, request.consume(state))
    }
}
