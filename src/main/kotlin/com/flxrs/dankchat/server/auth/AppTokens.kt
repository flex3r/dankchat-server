package com.flxrs.dankchat.server.auth

import com.flxrs.dankchat.server.twitch.TwitchIdApi
import io.quarkus.cache.CacheKeyGenerator
import io.quarkus.cache.CacheResult
import io.smallrye.config.ConfigMapping
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.lang.reflect.Method
import java.security.MessageDigest
import java.util.HexFormat

@ConfigMapping(prefix = "dankchat.app")
interface AppConfig {
    /** Twitch client ids whose user tokens identify DankChat app users. */
    fun twitchClientIds(): Set<String>

    /** Base URL of the site that login codes open. */
    fun siteUrl(): String
}

/** Checks Twitch user tokens sent by the DankChat app. */
@ApplicationScoped
class AppTokens(
    private val config: AppConfig,
    @param:RestClient private val twitchId: TwitchIdApi,
) {
    /** The token's Twitch user id, or null when Twitch rejects the token or it belongs to another app. */
    @CacheResult(cacheName = "app-tokens", keyGenerator = TokenHashKeyGenerator::class)
    fun twitchIdOf(token: String): String? {
        val validated =
            try {
                twitchId.validate("OAuth $token")
            } catch (e: WebApplicationException) {
                when (e.response.status) {
                    Response.Status.UNAUTHORIZED.statusCode -> return null
                    else -> throw e
                }
            }
        return validated.userId?.takeIf { validated.clientId in config.twitchClientIds() }
    }
}

/** Keeps raw tokens out of the cache. */
@ApplicationScoped
class TokenHashKeyGenerator : CacheKeyGenerator {
    override fun generate(
        method: Method,
        vararg methodParams: Any,
    ): Any = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((methodParams.single() as String).toByteArray()))
}
