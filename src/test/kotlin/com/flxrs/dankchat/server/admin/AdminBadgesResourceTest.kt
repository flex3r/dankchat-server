package com.flxrs.dankchat.server.admin

import com.flxrs.dankchat.server.grantBadge
import com.flxrs.dankchat.server.resetDatabase
import com.flxrs.dankchat.server.twitch.HelixApi
import com.flxrs.dankchat.server.twitch.HelixUserDto
import com.flxrs.dankchat.server.twitch.HelixUsersDto
import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.matchesPattern
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import kotlin.test.assertContentEquals

@QuarkusTest
@TestSecurity(user = "73697410", augmentors = [AdminRoleAugmentor::class])
class AdminBadgesResourceTest {
    @InjectMock
    @RestClient
    lateinit var helix: HelixApi

    @BeforeEach
    fun setup() {
        resetDatabase()
        QuarkusTransaction.requiringNew().run {
            grantBadge("100", "supporter")
        }
    }

    @Test
    @TestSecurity(user = "100", augmentors = [AdminRoleAugmentor::class])
    fun `is admin only`() {
        given()
            .get("/api/admin/badges")
            .then()
            .statusCode(403)
    }

    @Test
    fun `lists badges with holder counts`() {
        given()
            .get("/api/admin/badges")
            .then()
            .statusCode(200)
            .body("id", contains("developer", "contributor", "supporter"))
            .body("find { it.id == 'supporter' }.holders", equalTo(1))
    }

    @Test
    fun `creates a badge and serves its image`() {
        val url =
            createBadge(id = "borgir", image = PNG)
                .statusCode(200)
                .body("title", equalTo("Borgir"))
                .body("url", matchesPattern("https://api\\.dank\\.chat/badges/borgir-[0-9a-f]{12}\\.png"))
                .extract()
                .path<String>("url")

        val bytes =
            given()
                .get(url.removePrefix("https://api.dank.chat"))
                .then()
                .statusCode(200)
                .contentType("image/png")
                .header("Cache-Control", "public, max-age=31536000, immutable")
                .extract()
                .asByteArray()
        assertContentEquals(PNG, bytes)

        given()
            .get("/v1/badges")
            .then()
            .body("badges.id", hasItem("borgir"))
    }

    @Test
    fun `rejects invalid badges`() {
        createBadge(id = "Not Valid", image = PNG).statusCode(400)
        createBadge(id = "borgir", image = "not an image".toByteArray())
            .statusCode(400)
            .body("message", equalTo("Images must be PNG, GIF or WebP"))
        createBadge(id = "supporter", image = PNG).statusCode(409)
    }

    @Test
    fun `updates a badge without replacing the image`() {
        val url = createBadge(id = "borgir", image = PNG).extract().path<String>("url")

        given()
            .multiPart("title", "Borgir Connoisseur")
            .multiPart("priority", "5")
            .put("/api/admin/badges/borgir")
            .then()
            .statusCode(200)
            .body("title", equalTo("Borgir Connoisseur"))
            .body("priority", equalTo(5))
            .body("url", equalTo(url))
    }

    @Test
    fun `deletes badges except the supporter badge`() {
        createBadge(id = "borgir", image = PNG)

        given().delete("/api/admin/badges/supporter").then().statusCode(409)
        given().delete("/api/admin/badges/borgir").then().statusCode(204)
        given()
            .get("/api/admin/badges")
            .then()
            .body("id", not(hasItem("borgir")))
    }

    @Test
    fun `grants and revokes badges by login`() {
        Mockito.`when`(helix.users(listOf("alice"), emptyList())).thenReturn(HelixUsersDto(listOf(ALICE)))
        Mockito.`when`(helix.users(listOf("nobody"), emptyList())).thenReturn(HelixUsersDto(emptyList()))

        grant("@Alice")
            .statusCode(200)
            .body("twitchId", equalTo("200"))
            .body("displayName", equalTo("Alice"))
            .body("source", equalTo("GRANT"))
            .body("grantedBy", equalTo("73697410"))
        grant("alice").statusCode(409)
        grant("nobody").statusCode(404)

        given()
            .get("/api/admin/badges/contributor/holders")
            .then()
            .body("twitchId", contains("200"))
        given()
            .get("/v1/badges")
            .then()
            .body("users.200", equalTo("contributor"))

        given().delete("/api/admin/badges/contributor/holders/200").then().statusCode(204)
        given().delete("/api/admin/badges/contributor/holders/200").then().statusCode(404)
    }

    private fun createBadge(
        id: String,
        image: ByteArray,
    ) = given()
        .multiPart("id", id)
        .multiPart("title", "Borgir")
        .multiPart("priority", "50")
        .multiPart("image", "badge.png", image, "image/png")
        .post("/api/admin/badges")
        .then()

    @Test
    fun `grants badges by user id`() {
        Mockito.`when`(helix.users(listOf("200"), listOf("200"))).thenReturn(HelixUsersDto(listOf(ALICE)))

        grant("200")
            .statusCode(200)
            .body("twitchId", equalTo("200"))
            .body("login", equalTo("alice"))
    }

    @Test
    fun `fills in missing holder names from Twitch`() {
        Mockito
            .`when`(helix.users(emptyList(), listOf("100")))
            .thenReturn(HelixUsersDto(listOf(HelixUserDto(id = "100", login = "bob", displayName = "Bob"))))

        given()
            .get("/api/admin/badges/supporter/holders")
            .then()
            .statusCode(200)
            .body("login", contains("bob"))
            .body("displayName", contains("Bob"))
        given()
            .get("/api/admin/badges/supporter/holders")
            .then()
            .body("login", contains("bob"))
        Mockito.verify(helix, Mockito.times(1)).users(emptyList(), listOf("100"))
    }

    private fun grant(login: String) = given()
        .contentType(ContentType.JSON)
        .body("""{"user":"$login"}""")
        .post("/api/admin/badges/contributor/holders")
        .then()

    companion object {
        private val ALICE = HelixUserDto(id = "200", login = "alice", displayName = "Alice")
        private val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3)
    }
}
