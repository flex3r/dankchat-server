package com.flxrs.dankchat.server.emote

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "ivr")
@Path("/v2/twitch/emotes")
interface IvrApi {
    /** Expects comma separated set ids; unknown sets are left out of the result. */
    @GET
    @Path("/sets")
    fun emoteSets(
        @QueryParam("set_id") setIds: String,
    ): List<IvrEmoteSetDto>
}

@Serializable
data class IvrEmoteSetDto(
    @SerialName("setID") val id: String,
    @SerialName("channelLogin") val channelLogin: String? = null,
    @SerialName("channelID") val channelId: String? = null,
    val tier: String? = null,
    @SerialName("emoteList") val emotes: List<IvrEmoteDto> = emptyList(),
)

@Serializable
data class IvrEmoteDto(
    val code: String,
    val id: String,
    val type: String? = null,
    val assetType: String? = null,
)
