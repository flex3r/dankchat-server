package com.flxrs.dankchat.server.donation

import com.flxrs.dankchat.server.badge.BadgeSnapshotService
import com.flxrs.dankchat.server.badge.BadgeSource
import com.flxrs.dankchat.server.badge.UserBadgeEntity
import com.flxrs.dankchat.server.resetDatabase
import com.flxrs.dankchat.server.twitch.HelixApi
import com.flxrs.dankchat.server.twitch.HelixUserDto
import com.flxrs.dankchat.server.twitch.HelixUsersDto
import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.ws.rs.WebApplicationException
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@QuarkusTest
class DonationPollerTest {
    @InjectMock
    @RestClient
    lateinit var streamElements: StreamElementsApi

    @InjectMock
    @RestClient
    lateinit var helix: HelixApi

    @Inject
    lateinit var poller: DonationPoller

    @Inject
    lateinit var snapshots: BadgeSnapshotService

    @BeforeEach
    fun setup() {
        resetDatabase()
        val tips = listOf(tip("se1", "alice"), tip("se2", "bob"), tip("se1", "alice"))
        Mockito.`when`(streamElements.tips(CHANNEL_ID, 100, 0, AUTHORIZATION)).thenReturn(TipsDto(docs = tips, totalDocs = tips.size))
        Mockito.`when`(streamElements.channel("se1", AUTHORIZATION)).thenReturn(StreamElementsChannelDto(provider = "twitch", providerId = "111"))
        Mockito.`when`(streamElements.channel("se2", AUTHORIZATION)).thenThrow(WebApplicationException(404))
        Mockito.`when`(helix.users(listOf("bob"), emptyList())).thenReturn(HelixUsersDto(listOf(HelixUserDto(id = "222", login = "bob", displayName = "Bob"))))
    }

    @Test
    fun `records each new donor once with a supporter badge`() {
        poller.poll()
        poller.poll()

        val donations = QuarkusTransaction.requiringNew().call { DonationEntity.listAll().associate { it.seChannelId to it.twitchId } }
        val badges = QuarkusTransaction.requiringNew().call { UserBadgeEntity.listAll().map { Triple(it.twitchId, it.badgeId, it.source) } }

        assertEquals(mapOf("se1" to "111", "se2" to "222"), donations)
        assertEquals(setOf(Triple("111", "supporter", BadgeSource.DONATION), Triple("222", "supporter", BadgeSource.DONATION)), badges.toSet())
        assertEquals(2, badges.size)
        verify(streamElements, times(1)).channel("se1", AUTHORIZATION)
        assertTrue(snapshots.current.body.contains("\"users\":{\"111\":\"supporter\",\"222\":\"supporter\"}"))
    }

    @Test
    fun `skips donors who deleted their account`() {
        QuarkusTransaction.requiringNew().run { ErasedDonorEntity().apply { channelHash = channelHash("se1") }.persist() }

        poller.poll()

        val donations = QuarkusTransaction.requiringNew().call { DonationEntity.listAll().map { it.seChannelId } }
        assertEquals(listOf("se2"), donations)
        verify(streamElements, times(0)).channel("se1", AUTHORIZATION)
    }

    @Test
    fun `pages through all tips`() {
        val firstPage = (1..100).map { tip("first-$it", "user$it") }
        val secondPage = listOf(tip("second", "late"))
        Mockito.`when`(streamElements.tips(CHANNEL_ID, 100, 0, AUTHORIZATION)).thenReturn(TipsDto(docs = firstPage, totalDocs = 101))
        Mockito.`when`(streamElements.tips(CHANNEL_ID, 100, 100, AUTHORIZATION)).thenReturn(TipsDto(docs = secondPage, totalDocs = 101))
        Mockito.`when`(streamElements.channel(anyString(), anyString())).thenReturn(StreamElementsChannelDto(provider = "twitch", providerId = "1"))
        Mockito.`when`(streamElements.channel("second", AUTHORIZATION)).thenReturn(StreamElementsChannelDto(provider = "twitch", providerId = "2"))

        poller.poll()

        val donations = QuarkusTransaction.requiringNew().call { DonationEntity.count() }
        assertEquals(101, donations)
    }

    private fun tip(
        channel: String,
        username: String,
    ) = TipDocDto(donation = TipDonationDto(user = TipUserDto(username = username, channel = channel)))

    companion object {
        private const val CHANNEL_ID = "5b144fc91a5cbe3a3a920871"
        private const val AUTHORIZATION = "Bearer test-token"
    }
}
