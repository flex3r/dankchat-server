package com.flxrs.dankchat.server.badge

import io.smallrye.common.annotation.NonBlocking
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.EntityTag
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/v1/badges")
class BadgesResource(
    private val snapshots: BadgeSnapshotService,
) {
    @GET
    @NonBlocking
    @Produces(MediaType.APPLICATION_JSON)
    fun badges(
        @HeaderParam(HttpHeaders.IF_NONE_MATCH) ifNoneMatch: String?,
    ): Response {
        val snapshot = snapshots.current
        val response = when {
            matchesEtag(ifNoneMatch, snapshot.etag) -> Response.notModified()
            else -> Response.ok(snapshot.body)
        }
        return response
            .tag(EntityTag(snapshot.etag))
            .header(HttpHeaders.CACHE_CONTROL, "no-cache, public")
            .header("Access-Control-Allow-Origin", "*")
            .build()
    }
}

/** If-None-Match uses weak comparison, proxies like Cloudflare weaken ETags when they re-compress. */
private fun matchesEtag(
    ifNoneMatch: String?,
    etag: String,
): Boolean {
    ifNoneMatch ?: return false
    return ifNoneMatch.split(',').any { candidate ->
        val tag = candidate.trim().removePrefix("W/")
        tag == "*" || tag == "\"$etag\""
    }
}
