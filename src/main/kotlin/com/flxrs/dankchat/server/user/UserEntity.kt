package com.flxrs.dankchat.server.user

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "users")
class UserEntity : PanacheEntityBase {
    @Id
    @Column(name = "twitch_id")
    lateinit var twitchId: String

    var login: String? = null

    @Column(name = "display_name")
    var displayName: String? = null

    @Column(name = "selected_badge_id")
    var selectedBadgeId: String? = null

    @Column(name = "hide_badges")
    var hideBadges: Boolean = false

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()

    companion object : PanacheCompanionBase<UserEntity, String> {
        fun findOrCreate(
            twitchId: String,
            login: String? = null,
        ): UserEntity = findById(twitchId) ?: UserEntity().also {
            it.twitchId = twitchId
            it.login = login
            it.persist()
        }
    }
}
