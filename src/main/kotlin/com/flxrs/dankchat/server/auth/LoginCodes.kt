package com.flxrs.dankchat.server.auth

import jakarta.enterprise.context.ApplicationScoped
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/** Single-use codes that let the app open dank.chat already signed in. Kept in memory, so a restart drops open codes. */
@ApplicationScoped
class LoginCodes(
    private val clock: Clock,
) {
    private val random = SecureRandom()
    private val pending = ConcurrentHashMap<String, PendingLogin>()

    fun issue(twitchId: String): String {
        val now = clock.instant()
        pending.values.removeIf { it.expiresAt <= now }
        val code = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(CODE_BYTES).also(random::nextBytes))
        pending[code] = PendingLogin(twitchId = twitchId, expiresAt = now.plus(TTL))
        return code
    }

    /** The Twitch id the code was issued for, or null when it is unknown, used or expired. */
    fun redeem(code: String): String? = pending.remove(code)?.takeIf { it.expiresAt > clock.instant() }?.twitchId

    private data class PendingLogin(
        val twitchId: String,
        val expiresAt: Instant,
    )

    companion object {
        private const val CODE_BYTES = 32
        val TTL: Duration = Duration.ofSeconds(60)
    }
}
