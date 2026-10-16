package com.flxrs.dankchat.server.auth

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

class SessionCookieRequest(
    val twitchId: String,
) : BaseAuthenticationRequest()

/**
 * Signs in requests carrying a [SESSION_COOKIE]. Ranks below the Twitch code flow, which keeps creating the challenge for
 * signed-out users. Invalid cookies leave the request anonymous instead of failing it, so public pages keep working.
 */
@ApplicationScoped
class SessionCookieMechanism(
    private val cookies: SessionCookies,
) : HttpAuthenticationMechanism {
    override fun authenticate(
        context: RoutingContext,
        identityProviderManager: IdentityProviderManager,
    ): Uni<SecurityIdentity> {
        val twitchId = context
            .request()
            .getCookie(SESSION_COOKIE)
            ?.value
            ?.let(cookies::verify) ?: return Uni.createFrom().nullItem()
        val request = HttpSecurityUtils.setRoutingContextAttribute(SessionCookieRequest(twitchId), context)
        return identityProviderManager.authenticate(request)
    }

    override fun getChallenge(context: RoutingContext): Uni<ChallengeData> = Uni.createFrom().item(ChallengeData(401))

    override fun getCredentialTypes(): Set<Class<out AuthenticationRequest>> = setOf(SessionCookieRequest::class.java)

    override fun getCredentialTransport(context: RoutingContext): Uni<HttpCredentialTransport> = Uni.createFrom().item(HttpCredentialTransport(HttpCredentialTransport.Type.COOKIE, SESSION_COOKIE))
}

@ApplicationScoped
class SessionCookieIdentityProvider : IdentityProvider<SessionCookieRequest> {
    override fun getRequestType(): Class<SessionCookieRequest> = SessionCookieRequest::class.java

    override fun authenticate(
        request: SessionCookieRequest,
        context: AuthenticationRequestContext,
    ): Uni<SecurityIdentity> = Uni.createFrom().item(
        QuarkusSecurityIdentity
            .builder()
            .setPrincipal(QuarkusPrincipal(request.twitchId))
            .build(),
    )
}
