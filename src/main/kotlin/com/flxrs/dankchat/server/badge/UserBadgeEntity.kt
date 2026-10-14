package com.flxrs.dankchat.server.badge

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "user_badges")
class UserBadgeEntity : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "twitch_id")
    lateinit var twitchId: String

    @Column(name = "badge_id")
    lateinit var badgeId: String

    @Enumerated(EnumType.STRING)
    lateinit var source: BadgeSource

    @Column(name = "granted_by")
    var grantedBy: String? = null

    @Column(name = "granted_at")
    var grantedAt: Instant = Instant.now()

    companion object : PanacheCompanion<UserBadgeEntity> {
        fun exists(
            twitchId: String,
            badgeId: String,
        ): Boolean = count("twitchId = ?1 and badgeId = ?2", twitchId, badgeId) > 0
    }
}

enum class BadgeSource {
    DONATION,
    GRANT,
}
