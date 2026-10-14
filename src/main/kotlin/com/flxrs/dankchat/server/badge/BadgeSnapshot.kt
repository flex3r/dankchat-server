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

        fun from(response: BadgesResponseDto): BadgeSnapshot {
            val body = Json.encodeToString(response)
            val hash = MessageDigest.getInstance("SHA-256").digest(body.toByteArray())
            return BadgeSnapshot(body = body, etag = HexFormat.of().formatHex(hash))
        }
    }
}
