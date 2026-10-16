package com.flxrs.dankchat.server.auth

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionCookiesTest {
    private val clock = TestClock()
    private val cookies = SessionCookies(config(SECRET), clock)

    @Test
    fun `verifies its own cookies`() {
        val cookie = cookies.issue("100")

        assertEquals("100", cookies.verify(cookie.value))
        assertEquals(Duration.ofDays(30).seconds.toInt(), cookie.maxAge)
    }

    @Test
    fun `rejects expired cookies`() {
        val value = cookies.issue("100").value
        clock.advance(Duration.ofDays(30))

        assertNull(cookies.verify(value))
    }

    @Test
    fun `rejects changed cookies`() {
        val (_, expiresAt, signature) = cookies.issue("100").value.split('.')

        assertNull(cookies.verify("200.$expiresAt.$signature"))
        assertNull(cookies.verify("100.${expiresAt.toLong() + 60}.$signature"))
        assertNull(cookies.verify("100.$expiresAt.${signature.reversed()}"))
        assertNull(cookies.verify("garbage"))
    }

    @Test
    fun `rejects cookies signed with another secret`() {
        val other = SessionCookies(config("another-secret-that-is-long-enough-too"), clock)

        assertNull(cookies.verify(other.issue("100").value))
    }

    @Test
    fun `requires a long secret`() {
        assertThrows<IllegalArgumentException> { SessionCookies(config("short"), clock) }
    }

    @Test
    fun `clearing expires the cookie`() {
        val cleared = cookies.expired()

        assertEquals(SESSION_COOKIE, cleared.name)
        assertEquals(0, cleared.maxAge)
    }

    private fun config(secret: String) = object : SessionConfig {
        override fun secret() = secret

        override fun maxAge(): Duration = Duration.ofDays(30)

        override fun secureCookie() = true
    }

    companion object {
        private const val SECRET = "test-secret-that-is-at-least-32-chars"
    }
}
