package com.flxrs.dankchat.server.admin

import com.flxrs.dankchat.server.badge.BadgeSource
import kotlinx.serialization.Serializable

@Serializable
data class AdminBadgeDto(
    val id: String,
    val title: String,
    val url: String,
    val priority: Int,
    val holders: Int,
)

@Serializable
data class BadgeHolderDto(
    val twitchId: String,
    val login: String?,
    val displayName: String?,
    val source: BadgeSource,
    val grantedBy: String?,
    val grantedAt: String,
)

@Serializable
data class GrantBadgeRequest(
    val user: String,
)
