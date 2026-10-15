package com.flxrs.dankchat.server.account

import io.quarkus.oidc.OidcSession
import io.quarkus.security.Authenticated
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response

/** Entry point for the Twitch login; the code flow returns here and the user lands on their account page. */
@Path("/login")
@Authenticated
class LoginResource {
    @GET
    fun login(): Response = Response
        .status(Response.Status.SEE_OTHER)
        .header(HttpHeaders.LOCATION, "/account")
        .build()
}

@Path("/logout")
@Authenticated
class LogoutResource(
    private val session: OidcSession,
) {
    @POST
    fun logout(): Response {
        session.logout().await().indefinitely()
        return Response.noContent().build()
    }
}
