package com.flxrs.dankchat.server

import io.quarkus.security.identity.SecurityIdentity
import org.eclipse.microprofile.jwt.JsonWebToken

/** The Twitch user id, which Twitch puts in the ID token subject. */
val SecurityIdentity.twitchId: String
    get() = (principal as? JsonWebToken)?.subject ?: principal.name
