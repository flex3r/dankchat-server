package com.flxrs.dankchat.server.admin

import com.flxrs.dankchat.server.twitchId
import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.SecurityIdentityAugmentor
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped

const val ADMIN_ROLE = "admin"

@ApplicationScoped
class AdminRoleAugmentor(
    private val config: AdminConfig,
) : SecurityIdentityAugmentor {
    override fun augment(
        identity: SecurityIdentity,
        context: AuthenticationRequestContext,
    ): Uni<SecurityIdentity> {
        val augmented =
            when {
                !identity.isAnonymous && identity.twitchId in config.twitchIds() -> QuarkusSecurityIdentity.builder(identity).addRole(ADMIN_ROLE).build()
                else -> identity
            }
        return Uni.createFrom().item(augmented)
    }
}
