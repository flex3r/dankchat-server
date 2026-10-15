package com.flxrs.dankchat.server.account

import com.flxrs.dankchat.server.badge.BadgeDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val twitchId: String,
    val displayName: String?,
    val admin: Boolean,
    val badges: List<BadgeDto>,
    val choice: BadgeChoice,
    val shownBadgeId: String?,
    val chatColor: String,
)

@Serializable
sealed interface BadgeChoice {
    @Serializable
    @SerialName("automatic")
    data object Automatic : BadgeChoice

    @Serializable
    @SerialName("hidden")
    data object Hidden : BadgeChoice

    @Serializable
    @SerialName("badge")
    data class Badge(
        val id: String,
    ) : BadgeChoice
}

@Serializable
data class AccountExportDto(
    val twitchId: String,
    val login: String?,
    val displayName: String?,
    val selectedBadgeId: String?,
    val hideBadges: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val badges: List<ExportedBadgeDto>,
    val donations: List<ExportedDonationDto>,
)

@Serializable
data class ExportedBadgeDto(
    val id: String,
    val source: String,
    val grantedAt: String,
)

@Serializable
data class ExportedDonationDto(
    val streamElementsChannelId: String,
    val name: String,
    val recordedAt: String,
)
