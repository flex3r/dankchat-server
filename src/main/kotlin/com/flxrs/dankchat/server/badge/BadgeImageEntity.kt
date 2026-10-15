package com.flxrs.dankchat.server.badge

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "badge_images")
class BadgeImageEntity : PanacheEntityBase {
    @Id
    lateinit var name: String

    @Column(name = "content_type")
    lateinit var contentType: String

    lateinit var data: ByteArray

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now()

    companion object : PanacheCompanionBase<BadgeImageEntity, String>
}
