package com.flxrs.dankchat.server.badge

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "badges")
class BadgeEntity : PanacheEntityBase {
    @Id
    lateinit var id: String

    lateinit var title: String

    lateinit var image: String

    var priority: Int = 0

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now()

    companion object : PanacheCompanionBase<BadgeEntity, String>
}
