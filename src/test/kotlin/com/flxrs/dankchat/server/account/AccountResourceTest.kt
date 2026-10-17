package com.flxrs.dankchat.server.account

import com.flxrs.dankchat.server.admin.AdminRoleAugmentor
import com.flxrs.dankchat.server.badge.UserBadgeEntity
import com.flxrs.dankchat.server.donation.DonationEntity
import com.flxrs.dankchat.server.donation.ErasedDonorEntity
import com.flxrs.dankchat.server.donation.channelHash
import com.flxrs.dankchat.server.grantBadge
import com.flxrs.dankchat.server.resetDatabase
import com.flxrs.dankchat.server.twitch.HelixApi
import com.flxrs.dankchat.server.twitch.HelixChatColorDto
import com.flxrs.dankchat.server.twitch.HelixChatColorsDto
import com.flxrs.dankchat.server.twitch.HelixUsersDto
import com.flxrs.dankchat.server.user.UserEntity
import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.quarkus.test.security.oidc.Claim
import io.quarkus.test.security.oidc.OidcSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.ws.rs.WebApplicationException
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@QuarkusTest
class AccountResourceTest {
    @InjectMock
    @RestClient
    lateinit var helix: HelixApi

    @BeforeEach
    fun setup() {
        resetDatabase()
        Mockito.`when`(helix.chatColors(anyList())).thenReturn(HelixChatColorsDto(emptyList()))
        Mockito.`when`(helix.chatColors(listOf("100"))).thenReturn(chatColor("#00BCD4"))
        Mockito.`when`(helix.users(anyList(), anyList())).thenReturn(HelixUsersDto(emptyList()))
        QuarkusTransaction.requiringNew().run {
            grantBadge("100", "supporter")
            grantBadge("100", "contributor")
        }
    }

    @Test
    fun `requires a login`() {
        given()
            .get("/api/me")
            .then()
            .statusCode(401)
    }

    @Test
    @TestSecurity(user = "100")
    @OidcSecurity(claims = [Claim(key = "sub", value = "100"), Claim(key = "preferred_username", value = "Alice")])
    fun `shows owned badges and stores the display name`() {
        given()
            .get("/api/me")
            .then()
            .statusCode(200)
            .body("twitchId", equalTo("100"))
            .body("displayName", equalTo("Alice"))
            .body("admin", equalTo(false))
            .body("badges.id", contains("contributor", "supporter"))
            .body("choice.type", equalTo("automatic"))
            .body("shownBadgeId", equalTo("contributor"))
            .body("chatColor", equalTo("#00BCD4"))

        val displayName = QuarkusTransaction.requiringNew().call { UserEntity.findById("100")?.displayName }
        assertEquals("Alice", displayName)
    }

    @Test
    @TestSecurity(user = "300")
    @OidcSecurity(claims = [Claim(key = "sub", value = "300"), Claim(key = "preferred_username", value = "Bob")])
    fun `shows new users without storing them`() {
        given()
            .get("/api/me")
            .then()
            .statusCode(200)
            .body("twitchId", equalTo("300"))
            .body("displayName", equalTo("Bob"))
            .body("choice.type", equalTo("automatic"))

        assertNull(QuarkusTransaction.requiringNew().call { UserEntity.findById("300") })
    }

    @Test
    @TestSecurity(user = "100")
    @OidcSecurity(claims = [Claim(key = "sub", value = "100"), Claim(key = "preferred_username", value = "Alice")])
    fun `uses DankChat's default color for users without one`() {
        Mockito.`when`(helix.chatColors(listOf("100"))).thenReturn(chatColor(""))

        given()
            .get("/api/me")
            .then()
            .statusCode(200)
            .body("chatColor", equalTo("#5F9EA0"))
    }

    @Test
    @TestSecurity(user = "100")
    @OidcSecurity(claims = [Claim(key = "sub", value = "100"), Claim(key = "preferred_username", value = "Alice")])
    fun `falls back to the default color when the lookup fails`() {
        Mockito.`when`(helix.chatColors(listOf("100"))).thenThrow(WebApplicationException(503))

        given()
            .get("/api/me")
            .then()
            .statusCode(200)
            .body("chatColor", equalTo("#5F9EA0"))
    }

