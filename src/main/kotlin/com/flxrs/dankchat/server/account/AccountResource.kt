package com.flxrs.dankchat.server.account

import com.flxrs.dankchat.server.admin.ADMIN_ROLE
import com.flxrs.dankchat.server.twitchId
import io.quarkus.oidc.IdToken
import io.quarkus.oidc.OidcSession
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/me")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
class AccountResource(
    private val identity: SecurityIdentity,
    private val accounts: AccountService,
    private val session: OidcSession,
) {
    @Inject
    @IdToken
    lateinit var idToken: JsonWebToken

    @GET
    fun account(): AccountDto = accounts.account(
        twitchId = identity.twitchId,
        displayName = idToken.getClaim<String>("preferred_username"),
        admin = identity.hasRole(ADMIN_ROLE),
    )

    @GET
    @Path("/export")
    fun export(): Response = Response
        .ok(accounts.export(identity.twitchId))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dankchat-data.json\"")
        .build()

    @DELETE
    fun delete(): Response {
        accounts.delete(identity.twitchId)
        session.logout().await().indefinitely()
        return Response.noContent().build()
    }

    @PUT
    @Path("/badge")
    @Consumes(MediaType.APPLICATION_JSON)
    fun choose(choice: BadgeChoice): AccountDto = accounts.choose(
        twitchId = identity.twitchId,
        choice = choice,
        admin = identity.hasRole(ADMIN_ROLE),
    )
}
