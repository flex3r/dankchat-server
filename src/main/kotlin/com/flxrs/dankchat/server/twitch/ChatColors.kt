package com.flxrs.dankchat.server.twitch

import io.quarkus.cache.CacheResult
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
class ChatColors(
    @param:RestClient private val helix: HelixApi,
) {
    /** The user's chat name color, or the color DankChat assigns users who never picked one. */
    @CacheResult(cacheName = "chat-colors")
    fun colorOf(twitchId: String): String {
        val color = helix
            .chatColors(listOf(twitchId))
            .data
            .firstOrNull()
            ?.color
        return color?.takeIf { it.isNotEmpty() } ?: defaultChatColor(twitchId)
    }
}

/** Matches DankChat's stable color for users without one, picked from Twitch's default name colors. */
fun defaultChatColor(twitchId: String): String {
    val seed = twitchId.toIntOrNull() ?: twitchId.sumOf { it.code }
    return DEFAULT_CHAT_COLORS[seed % DEFAULT_CHAT_COLORS.size]
}

private val DEFAULT_CHAT_COLORS =
    listOf(
        "#FF0000",
        "#0000FF",
        "#00FF00",
        "#B22222",
        "#FF7F50",
        "#9ACD32",
        "#FF4500",
        "#2E8B57",
        "#DAA520",
        "#D2691E",
        "#5F9EA0",
        "#1E90FF",
        "#FF69B4",
        "#8A2BE2",
        "#00FF7F",
    )
