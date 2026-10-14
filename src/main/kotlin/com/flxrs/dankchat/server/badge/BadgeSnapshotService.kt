package com.flxrs.dankchat.server.badge

import com.flxrs.dankchat.server.user.UserEntity
import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.panache.common.Sort
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.enterprise.event.TransactionPhase
import org.jboss.logging.Logger

@ApplicationScoped
class BadgeSnapshotService {
    @Volatile
    private var snapshot: BadgeSnapshot = BadgeSnapshot.EMPTY

    val current: BadgeSnapshot
        get() = snapshot

    @Startup
    fun init() {
        rebuild()
    }

    fun onBadgesChanged(
        @Observes(during = TransactionPhase.AFTER_SUCCESS) event: BadgesChanged,
    ) {
        log.debug("Rebuilding badge snapshot: ${event.reason}")
        rebuild()
    }

    @Synchronized
    fun rebuild() {
        snapshot = QuarkusTransaction.requiringNew().call { BadgeSnapshot.from(loadResponse()) }
    }

    private fun loadResponse(): BadgesResponseDto {
        val badges = BadgeEntity.listAll(Sort.descending("priority").and("id"))
        val holdings = UserBadgeEntity.listAll().map { BadgeHolding(twitchId = it.twitchId, badgeId = it.badgeId) }
        val preferences =
            UserEntity
                .list("hideBadges = true or selectedBadgeId is not null")
                .associate { it.twitchId to BadgePreference(selectedBadgeId = it.selectedBadgeId, hideBadges = it.hideBadges) }

        return BadgesResponseDto(
            badges = badges.map { BadgeDto(id = it.id, title = it.title, url = it.imageUrl) },
            users = selectBadges(priorities = badges.associate { it.id to it.priority }, holdings = holdings, preferences = preferences),
        )
    }

    companion object {
        private val log = Logger.getLogger(BadgeSnapshotService::class.java)
    }
}
