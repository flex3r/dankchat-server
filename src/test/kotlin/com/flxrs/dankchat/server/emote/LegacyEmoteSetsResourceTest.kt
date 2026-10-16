package com.flxrs.dankchat.server.emote

import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.ws.rs.WebApplicationException
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito

// Sets stay cached between tests, so every test asks for its own set ids
@QuarkusTest
class LegacyEmoteSetsResourceTest {
    @InjectMock
    @RestClient
    lateinit var ivr: IvrApi

    @BeforeEach
    fun setup() {
        Mockito.`when`(ivr.emoteSets(anyString())).thenAnswer { invocation ->
            (invocation.arguments[0] as String).split(",").mapNotNull { KNOWN_SETS[it] }
        }
    }

    @Test
    fun `serves sets in the old DankChat API format`() {
        given()
            .queryParam("id", "global-1,sub-1")
            .get("/legacy/sets")
            .then()
            .statusCode(200)
            .contentType("application/json")
            .body("set_id", containsInAnyOrder("global-1", "sub-1"))
            .body("find { it.set_id == 'global-1' }.channel_name", equalTo("Twitch"))
            .body("find { it.set_id == 'global-1' }.channel_id", equalTo("0"))
            .body("find { it.set_id == 'global-1' }.tier", equalTo(1))
            .body("find { it.set_id == 'sub-1' }.channel_name", equalTo("forsen"))
            .body("find { it.set_id == 'sub-1' }.channel_id", equalTo("22484632"))
            .body("find { it.set_id == 'sub-1' }.tier", equalTo(2))
            .body("find { it.set_id == 'sub-1' }.emotes[0].code", equalTo("forsenE"))
            .body("find { it.set_id == 'sub-1' }.emotes[0].id", equalTo("emotesv2_1"))
            .body("find { it.set_id == 'sub-1' }.emotes[0].type", equalTo("SUBSCRIPTIONS"))
            .body("find { it.set_id == 'sub-1' }.emotes[0].assetType", equalTo("STATIC"))
    }

    @Test
    fun `shows Twitch partner sets as Twitch`() {
        given()
            .queryParam("id", "partner-1")
            .get("/legacy/sets")
            .then()
            .statusCode(200)
            .body("[0].channel_name", equalTo("Twitch"))
    }

    @Test
    fun `accepts repeated id parameters and leaves out unknown sets`() {
        given()
            .queryParam("id", "sub-2", "unknown", "sub-2")
            .get("/legacy/sets")
            .then()
            .statusCode(200)
            .body("set_id", containsInAnyOrder("sub-2"))
    }

    @Test
    fun `asks ivr only for sets it hasn't cached`() {
        repeat(2) {
            given()
                .queryParam("id", "sub-3")
                .get("/legacy/sets")
                .then()
                .statusCode(200)
        }
        Mockito.verify(ivr, Mockito.times(1)).emoteSets("sub-3")
    }

    @Test
    fun `asks ivr for at most 50 sets at once`() {
        val ids = (1..51).map { "batch-$it" }
        given()
            .queryParam("id", ids.joinToString(","))
            .get("/legacy/sets")
            .then()
            .statusCode(404)
        Mockito.verify(ivr, Mockito.times(2)).emoteSets(anyString())
    }

    @Test
    fun `answers 404 when ivr fails`() {
        Mockito.`when`(ivr.emoteSets("failing-1")).thenThrow(WebApplicationException(503))
        given()
            .queryParam("id", "failing-1")
            .get("/legacy/sets")
            .then()
            .statusCode(404)
    }

    @Test
    fun `requires set ids`() {
        given()
            .get("/legacy/sets")
            .then()
            .statusCode(400)
        given()
            .queryParam("id", " , ")
            .get("/legacy/sets")
            .then()
            .statusCode(400)
    }

    companion object {
        private val KNOWN_SETS =
            listOf(
                IvrEmoteSetDto(id = "global-1", emotes = listOf(IvrEmoteDto(code = "Kappa", id = "25", type = "GLOBALS", assetType = "STATIC"))),
                IvrEmoteSetDto(
                    id = "sub-1",
                    channelLogin = "forsen",
                    channelId = "22484632",
                    tier = "2",
                    emotes = listOf(IvrEmoteDto(code = "forsenE", id = "emotesv2_1", type = "SUBSCRIPTIONS", assetType = "STATIC")),
                ),
                IvrEmoteSetDto(id = "sub-2", channelLogin = "forsen", channelId = "22484632", tier = "1"),
                IvrEmoteSetDto(id = "sub-3", channelLogin = "forsen", channelId = "22484632", tier = "1"),
                IvrEmoteSetDto(id = "partner-1", channelLogin = "qa_TW_Partner", channelId = "1"),
            ).associateBy { it.id }
    }
}
