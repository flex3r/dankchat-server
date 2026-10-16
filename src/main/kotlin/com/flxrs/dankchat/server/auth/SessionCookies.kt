package com.flxrs.dankchat.server.auth

import io.quarkus.runtime.Startup
import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.NewCookie
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

const val SESSION_COOKIE = "dankchat_session"

@ConfigMapping(prefix = "dankchat.session")
interface SessionConfig {
    /** Signs session cookies, at least 32 characters. */
    fun secret(): String

    @WithDefault("30D")
    fun maxAge(): Duration

    @WithDefault("true")
    fun secureCookie(): Boolean
}

/** Signed `<twitch id>.<expiry epoch seconds>.<signature>` cookies for sessions started from the app. */
@Startup
@ApplicationScoped
class SessionCookies(
    private val config: SessionConfig,
    private val clock: Clock,
) {
    private val key =
        run {
            require(config.secret().length >= MIN_SECRET_LENGTH) { "dankchat.session.secret must be at least $MIN_SECRET_LENGTH characters" }
            SecretKeySpec(config.secret().toByteArray(), HMAC)
        }

    fun issue(twitchId: String): NewCookie {
        val expiresAt = clock.instant().plus(config.maxAge()).epochSecond
        val payload = "$twitchId.$expiresAt"
        return cookie(value = "$payload.${sign(payload)}", maxAge = config.maxAge().seconds.toInt())
    }

    fun expired(): NewCookie = cookie(value = "", maxAge = 0)

    /** The Twitch id of a valid, unexpired cookie, or null. */
    fun verify(value: String): String? {
        val parts = value.split('.')
        if (parts.size != 3) {
            return null
        }
        val (twitchId, expiresAt, signature) = parts
        val expected = sign("$twitchId.$expiresAt")
        val valid = MessageDigest.isEqual(expected.toByteArray(), signature.toByteArray())
        val expiry = expiresAt.toLongOrNull()
        return when {
            !valid || expiry == null || expiry <= clock.instant().epochSecond -> null
            else -> twitchId
        }
    }

    private fun sign(payload: String): String {
        val mac = Mac.getInstance(HMAC).apply { init(key) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.toByteArray()))
    }

    private fun cookie(
        value: String,
        maxAge: Int,
    ): NewCookie = NewCookie
        .Builder(SESSION_COOKIE)
        .value(value)
        .path("/")
        .maxAge(maxAge)
        .httpOnly(true)
        .secure(config.secureCookie())
        .sameSite(NewCookie.SameSite.LAX)
        .build()

    companion object {
        private const val HMAC = "HmacSHA256"
        private const val MIN_SECRET_LENGTH = 32
    }
}
