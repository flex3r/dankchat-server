package com.flxrs.dankchat.server.badge

import jakarta.transaction.Transactional
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response

@Path("/badges")
class BadgeImagesResource {
    @GET
    @Path("/{name}")
    @Transactional
    fun image(
        @PathParam("name") name: String,
    ): Response {
        val image = BadgeImageEntity.findById(name) ?: throw NotFoundException()
        return Response
            .ok(image.data, image.contentType)
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
            .header("Access-Control-Allow-Origin", "*")
            .build()
    }
}
