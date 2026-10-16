package com.flxrs.dankchat.server.account

import com.flxrs.dankchat.server.auth.LoginCodes
import com.flxrs.dankchat.server.auth.SessionCookies
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response

/** Entry point for the Twitch login; the code flow returns here and the user lands on their account page. */
@Path("/login")
@Authenticated
class LoginResource {
    @GET
    fun login(): Response = seeOther("/account").build()
}

/** Opens the account page for a login code from the app; unknown or expired codes fall back to the Twitch login. */
@Path("/login/app")
class AppLoginResource(
    private val identity: SecurityIdentity,
    private val loginCodes: LoginCodes,
    private val cookies: SessionCookies,
    private val sessions: AccountSessions,
) {
    @GET
    fun redeem(
        @QueryParam("code") code: String?,
    ): Response {
        val twitchId = code?.let(loginCodes::redeem) ?: return seeOther("/login").build()
        sessions.endTwitchSession(identity)
        return seeOther("/account").cookie(cookies.issue(twitchId)).build()
    }
}

@Path("/logout")
@Authenticated
class LogoutResource(
    private val identity: SecurityIdentity,
    private val sessions: AccountSessions,
) {
    @POST
    fun logout(): Response = Response
        .noContent()
        .cookie(sessions.end(identity))
        .build()
}

// A relative Location, so the redirect keeps the public https origin behind the proxy.
private fun seeOther(path: String): Response.ResponseBuilder = Response
    .status(Response.Status.SEE_OTHER)
    .header(HttpHeaders.LOCATION, path)
