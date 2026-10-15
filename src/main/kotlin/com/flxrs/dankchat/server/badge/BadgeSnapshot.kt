package com.flxrs.dankchat.server.badge

import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.HexFormat

class BadgeSnapshot private constructor(
    val body: String,
    val etag: String,
) {
    companion object {
        val EMPTY = from(BadgesResponseDto(badges = emptyList(), users = emptyMap()))
        val EMPTY_LEGACY = from(emptyList())

        fun from(response: BadgesResponseDto): BadgeSnapshot = of(Json.encodeToString(response))

        fun from(legacyBadges: List<LegacyBadgeDto>): BadgeSnapshot = of(Json.encodeToString(legacyBadges))

        private fun of(body: String): BadgeSnapshot {
            val hash = MessageDigest.getInstance("SHA-256").digest(body.toByteArray())
            return BadgeSnapshot(body = body, etag = HexFormat.of().formatHex(hash))
        }
    }
}
