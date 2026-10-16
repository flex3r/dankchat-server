package com.flxrs.dankchat.server.emote

import com.flxrs.dankchat.server.apiError
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/legacy/sets")
class LegacyEmoteSetsResource(
    private val emoteSets: LegacyEmoteSets,
) {
    /** Takes repeated or comma separated `id` parameters, like the old DankChat API. */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun sets(
        @QueryParam("id") idParams: List<String>,
    ): List<LegacyEmoteSetDto> {
        val ids = idParams.flatMap { it.split(",") }.map { it.trim() }.filter { it.isNotEmpty() }
        if (ids.isEmpty()) {
            apiError(Response.Status.BAD_REQUEST, "Invalid parameters")
        }
        return emoteSets.emoteSets(ids).ifEmpty { apiError(Response.Status.NOT_FOUND, "No emote sets found") }
    }
}
