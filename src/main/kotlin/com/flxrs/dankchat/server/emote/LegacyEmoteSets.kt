package com.flxrs.dankchat.server.emote

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.WebApplicationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.logging.Logger
import java.time.Duration

/** Twitch emote sets for DankChat 3.x, which loads all Twitch emotes from `flxrs.com/api/sets`. */
@ApplicationScoped
class LegacyEmoteSets(
    @param:RestClient private val ivr: IvrApi,
) {
    private val cache: Cache<String, LegacyEmoteSetDto> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(10_000)
            .build()

    fun emoteSets(ids: Collection<String>): List<LegacyEmoteSetDto> = cache.getAll(ids.distinct(), ::fetch).values.toList()

    private fun fetch(ids: Collection<String>): Map<String, LegacyEmoteSetDto> = ids
        .chunked(IVR_MAX_SETS)
        .flatMap { chunk ->
            try {
                ivr.emoteSets(chunk.joinToString(separator = ","))
            } catch (e: WebApplicationException) {
                log.warn("ivr emote sets lookup failed with ${e.response.status}")
                emptyList()
            } catch (e: ProcessingException) {
                log.warn("ivr emote sets lookup failed", e)
                emptyList()
            }
        }.associate { it.id to it.toLegacy() }

    private fun IvrEmoteSetDto.toLegacy() = LegacyEmoteSetDto(
        setId = id,
        channelName = channelLogin?.takeIf { it != TWITCH_PARTNER_LOGIN } ?: TWITCH_CHANNEL_NAME,
        channelId = channelId ?: TWITCH_CHANNEL_ID,
        tier = tier?.toIntOrNull() ?: 1,
        emotes = emotes.map { LegacyEmoteDto(code = it.code, id = it.id, type = it.type, assetType = it.assetType) },
    )

    companion object {
        private const val IVR_MAX_SETS = 50
        private const val TWITCH_PARTNER_LOGIN = "qa_TW_Partner"
        private const val TWITCH_CHANNEL_NAME = "Twitch"
        private const val TWITCH_CHANNEL_ID = "0"
        private val log = Logger.getLogger(LegacyEmoteSets::class.java)
    }
}

@Serializable
data class LegacyEmoteSetDto(
    @SerialName("set_id") val setId: String,
    @SerialName("channel_name") val channelName: String,
    @SerialName("channel_id") val channelId: String,
    val tier: Int,
    val emotes: List<LegacyEmoteDto>,
)

@Serializable
data class LegacyEmoteDto(
    val code: String,
    val id: String,
    val type: String?,
    val assetType: String?,
)
