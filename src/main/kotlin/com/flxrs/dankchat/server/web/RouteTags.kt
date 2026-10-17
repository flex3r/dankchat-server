package com.flxrs.dankchat.server.web

import io.micrometer.core.instrument.Tags
import io.quarkus.micrometer.runtime.HttpServerMetricsTagsContributor
import jakarta.inject.Singleton

private val KNOWN_PATHS = setOf("/account", "/admin", "/admin/badge", "/privacy", "/delete-account", "/login", "/login/callback")

/**
 * Names every request metric after its route, so redirects and 304s keep their endpoint and unknown paths can't create new series.
 * Quarkus only does that itself for routes with path parameters.
 */
@Singleton
class RouteTags : HttpServerMetricsTagsContributor {
    override fun contribute(context: HttpServerMetricsTagsContributor.Context): Tags {
        val route = routeOf(
            path = context.request().path().orEmpty(),
            template = context.requestContextLocalData("UrlPathTemplate"),
            status = context.response().map { it.statusCode() }.orElse(0),
        )
        return Tags.of("uri", route)
    }
}

fun routeOf(
    path: String,
    template: String?,
    status: Int,
): String {
    val normalized = path.trimEnd('/').ifEmpty { "/" }
    return when {
        template != null -> template
        normalized == "/" -> "root"
        normalized in KNOWN_PATHS -> normalized
        normalized.startsWith("/_app/") -> "/_app/{asset}"
        status == 404 -> "NOT_FOUND"
        else -> "/{other}"
    }
}
