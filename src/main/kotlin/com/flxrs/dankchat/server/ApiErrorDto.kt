package com.flxrs.dankchat.server

import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorDto(
    val message: String,
)

fun apiError(
    status: Response.Status,
    message: String,
): Nothing = throw WebApplicationException(
    Response
        .status(status)
        .type(MediaType.APPLICATION_JSON)
        .entity(ApiErrorDto(message))
        .build(),
)
