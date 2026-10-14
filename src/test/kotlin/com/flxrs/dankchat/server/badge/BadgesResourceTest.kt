package com.flxrs.dankchat.server.badge

import com.flxrs.dankchat.server.grantBadge
import com.flxrs.dankchat.server.resetDatabase
import com.flxrs.dankchat.server.user.UserEntity
import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.enterprise.event.Event
import jakarta.inject.Inject
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
class BadgesResourceTest {
    @Inject
    lateinit var snapshots: BadgeSnapshotService

    @Inject
    lateinit var badgesChanged: Event<BadgesChanged>

    @BeforeEach
    fun setup() {
        resetDatabase()
        QuarkusTransaction.requiringNew().run {
            grantBadge("1", "supporter")
            grantBadge("1", "contributor")
            grantBadge("2", "supporter")
            grantBadge("2", "contributor")
            grantBadge("3", "supporter")
            grantBadge("4", "supporter")
            UserEntity.update("selectedBadgeId = 'supporter' where twitchId = '2'")
            UserEntity.update("hideBadges = true where twitchId = '3'")
            UserEntity.update("selectedBadgeId = 'developer' where twitchId = '4'")
        }
        snapshots.rebuild()
    }

    @Test
    fun `serves one badge per user with cors and cache headers`() {
        given()
            .header("Origin", "https://example.com")
            .get("/v1/badges")
            .then()
            .statusCode(200)
            .contentType("application/json")
            .header("Access-Control-Allow-Origin", "*")
            .header("Cache-Control", "no-cache, public")
            .header("ETag", matchesPattern("\"[0-9a-f]{64}\""))
            .body("users", equalTo(mapOf("1" to "contributor", "2" to "supporter", "4" to "supporter")))
            .body("badges.id", hasItems("developer", "top-supporter", "contributor", "supporter"))
    }

    @Test
    fun `answers not modified for a matching etag`() {
        val etag = given()
            .get("/v1/badges")
            .then()
            .extract()
            .header("ETag")

        given()
            .header("If-None-Match", etag)
            .get("/v1/badges")
            .then()
            .statusCode(304)
            .header("ETag", etag)
    }

    @Test
    fun `answers not modified for a weakened etag in a list`() {
        val etag = given()
            .get("/v1/badges")
            .then()
            .extract()
            .header("ETag")

        given()
            .header("If-None-Match", "\"stale\", W/$etag")
            .get("/v1/badges")
            .then()
            .statusCode(304)
            .header("ETag", etag)
    }

    @Test
    fun `snapshot updates once a change commits`() {
        val etag = given()
            .get("/v1/badges")
            .then()
            .extract()
            .header("ETag")

        QuarkusTransaction.requiringNew().run {
            UserEntity.update("hideBadges = false where twitchId = '3'")
            badgesChanged.fire(BadgesChanged(reason = "test"))
        }

        given()
            .header("If-None-Match", etag)
            .get("/v1/badges")
            .then()
            .statusCode(200)
            .body("users.3", equalTo("supporter"))
    }
}
