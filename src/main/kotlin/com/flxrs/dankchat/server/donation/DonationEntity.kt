package com.flxrs.dankchat.server.donation

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "donations")
class DonationEntity : PanacheEntityBase {
    @Id
    @Column(name = "se_channel_id")
    lateinit var seChannelId: String

    @Column(name = "twitch_id")
    lateinit var twitchId: String

    lateinit var name: String

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now()

    companion object : PanacheCompanionBase<DonationEntity, String>
}
