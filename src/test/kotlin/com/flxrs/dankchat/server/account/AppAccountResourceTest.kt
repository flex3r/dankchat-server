package com.flxrs.dankchat.server.account

import com.flxrs.dankchat.server.auth.SESSION_COOKIE
import com.flxrs.dankchat.server.grantBadge
import com.flxrs.dankchat.server.resetDatabase
import com.flxrs.dankchat.server.twitch.HelixApi
import com.flxrs.dankchat.server.twitch.HelixChatColorsDto
import com.flxrs.dankchat.server.twitch.HelixUserDto
import com.flxrs.dankchat.server.twitch.HelixUsersDto
import com.flxrs.dankchat.server.twitch.TwitchIdApi
import com.flxrs.dankchat.server.twitch.TwitchValidateDto
import com.flxrs.dankchat.server.user.UserEntity
import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import jakarta.ws.rs.WebApplicationException
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito
import kotlin.test.assertEquals
import kotlin.test.assertNull

@QuarkusTest
class AppAccountResourceTest {
    @InjectMock
    @RestClient
    lateinit var twitchId: TwitchIdApi

    @InjectMock
    @RestClient
    lateinit var helix: HelixApi

    @BeforeEach
    fun setup() {
        resetDatabase()
        Mockito.`when`(helix.chatColors(anyList())).thenReturn(HelixChatColorsDto(emptyList()))
        Mockito.`when`(helix.users(anyList(), anyList())).thenReturn(HelixUsersDto(emptyList()))
        Mockito.`when`(helix.users(emptyList(), listOf("100"))).thenReturn(HelixUsersDto(listOf(HelixUserDto(id = "100", login = "alice", displayName = "Alice"))))
        Mockito.`when`(twitchId.validate("OAuth $APP_TOKEN")).thenReturn(TwitchValidateDto(clientId = APP_CLIENT_ID, userId = "100"))
        Mockito.`when`(twitchId.validate("OAuth $ADMIN_TOKEN")).thenReturn(TwitchValidateDto(clientId = APP_CLIENT_ID, userId = ADMIN_ID))
        Mockito.`when`(twitchId.validate("OAuth $OTHER_APP_TOKEN")).thenReturn(TwitchValidateDto(clientId = "another-app", userId = "100"))
        Mockito.`when`(twitchId.validate("OAuth $EXPIRED_TOKEN")).thenThrow(WebApplicationException(401))
        Mockito.`when`(twitchId.validate("OAuth $NEW_USER_TOKEN")).thenReturn(TwitchValidateDto(clientId = APP_CLIENT_ID, userId = NEW_USER_ID))
        QuarkusTransaction.requiringNew().run {
            grantBadge("100", "supporter")
            grantBadge("100", "contributor")
        }
    }

    @Test
    fun `requires the app's token`() {
        given()
            .get("/v1/me")
            .then()
            .statusCode(401)
            .header("WWW-Authenticate", "Bearer")
    }

    @Test
    fun `rejects tokens Twitch doesn't accept`() {
        app(EXPIRED_TOKEN)
            .get("/v1/me")
            .then()
            .statusCode(401)
    }

    @Test
    fun `rejects tokens of other apps`() {
        app(OTHER_APP_TOKEN)
            .get("/v1/me")
            .then()
            .statusCode(401)
    }

    @Test
    fun `shows the account for the app's token`() {
        app(APP_TOKEN)
            .get("/v1/me")
            .then()
            .statusCode(200)
            .body("twitchId", equalTo("100"))
            .body("displayName", equalTo("Alice"))
            .body("badges.id", contains("contributor", "supporter"))
            .body("choice.type", equalTo("automatic"))
    }

    @Test
    fun `reading the account stores nothing for new users`() {
        app(NEW_USER_TOKEN)
            .get("/v1/me")
            .then()
            .statusCode(200)
            .body("twitchId", equalTo(NEW_USER_ID))
            .body("badges", empty<Any>())
            .body("choice.type", equalTo("automatic"))

        assertNull(QuarkusTransaction.requiringNew().call { UserEntity.findById(NEW_USER_ID) })
    }

    @Test
    fun `choosing stores the choice for new users`() {
        app(NEW_USER_TOKEN)
            .contentType(ContentType.JSON)
            .body("""{"type":"hidden"}""")
            .put("/v1/me/badge")
            .then()
            .statusCode(200)
            .body("choice.type", equalTo("hidden"))

        assertEquals(true, QuarkusTransaction.requiringNew().call { UserEntity.findById(NEW_USER_ID)?.hideBadges })
    }

    @Test
    fun `chooses a badge with the app's token`() {
        app(APP_TOKEN)
            .contentType(ContentType.JSON)
            .body("""{"type":"badge","id":"supporter"}""")
            .put("/v1/me/badge")
            .then()
            .statusCode(200)
            .body("choice.id", equalTo("supporter"))
            .body("shownBadgeId", equalTo("supporter"))
    }

