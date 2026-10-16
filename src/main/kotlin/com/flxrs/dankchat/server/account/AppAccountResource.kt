package com.flxrs.dankchat.server.account

import com.flxrs.dankchat.server.admin.ADMIN_ROLE
import com.flxrs.dankchat.server.auth.AppConfig
import com.flxrs.dankchat.server.auth.LoginCodes
import com.flxrs.dankchat.server.twitchId
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import kotlinx.serialization.Serializable

/** The account API for the DankChat app, signed in with the app's Twitch token. */
@Path("/v1/me")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
class AppAccountResource(
    private val identity: SecurityIdentity,
    private val accounts: AccountService,
    private val loginCodes: LoginCodes,
    private val config: AppConfig,
) {
    @GET
    fun account(): AccountDto = accounts.account(
        twitchId = identity.twitchId,
        displayName = null,
        admin = identity.hasRole(ADMIN_ROLE),
    )

    @PUT
    @Path("/badge")
    @Consumes(MediaType.APPLICATION_JSON)
    fun choose(choice: BadgeChoice): AccountDto = accounts.choose(
        twitchId = identity.twitchId,
        choice = choice,
        admin = identity.hasRole(ADMIN_ROLE),
    )

    /** A link that opens the account page on the site already signed in. */
    @POST
    @Path("/login-code")
    fun loginCode(): LoginCodeDto {
        val code = loginCodes.issue(identity.twitchId)
        return LoginCodeDto(url = "${config.siteUrl().trimEnd('/')}/login/app?code=$code")
    }
}

@Serializable
data class LoginCodeDto(
    val url: String,
)
