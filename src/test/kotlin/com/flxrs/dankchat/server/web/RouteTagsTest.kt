package com.flxrs.dankchat.server.web

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@QuarkusTest
class RouteTagsTest {
    @Test
    fun `not modified responses keep their endpoint`() {
        val etag = given().get("/v1/badges").header("ETag")
        given()
            .header("If-None-Match", etag)
            .get("/v1/badges")
            .then()
            .statusCode(304)

        given()
            .get("/q/metrics")
            .then()
            .body(containsString("status=\"304\",uri=\"/v1/badges\""))
            .body(not(containsString("uri=\"REDIRECTION\"")))
    }

    @Test
    fun `routes are named after their template or page`() {
        assertEquals("/badges/{name}", routeOf(path = "/badges/a1b2.png", template = "/badges/{name}", status = 304))
        assertEquals("root", routeOf(path = "/", template = null, status = 200))
        assertEquals("/privacy", routeOf(path = "/privacy/", template = null, status = 304))
        assertEquals("/_app/{asset}", routeOf(path = "/_app/immutable/entry/start.js", template = null, status = 200))
        assertEquals("NOT_FOUND", routeOf(path = "/wp-login.php", template = null, status = 404))
        assertEquals("/{other}", routeOf(path = "/admin/controller/extension", template = null, status = 200))
    }
}
