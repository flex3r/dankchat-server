package com.flxrs.dankchat.server.twitch

import io.quarkus.oidc.client.filter.OidcClientFilter
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "twitch-helix")
@OidcClientFilter("twitch")
@ClientHeaderParam(name = "Client-Id", value = ["\${quarkus.oidc-client.twitch.client-id}"])
@Path("/helix")
interface HelixApi {
    @GET
    @Path("/users")
    fun users(
        @QueryParam("login") logins: List<String>,
        @QueryParam("id") ids: List<String>,
    ): HelixUsersDto

    @GET
    @Path("/chat/color")
    fun chatColors(
        @QueryParam("user_id") userIds: List<String>,
    ): HelixChatColorsDto
}

@Serializable
data class HelixUsersDto(
    val data: List<HelixUserDto>,
)

@Serializable
data class HelixUserDto(
    val id: String,
    val login: String,
    @SerialName("display_name") val displayName: String,
)

@Serializable
data class HelixChatColorsDto(
    val data: List<HelixChatColorDto>,
)

@Serializable
data class HelixChatColorDto(
    @SerialName("user_id") val userId: String,
    val color: String,
)
