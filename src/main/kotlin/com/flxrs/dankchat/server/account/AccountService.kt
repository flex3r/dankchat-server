package com.flxrs.dankchat.server.account

import com.flxrs.dankchat.server.apiError
import com.flxrs.dankchat.server.badge.BadgeDto
import com.flxrs.dankchat.server.badge.BadgeEntity
import com.flxrs.dankchat.server.badge.BadgeHolding
import com.flxrs.dankchat.server.badge.BadgePreference
import com.flxrs.dankchat.server.badge.BadgesChanged
import com.flxrs.dankchat.server.badge.BadgesConfig
import com.flxrs.dankchat.server.badge.UserBadgeEntity
import com.flxrs.dankchat.server.badge.imageUrl
import com.flxrs.dankchat.server.badge.selectBadges
import com.flxrs.dankchat.server.donation.DonationEntity
import com.flxrs.dankchat.server.donation.ErasedDonorEntity
import com.flxrs.dankchat.server.donation.channelHash
import com.flxrs.dankchat.server.twitch.ChatColors
import com.flxrs.dankchat.server.twitch.defaultChatColor
import com.flxrs.dankchat.server.user.UserEntity
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Event
import jakarta.transaction.Transactional
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import java.time.Instant

@ApplicationScoped
class AccountService(
    private val badgesConfig: BadgesConfig,
    private val badgesChanged: Event<BadgesChanged>,
    private val chatColors: ChatColors,
) {
    @Transactional
    fun account(
        twitchId: String,
        displayName: String?,
        admin: Boolean,
    ): AccountDto {
        val user = UserEntity.findOrCreate(twitchId)
        if (displayName != null && user.displayName != displayName) {
            user.displayName = displayName
            user.updatedAt = Instant.now()
        }
        return user.toAccount(admin)
    }

    @Transactional
    fun choose(
        twitchId: String,
        choice: BadgeChoice,
        admin: Boolean,
    ): AccountDto {
        val user = UserEntity.findOrCreate(twitchId)
        when (choice) {
            BadgeChoice.Automatic -> {
                user.selectedBadgeId = null
                user.hideBadges = false
            }

            BadgeChoice.Hidden -> {
                user.selectedBadgeId = null
                user.hideBadges = true
            }

            is BadgeChoice.Badge -> {
                if (!UserBadgeEntity.exists(twitchId = twitchId, badgeId = choice.id)) {
                    apiError(Response.Status.BAD_REQUEST, "You don't own this badge")
                }
                user.selectedBadgeId = choice.id
                user.hideBadges = false
            }
        }
        user.updatedAt = Instant.now()
        badgesChanged.fire(BadgesChanged(reason = "badge choice of $twitchId"))
        return user.toAccount(admin)
    }

    @Transactional
    fun export(twitchId: String): AccountExportDto {
        val user = UserEntity.findById(twitchId) ?: apiError(Response.Status.NOT_FOUND, "There is no data stored for you")
        return AccountExportDto(
            twitchId = user.twitchId,
            login = user.login,
            displayName = user.displayName,
            selectedBadgeId = user.selectedBadgeId,
            hideBadges = user.hideBadges,
            createdAt = user.createdAt.toString(),
            updatedAt = user.updatedAt.toString(),
            badges =
                UserBadgeEntity.list("twitchId", twitchId).map {
                    ExportedBadgeDto(id = it.badgeId, source = it.source.name, grantedAt = it.grantedAt.toString())
                },
            donations =
                DonationEntity.list("twitchId", twitchId).map {
                    ExportedDonationDto(streamElementsChannelId = it.seChannelId, name = it.name, recordedAt = it.createdAt.toString())
                },
        )
    }

    @Transactional
    fun delete(twitchId: String) {
        DonationEntity.list("twitchId", twitchId).forEach { donation ->
            val hash = channelHash(donation.seChannelId)
            if (ErasedDonorEntity.findById(hash) == null) {
                ErasedDonorEntity().apply { channelHash = hash }.persist()
            }
            donation.delete()
        }
        UserBadgeEntity.delete("twitchId", twitchId)
        UserBadgeEntity.update("grantedBy = null where grantedBy = ?1", twitchId)
        UserEntity.deleteById(twitchId)
        badgesChanged.fire(BadgesChanged(reason = "account deletion"))
    }

    private fun UserEntity.toAccount(admin: Boolean): AccountDto {
        val ownedIds = UserBadgeEntity.list("twitchId", twitchId).map { it.badgeId }
        val owned =
            when {
                ownedIds.isEmpty() -> emptyList()
                else -> BadgeEntity.list("id in ?1", Sort.descending("priority").and("id"), ownedIds)
            }
        val selected = selectedBadgeId
        val choice =
            when {
                hideBadges -> BadgeChoice.Hidden
                selected != null && selected in ownedIds -> BadgeChoice.Badge(selected)
                else -> BadgeChoice.Automatic
            }
        val shown =
            selectBadges(
                priorities = owned.associate { it.id to it.priority },
                holdings = ownedIds.map { BadgeHolding(twitchId = twitchId, badgeId = it) },
                preferences = mapOf(twitchId to BadgePreference(selectedBadgeId = selectedBadgeId, hideBadges = hideBadges)),
            )[twitchId]

        return AccountDto(
            twitchId = twitchId,
            displayName = displayName,
            admin = admin,
            badges = owned.map { BadgeDto(id = it.id, title = it.title, url = badgesConfig.imageUrl(it.image)) },
            choice = choice,
            shownBadgeId = shown,
            chatColor = chatColor(twitchId),
        )
    }

    private fun chatColor(twitchId: String): String = try {
        chatColors.colorOf(twitchId)
    } catch (e: WebApplicationException) {
        log.warn("Chat color lookup for $twitchId failed with ${e.response.status}")
        defaultChatColor(twitchId)
    } catch (e: ProcessingException) {
        log.warn("Chat color lookup for $twitchId failed", e)
        defaultChatColor(twitchId)
    }

    companion object {
        private val log = Logger.getLogger(AccountService::class.java)
    }
}
