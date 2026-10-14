package com.flxrs.dankchat.server.donation

import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.HttpHeaders
import kotlinx.serialization.Serializable
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "streamelements")
@Path("/kappa/v2")
interface StreamElementsApi {
    @GET
    @Path("/tips/{channelId}")
    fun tips(
        @PathParam("channelId") channelId: String,
        @QueryParam("limit") limit: Int,
        @QueryParam("offset") offset: Int,
        @HeaderParam(HttpHeaders.AUTHORIZATION) authorization: String,
    ): TipsDto

    @GET
    @Path("/channels/{channelId}")
    fun channel(
        @PathParam("channelId") channelId: String,
        @HeaderParam(HttpHeaders.AUTHORIZATION) authorization: String,
    ): StreamElementsChannelDto
}

@Serializable
data class TipsDto(
    val docs: List<TipDocDto> = emptyList(),
    val totalDocs: Int? = null,
    val total: Int? = null,
)

@Serializable
data class TipDocDto(
    val donation: TipDonationDto,
)

@Serializable
data class TipDonationDto(
    val user: TipUserDto,
)

@Serializable
data class TipUserDto(
    val username: String,
    val channel: String? = null,
)

@Serializable
data class StreamElementsChannelDto(
    val provider: String,
    val providerId: String,
)