    @Test
    fun `the website session doesn't open the app API`() {
        given()
            .cookie(SESSION_COOKIE, signIn(APP_TOKEN))
            .get("/v1/me")
            .then()
            .statusCode(401)
    }

    @Test
    fun `a login code opens the account page signed in`() {
        val url =
            app(APP_TOKEN)
                .post("/v1/me/login-code")
                .then()
                .statusCode(200)
                .body("url", startsWith("https://dank.chat/login/app?code="))
                .extract()
                .path<String>("url")

        val session =
            given()
                .redirects()
                .follow(false)
                .get(url.removePrefix("https://dank.chat"))
                .then()
                .statusCode(303)
                .header("Location", "/account")
                .extract()
                .cookie(SESSION_COOKIE)

        given()
            .cookie(SESSION_COOKIE, session)
            .get("/api/me")
            .then()
            .statusCode(200)
            .body("twitchId", equalTo("100"))
            .body("admin", equalTo(false))
    }

    @Test
    fun `app sessions show and store the Twitch display name`() {
        given()
            .cookie(SESSION_COOKIE, signIn(APP_TOKEN))
            .get("/api/me")
            .then()
            .statusCode(200)
            .body("displayName", equalTo("Alice"))

        assertEquals("Alice", QuarkusTransaction.requiringNew().call { UserEntity.findById("100")?.displayName })
    }

    @Test
    fun `app sessions open the account when the display name lookup fails`() {
        Mockito.`when`(helix.users(emptyList(), listOf("100"))).thenThrow(WebApplicationException(503))

        given()
            .cookie(SESSION_COOKIE, signIn(APP_TOKEN))
            .get("/api/me")
            .then()
            .statusCode(200)
            .body("twitchId", equalTo("100"))
            .body("displayName", nullValue())
    }

    @Test
    fun `login codes work once`() {
        val path = loginCodePath(APP_TOKEN)
        given()
            .redirects()
            .follow(false)
            .get(path)
            .then()
            .statusCode(303)

        given()
            .redirects()
            .follow(false)
            .get(path)
            .then()
            .statusCode(303)
            .header("Location", "/login")
    }

    @Test
    fun `unknown login codes fall back to the Twitch login`() {
        given()
            .redirects()
            .follow(false)
            .get("/login/app?code=unknown")
            .then()
            .statusCode(303)
            .header("Location", "/login")
    }

    @Test
    fun `app sessions keep the admin role`() {
        given()
            .cookie(SESSION_COOKIE, signIn(ADMIN_TOKEN))
            .get("/api/me")
            .then()
            .statusCode(200)
            .body("admin", equalTo(true))
    }

    @Test
    fun `changed session cookies stay signed out without breaking public pages`() {
        val (_, expiresAt, signature) = signIn(APP_TOKEN).split('.')
        val forged = "$ADMIN_ID.$expiresAt.$signature"

        given()
            .cookie(SESSION_COOKIE, forged)
            .get("/api/me")
            .then()
            .statusCode(401)
        given()
            .cookie(SESSION_COOKIE, forged)
            .get("/v1/badges")
            .then()
            .statusCode(200)
    }

    @Test
    fun `signing out clears the app session`() {
        val maxAge =
            given()
                .cookie(SESSION_COOKIE, signIn(APP_TOKEN))
                .post("/logout")
                .then()
                .statusCode(204)
                .extract()
                .detailedCookie(SESSION_COOKIE)
                .maxAge

        assertEquals(0L, maxAge)
    }

    @Test
    fun `deleting the account clears the app session`() {
        val maxAge =
            given()
                .cookie(SESSION_COOKIE, signIn(APP_TOKEN))
                .delete("/api/me")
                .then()
                .statusCode(204)
                .extract()
                .detailedCookie(SESSION_COOKIE)
                .maxAge

        assertEquals(0L, maxAge)
        assertNull(QuarkusTransaction.requiringNew().call { UserEntity.findById("100") })
    }

    private fun app(token: String): RequestSpecification = given().header("Authorization", "Bearer $token")

    private fun loginCodePath(token: String): String = app(token)
        .post("/v1/me/login-code")
        .then()
        .statusCode(200)
        .extract()
        .path<String>("url")
        .removePrefix("https://dank.chat")

    private fun signIn(token: String): String = given()
        .redirects()
        .follow(false)
        .get(loginCodePath(token))
        .then()
        .statusCode(303)
        .extract()
        .cookie(SESSION_COOKIE)

    companion object {
        private const val APP_CLIENT_ID = "xu7vd1i6tlr0ak45q1li2wdc0lrma8"
        private const val ADMIN_ID = "73697410"
        private const val APP_TOKEN = "app-token"
        private const val ADMIN_TOKEN = "admin-token"
        private const val OTHER_APP_TOKEN = "other-app-token"
        private const val EXPIRED_TOKEN = "expired-token"
        private const val NEW_USER_TOKEN = "new-user-token"
        private const val NEW_USER_ID = "300"
    }
}
