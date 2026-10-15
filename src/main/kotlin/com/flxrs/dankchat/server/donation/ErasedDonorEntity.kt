package com.flxrs.dankchat.server.donation

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.security.MessageDigest
import java.time.Instant
import java.util.HexFormat

/** Donor of a deleted account, kept only as a hash so polling all tips doesn't recreate the account. */
@Entity
@Table(name = "erased_donors")
class ErasedDonorEntity : PanacheEntityBase {
    @Id
    @Column(name = "channel_hash")
    lateinit var channelHash: String

    @Column(name = "erased_at")
    var erasedAt: Instant = Instant.now()

    companion object : PanacheCompanionBase<ErasedDonorEntity, String>
}

fun channelHash(seChannelId: String): String = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(seChannelId.toByteArray()))
