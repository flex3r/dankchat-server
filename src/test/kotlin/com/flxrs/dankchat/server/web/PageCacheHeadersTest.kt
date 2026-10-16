package com.flxrs.dankchat.server.web

import io.quarkiverse.quinoa.testing.QuinoaTestProfiles
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
@TestProfile(QuinoaTestProfiles.Enable::class)
class PageCacheHeadersTest {
    @Test
    fun `pages revalidate on every load`() {
        given()
            .get("/delete-account")
            .then()
            .statusCode(200)
            .contentType(containsString("text/html"))
            .header("Cache-Control", equalTo("no-cache"))
    }

    @Test
    fun `assets are still served`() {
        val page = given().get("/").asString()
        val asset = Regex("/_app/immutable/entry/start\\.[^\"]+\\.js").find(page)!!.value
        given()
            .get(asset)
            .then()
            .statusCode(200)
            .contentType(containsString("javascript"))
    }

    @Test
    fun `missing assets are not answered with the page`() {
        given()
            .get("/_app/immutable/chunks/missing.js")
            .then()
            .statusCode(404)
    }
}
