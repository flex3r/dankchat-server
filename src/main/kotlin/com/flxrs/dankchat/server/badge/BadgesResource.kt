package com.flxrs.dankchat.server.badge

import io.smallrye.common.annotation.NonBlocking
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.EntityTag
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Request
import jakarta.ws.rs.core.Response

@Path("/v1/badges")
class BadgesResource(
    private val snapshots: BadgeSnapshotService,
) {
    @GET
    @NonBlocking
    @Produces(MediaType.APPLICATION_JSON)
    fun badges(
        @Context request: Request,
    ): Response {
        val snapshot = snapshots.current
        val etag = EntityTag(snapshot.etag)
        val response = request.evaluatePreconditions(etag) ?: Response.ok(snapshot.body)
        return response
            .tag(etag)
            .header(HttpHeaders.CACHE_CONTROL, "no-cache, public")
            .header("Access-Control-Allow-Origin", "*")
            .build()
    }
}
