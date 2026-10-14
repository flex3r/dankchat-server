package com.flxrs.dankchat.server

import com.flxrs.dankchat.server.badge.BadgeEntity
import com.flxrs.dankchat.server.badge.BadgeSource
import com.flxrs.dankchat.server.badge.UserBadgeEntity
import com.flxrs.dankchat.server.donation.DonationEntity
import com.flxrs.dankchat.server.user.UserEntity
import io.quarkus.narayana.jta.QuarkusTransaction

private val SYSTEM_BADGE_IDS = listOf("developer", "top-supporter", "contributor", "supporter")

fun resetDatabase() {
    QuarkusTransaction.requiringNew().run {
        UserBadgeEntity.deleteAll()
        DonationEntity.deleteAll()
        UserEntity.deleteAll()
        BadgeEntity.delete("id not in ?1", SYSTEM_BADGE_IDS)
    }
}

fun grantBadge(
    twitchId: String,
    badgeId: String,
) {
    UserEntity.findOrCreate(twitchId)
    UserBadgeEntity()
        .apply {
            this.twitchId = twitchId
            this.badgeId = badgeId
            source = BadgeSource.GRANT
        }.persist()
}
