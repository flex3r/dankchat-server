package com.flxrs.dankchat.server.admin

import com.flxrs.dankchat.server.apiError
import com.flxrs.dankchat.server.badge.BadgeEntity
import com.flxrs.dankchat.server.badge.BadgeImageEntity
import com.flxrs.dankchat.server.badge.BadgeImageType
import com.flxrs.dankchat.server.badge.BadgeSource
import com.flxrs.dankchat.server.badge.BadgesChanged
import com.flxrs.dankchat.server.badge.BadgesConfig
import com.flxrs.dankchat.server.badge.SUPPORTER_BADGE_ID
import com.flxrs.dankchat.server.badge.UserBadgeEntity
import com.flxrs.dankchat.server.badge.badgeImageName
import com.flxrs.dankchat.server.badge.imageUrl
import com.flxrs.dankchat.server.twitch.HelixUserDto
import com.flxrs.dankchat.server.twitch.TwitchUsers
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
class AdminBadgeService(
    private val badgesConfig: BadgesConfig,
    private val badgesChanged: Event<BadgesChanged>,
    private val twitchUsers: TwitchUsers,
) {
    @Transactional
    fun badges(): List<AdminBadgeDto> {
        val holders = UserBadgeEntity.listAll().groupingBy { it.badgeId }.eachCount()
        return BadgeEntity.listAll(Sort.descending("priority").and("id")).map { it.toDto(holders[it.id] ?: 0) }
    }

    @Transactional
    fun create(
        id: String,
        title: String,
        priority: Int,
        image: ByteArray,
    ): AdminBadgeDto {
        if (!BADGE_ID_PATTERN.matches(id)) {
            apiError(Response.Status.BAD_REQUEST, "Badge ids use lowercase letters, digits and dashes, up to 64 characters")
        }
        if (BadgeEntity.findById(id) != null) {
            apiError(Response.Status.CONFLICT, "A badge with this id already exists")
        }
        val badge =
            BadgeEntity().apply {
                this.id = id
                this.title = validTitle(title)
                this.priority = priority
                this.image = storeImage(id, image)
            }
        badge.persist()
        badgesChanged.fire(BadgesChanged(reason = "badge $id created"))
        return badge.toDto(holders = 0)
    }

    @Transactional
    fun update(
        id: String,
        title: String,
        priority: Int,
        image: ByteArray?,
    ): AdminBadgeDto {
        val badge = findBadge(id)
        badge.title = validTitle(title)
        badge.priority = priority
        if (image != null) {
            badge.image = storeImage(id, image)
        }
        badgesChanged.fire(BadgesChanged(reason = "badge $id updated"))
        return badge.toDto(holders = UserBadgeEntity.count("badgeId", id).toInt())
    }

    @Transactional
    fun delete(id: String) {
        if (id == SUPPORTER_BADGE_ID) {
            apiError(Response.Status.CONFLICT, "The supporter badge is needed for donations")
        }
        findBadge(id).delete()
        badgesChanged.fire(BadgesChanged(reason = "badge $id deleted"))
    }

    @Transactional
    fun holders(id: String): List<BadgeHolderDto> {
        findBadge(id)
        val holdings = UserBadgeEntity.list("badgeId", Sort.descending("grantedAt"), id)
        val users =
            when {
                holdings.isEmpty() -> emptyMap()
                else -> UserEntity.list("twitchId in ?1", holdings.map { it.twitchId }).associateBy { it.twitchId }
            }
        fillMissingNames(users.values.filter { it.login == null })
        return holdings.map { it.toDto(users[it.twitchId]) }
    }

    @Transactional
    fun grant(
        id: String,
        user: String,
        grantedBy: String,
    ): BadgeHolderDto {
        findBadge(id)
        val twitchUser =
            lookupTwitch { twitchUsers.byLoginOrId(user) }
                ?: apiError(Response.Status.NOT_FOUND, "There is no Twitch user named $user")

        if (UserBadgeEntity.exists(twitchId = twitchUser.id, badgeId = id)) {
            apiError(Response.Status.CONFLICT, "${twitchUser.displayName} already has this badge")
        }
        val holder = UserEntity.findOrCreate(twitchId = twitchUser.id)
        holder.applyTwitchNames(twitchUser)

        val holding =
            UserBadgeEntity().apply {
                twitchId = twitchUser.id
                badgeId = id
                source = BadgeSource.GRANT
                this.grantedBy = grantedBy
            }
        holding.persist()
        badgesChanged.fire(BadgesChanged(reason = "badge $id granted to ${twitchUser.id}"))
        return holding.toDto(holder)
    }

    @Transactional
    fun revoke(
        id: String,
        twitchId: String,
    ) {
        val deleted = UserBadgeEntity.delete("badgeId = ?1 and twitchId = ?2", id, twitchId)
        if (deleted == 0L) {
            apiError(Response.Status.NOT_FOUND, "This user doesn't have the badge")
        }
        badgesChanged.fire(BadgesChanged(reason = "badge $id revoked from $twitchId"))
    }

    /** Older holders only have an id, so their names are fetched from Twitch once and stored. */
    private fun fillMissingNames(users: List<UserEntity>) {
        if (users.isEmpty()) {
            return
        }
        val byId =
            try {
                twitchUsers.byIds(users.map { it.twitchId }).associateBy { it.id }
            } catch (e: WebApplicationException) {
                log.warn("Could not fetch Twitch names for ${users.size} users: ${e.response.status}")
                return
            } catch (e: ProcessingException) {
                log.warn("Could not fetch Twitch names for ${users.size} users", e)
                return
            }
        users.forEach { user -> byId[user.twitchId]?.let { user.applyTwitchNames(it) } }
    }

    private fun UserEntity.applyTwitchNames(twitchUser: HelixUserDto) {
        login = twitchUser.login
        displayName = twitchUser.displayName
        updatedAt = Instant.now()
    }

    private inline fun <T> lookupTwitch(block: () -> T): T = try {
        block()
    } catch (e: WebApplicationException) {
        apiError(Response.Status.BAD_GATEWAY, "Twitch user lookup failed with status ${e.response.status}")
    } catch (e: ProcessingException) {
        apiError(Response.Status.BAD_GATEWAY, "Twitch user lookup failed: ${e.message}")
    }

    private fun findBadge(id: String): BadgeEntity = BadgeEntity.findById(id) ?: apiError(Response.Status.NOT_FOUND, "Badge $id doesn't exist")

    private fun validTitle(title: String): String {
        val trimmed = title.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_TITLE_LENGTH) {
            apiError(Response.Status.BAD_REQUEST, "Titles need 1 to $MAX_TITLE_LENGTH characters")
        }
        return trimmed
    }

    private fun storeImage(
        badgeId: String,
        image: ByteArray,
    ): String {
        if (image.size > MAX_IMAGE_BYTES) {
            apiError(Response.Status.BAD_REQUEST, "Images can be at most 2 MB")
        }
        val type = BadgeImageType.detect(image) ?: apiError(Response.Status.BAD_REQUEST, "Images must be PNG, GIF or WebP")
        val name = badgeImageName(badgeId = badgeId, bytes = image, type = type)
        if (BadgeImageEntity.findById(name) == null) {
            BadgeImageEntity()
                .apply {
                    this.name = name
                    contentType = type.contentType
                    data = image
                }.persist()
        }
        return name
    }

    private fun BadgeEntity.toDto(holders: Int) = AdminBadgeDto(
        id = id,
        title = title,
        url = badgesConfig.imageUrl(image),
        priority = priority,
        holders = holders,
    )

    private fun UserBadgeEntity.toDto(user: UserEntity?) = BadgeHolderDto(
        twitchId = twitchId,
        login = user?.login,
        displayName = user?.displayName,
        source = source,
        grantedBy = grantedBy,
        grantedAt = grantedAt.toString(),
    )

    companion object {
        private val BADGE_ID_PATTERN = Regex("[a-z0-9]+(-[a-z0-9]+)*")
        private const val MAX_TITLE_LENGTH = 128
        private const val MAX_IMAGE_BYTES = 2 * 1024 * 1024
        private val log = Logger.getLogger(AdminBadgeService::class.java)
    }
}
