package com.flxrs.dankchat.server.badge

import io.smallrye.common.annotation.NonBlocking
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/legacy/badges")
class LegacyBadgesResource(
    private val snapshots: BadgeSnapshotService,
) {
    @GET
    @NonBlocking
    @Produces(MediaType.APPLICATION_JSON)
    fun badges(
        @HeaderParam(HttpHeaders.IF_NONE_MATCH) ifNoneMatch: String?,
    ): Response = snapshotResponse(snapshots.legacy, ifNoneMatch)
}
