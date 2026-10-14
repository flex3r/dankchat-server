package com.flxrs.dankchat.server.donation

import com.flxrs.dankchat.server.badge.BadgeSource
import com.flxrs.dankchat.server.badge.BadgesChanged
import com.flxrs.dankchat.server.badge.SUPPORTER_BADGE_ID
import com.flxrs.dankchat.server.badge.UserBadgeEntity
import com.flxrs.dankchat.server.user.UserEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Event
import jakarta.transaction.Transactional

data class NewDonor(
    val seChannelId: String,
    val name: String,
    val twitchId: String,
)

@ApplicationScoped
class DonationService(
    private val badgesChanged: Event<BadgesChanged>,
) {
    @Transactional
    fun knownChannelIds(): Set<String> = DonationEntity.listAll().map { it.seChannelId }.toSet()

    @Transactional
    fun record(donors: List<NewDonor>) {
        donors.forEach { donor ->
            DonationEntity()
                .apply {
                    seChannelId = donor.seChannelId
                    twitchId = donor.twitchId
                    name = donor.name
                }.persist()

            UserEntity.findOrCreate(twitchId = donor.twitchId, login = donor.name)
            if (!UserBadgeEntity.exists(twitchId = donor.twitchId, badgeId = SUPPORTER_BADGE_ID)) {
                UserBadgeEntity()
                    .apply {
                        twitchId = donor.twitchId
                        badgeId = SUPPORTER_BADGE_ID
                        source = BadgeSource.DONATION
                    }.persist()
            }
        }

        if (donors.isNotEmpty()) {
            badgesChanged.fire(BadgesChanged(reason = "${donors.size} new donors"))
        }
    }
}
