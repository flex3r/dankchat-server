package com.flxrs.dankchat.server.auth

import io.quarkus.security.AuthenticationFailedException
import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.IdentityProvider
import io.quarkus.security.identity.IdentityProviderManager
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.request.AuthenticationRequest
import io.quarkus.security.identity.request.BaseAuthenticationRequest
import io.quarkus.security.runtime.QuarkusPrincipal
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.quarkus.vertx.http.runtime.security.ChallengeData
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils
import io.smallrye.mutiny.Uni
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.HttpHeaders

/** Selects the DankChat app's Twitch token login in `quarkus.http.auth.permission.*.auth-mechanism`. */
const val APP_TOKEN_SCHEME = "dankchat-app"

class AppTokenRequest(
    val token: String,
) : BaseAuthenticationRequest()

/** Reads the Twitch user token the DankChat app sends as `Authorization: Bearer <token>`. */
@ApplicationScoped
class AppTokenMechanism : HttpAuthenticationMechanism {
    override fun authenticate(
        context: RoutingContext,
        identityProviderManager: IdentityProviderManager,
    ): Uni<SecurityIdentity> {
        val header = context.request().getHeader(HttpHeaders.AUTHORIZATION)
        val token = header?.takeIf { it.startsWith(BEARER_PREFIX, ignoreCase = true) }?.substring(BEARER_PREFIX.length)?.trim()
        if (token.isNullOrEmpty()) {
            return Uni.createFrom().nullItem()
        }
        val request = HttpSecurityUtils.setRoutingContextAttribute(AppTokenRequest(token), context)
        return identityProviderManager.authenticate(request)
    }

    override fun getChallenge(context: RoutingContext): Uni<ChallengeData> = Uni.createFrom().item(ChallengeData(401, HttpHeaders.WWW_AUTHENTICATE, "Bearer"))

    override fun getCredentialTypes(): Set<Class<out AuthenticationRequest>> = setOf(AppTokenRequest::class.java)

    override fun getCredentialTransport(context: RoutingContext): Uni<HttpCredentialTransport> =
        Uni.createFrom().item(HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, "bearer", APP_TOKEN_SCHEME))

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}

@ApplicationScoped
class AppTokenIdentityProvider(
    private val appTokens: AppTokens,
) : IdentityProvider<AppTokenRequest> {
    override fun getRequestType(): Class<AppTokenRequest> = AppTokenRequest::class.java

    override fun authenticate(
        request: AppTokenRequest,
        context: AuthenticationRequestContext,
    ): Uni<SecurityIdentity> = context.runBlocking {
        val twitchId = appTokens.twitchIdOf(request.token) ?: throw AuthenticationFailedException("Twitch rejected the token or it belongs to another app")
        QuarkusSecurityIdentity
            .builder()
            .setPrincipal(QuarkusPrincipal(twitchId))
            .build()
    }
}
