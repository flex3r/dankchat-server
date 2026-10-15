package com.flxrs.dankchat.server.badge

import kotlinx.serialization.Serializable

/** The badge format app versions before /v1/badges read from flxrs.com/api/badges. */
@Serializable
data class LegacyBadgeDto(
    val type: String,
    val url: String,
    val users: List<String>,
)

fun BadgesResponseDto.toLegacyBadges(): List<LegacyBadgeDto> {
    val usersByBadge = users.entries.groupBy(keySelector = { it.value }, valueTransform = { it.key })
    return badges.mapNotNull { badge ->
        usersByBadge[badge.id]?.let { LegacyBadgeDto(type = badge.title, url = badge.url, users = it) }
    }
}