    @Test
    @TestSecurity(user = "100")
    @OidcSecurity(claims = [Claim(key = "sub", value = "100"), Claim(key = "preferred_username", value = "Alice")])
    fun `choosing a badge updates the public badge list`() {
        choose("""{"type":"badge","id":"supporter"}""")
            .statusCode(200)
            .body("choice.type", equalTo("badge"))
            .body("choice.id", equalTo("supporter"))
            .body("shownBadgeId", equalTo("supporter"))

        given()
            .get("/v1/badges")
            .then()
            .body("users.100", equalTo("supporter"))
    }

    @Test
    @TestSecurity(user = "100")
    @OidcSecurity(claims = [Claim(key = "sub", value = "100"), Claim(key = "preferred_username", value = "Alice")])
    fun `hiding removes the user from the public badge list`() {
        choose("""{"type":"hidden"}""")
            .statusCode(200)
            .body("choice.type", equalTo("hidden"))
            .body("shownBadgeId", nullValue())

        given()
            .get("/v1/badges")
            .then()
            .body("users.100", nullValue())

        choose("""{"type":"automatic"}""")
            .statusCode(200)
            .body("shownBadgeId", equalTo("contributor"))
    }

    @Test
    @TestSecurity(user = "100")
    @OidcSecurity(claims = [Claim(key = "sub", value = "100"), Claim(key = "preferred_username", value = "Alice")])
    fun `rejects badges the user doesn't own`() {
        choose("""{"type":"badge","id":"developer"}""")
            .statusCode(400)
            .body("message", equalTo("You don't own this badge"))
    }

    @Test
    @TestSecurity(user = "73697410", augmentors = [AdminRoleAugmentor::class])
    @OidcSecurity(claims = [Claim(key = "sub", value = "73697410"), Claim(key = "preferred_username", value = "flex3rs")])
    fun `marks configured admins`() {
        given()
            .get("/api/me")
            .then()
            .statusCode(200)
            .body("admin", equalTo(true))
    }

    @Test
    @TestSecurity(user = "100")
    @OidcSecurity(claims = [Claim(key = "sub", value = "100"), Claim(key = "preferred_username", value = "Alice")])
    fun `exports the stored data`() {
        QuarkusTransaction.requiringNew().run { recordDonation("se1", "100") }

        given()
            .get("/api/me/export")
            .then()
            .statusCode(200)
            .header("Content-Disposition", containsString("dankchat-data.json"))
            .body("twitchId", equalTo("100"))
            .body("badges.id", containsInAnyOrder("supporter", "contributor"))
            .body("donations.streamElementsChannelId", contains("se1"))
    }

    @Test
    @TestSecurity(user = "100")
    @OidcSecurity(claims = [Claim(key = "sub", value = "100"), Claim(key = "preferred_username", value = "Alice")])
    fun `deleting removes all data and remembers erased donors`() {
        QuarkusTransaction.requiringNew().run {
            recordDonation("se1", "100")
            grantBadge("200", "contributor")
            UserBadgeEntity.update("grantedBy = ?1 where twitchId = ?2", "100", "200")
        }

        given()
            .delete("/api/me")
            .then()
            .statusCode(204)

        QuarkusTransaction.requiringNew().run {
            assertNull(UserEntity.findById("100"))
            assertEquals(0, UserBadgeEntity.count("twitchId", "100"))
            assertEquals(0, DonationEntity.count())
            assertNotNull(ErasedDonorEntity.findById(channelHash("se1")))
            assertNull(UserBadgeEntity.find("twitchId", "200").firstResult()?.grantedBy)
        }
        given()
            .get("/v1/badges")
            .then()
            .body("users.100", nullValue())
    }

    private fun choose(body: String) = given()
        .contentType(ContentType.JSON)
        .body(body)
        .put("/api/me/badge")
        .then()

    private fun recordDonation(
        seChannelId: String,
        twitchId: String,
    ) {
        DonationEntity()
            .apply {
                this.seChannelId = seChannelId
                this.twitchId = twitchId
                name = "alice"
            }.persist()
    }

    private fun chatColor(color: String) = HelixChatColorsDto(listOf(HelixChatColorDto(userId = "100", color = color)))
}
