package com.flxrs.dankchat.server.admin

import com.flxrs.dankchat.server.twitchId
import io.quarkus.security.identity.SecurityIdentity
import jakarta.annotation.security.RolesAllowed
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.RestForm
import org.jboss.resteasy.reactive.multipart.FileUpload
import java.nio.file.Files

@Path("/api/admin/badges")
@RolesAllowed(ADMIN_ROLE)
@Produces(MediaType.APPLICATION_JSON)
class AdminBadgesResource(
    private val identity: SecurityIdentity,
    private val badges: AdminBadgeService,
) {
    @GET
    fun badges(): List<AdminBadgeDto> = badges.badges()

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun create(
        @RestForm id: String,
        @RestForm title: String,
        @RestForm priority: Int,
        @RestForm image: FileUpload,
    ): AdminBadgeDto = badges.create(id = id.trim(), title = title, priority = priority, image = image.toUpload())

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun update(
        @PathParam("id") id: String,
        @RestForm title: String,
        @RestForm priority: Int,
        @RestForm image: FileUpload?,
    ): AdminBadgeDto = badges.update(id = id, title = title, priority = priority, image = image?.toUpload())

    @DELETE
    @Path("/{id}")
    fun delete(
        @PathParam("id") id: String,
    ) {
        badges.delete(id)
    }

    @GET
    @Path("/{id}/holders")
    fun holders(
        @PathParam("id") id: String,
    ): List<BadgeHolderDto> = badges.holders(id)

    @POST
    @Path("/{id}/holders")
    @Consumes(MediaType.APPLICATION_JSON)
    fun grant(
        @PathParam("id") id: String,
        request: GrantBadgeRequest,
    ): BadgeHolderDto = badges.grant(id = id, user = request.user, grantedBy = identity.twitchId)

    @DELETE
    @Path("/{id}/holders/{twitchId}")
    fun revoke(
        @PathParam("id") id: String,
        @PathParam("twitchId") twitchId: String,
    ) {
        badges.revoke(id = id, twitchId = twitchId)
    }

    private fun FileUpload.toUpload(): ByteArray = Files.readAllBytes(uploadedFile())
}
