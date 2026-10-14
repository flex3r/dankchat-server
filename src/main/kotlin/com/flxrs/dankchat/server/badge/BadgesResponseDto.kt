package com.flxrs.dankchat.server.badge

import kotlinx.serialization.Serializable

@Serializable
data class BadgesResponseDto(
    val badges: List<BadgeDto>,
    val users: Map<String, String>,
)

@Serializable
data class BadgeDto(
    val id: String,
    val title: String,
    val url: String,
)
