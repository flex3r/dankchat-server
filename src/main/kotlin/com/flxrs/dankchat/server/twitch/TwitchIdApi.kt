package com.flxrs.dankchat.server.twitch

import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "twitch-id")
@Path("/oauth2")
interface TwitchIdApi {
    /** Expects `OAuth <token>`; Twitch answers 401 for invalid or expired tokens. */
    @GET
    @Path("/validate")
    fun validate(
        @HeaderParam("Authorization") authorization: String,
    ): TwitchValidateDto
}

@Serializable
data class TwitchValidateDto(
    @SerialName("client_id") val clientId: String,
    @SerialName("user_id") val userId: String? = null,
)
