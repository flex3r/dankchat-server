package com.flxrs.dankchat.server.account

import com.flxrs.dankchat.server.auth.SessionCookies
import io.quarkus.oidc.OidcSession
import io.quarkus.security.identity.SecurityIdentity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.NewCookie
import org.eclipse.microprofile.jwt.JsonWebToken

/** Website sessions come from the Twitch code flow or from a login code opened in the app. */
@ApplicationScoped
class AccountSessions(
    private val oidcSession: OidcSession,
    private val cookies: SessionCookies,
) {
    /** Ends the user's session and returns the cookie that clears an app session. */
    fun end(identity: SecurityIdentity): NewCookie {
        endTwitchSession(identity)
        return cookies.expired()
    }

    fun endTwitchSession(identity: SecurityIdentity) {
        if (identity.principal is JsonWebToken) {
            oidcSession.logout().await().indefinitely()
        }
    }
}
