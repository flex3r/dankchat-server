package com.flxrs.dankchat.server.auth

import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class LoginCodesTest {
    private val clock = TestClock()
    private val codes = LoginCodes(clock)

    @Test
    fun `codes work once`() {
        val code = codes.issue("100")

        assertEquals("100", codes.redeem(code))
        assertNull(codes.redeem(code))
    }

    @Test
    fun `codes expire`() {
        val code = codes.issue("100")
        clock.advance(LoginCodes.TTL)

        assertNull(codes.redeem(code))
    }

    @Test
    fun `unknown codes are rejected`() {
        assertNull(codes.redeem("unknown"))
    }

    @Test
    fun `every code is new`() {
        assertNotEquals(codes.issue("100"), codes.issue("100"))
    }

    @Test
    fun `issuing drops expired codes`() {
        val expired = codes.issue("100")
        clock.advance(Duration.ofMinutes(5))
        codes.issue("200")
        // Back inside the code's lifetime: only a removed code is still rejected
        clock.advance(Duration.ofMinutes(-5))

        assertNull(codes.redeem(expired))
    }
}
