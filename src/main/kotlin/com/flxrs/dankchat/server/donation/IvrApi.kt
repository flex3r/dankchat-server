package com.flxrs.dankchat.server.donation

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import kotlinx.serialization.Serializable
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "ivr")
@Path("/v2/twitch")
interface IvrApi {
    @GET
    @Path("/user")
    fun users(
        @QueryParam("login") login: String,
    ): List<IvrUserDto>
}

@Serializable
data class IvrUserDto(
    val id: String,
)
