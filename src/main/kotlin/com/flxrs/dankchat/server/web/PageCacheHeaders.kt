package com.flxrs.dankchat.server.web

import io.vertx.core.http.HttpHeaders
import io.vertx.ext.web.Router
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes

/** Pages revalidate on every load, so a deploy never leaves browsers on assets that no longer exist. */
@ApplicationScoped
class PageCacheHeaders {
    fun register(
        @Observes router: Router,
    ) {
        router.route().order(Int.MIN_VALUE).handler { context ->
            context.addHeadersEndHandler {
                val response = context.response()
                if (response.headers()[HttpHeaders.CONTENT_TYPE]?.startsWith("text/html") == true) {
                    response.putHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
                }
            }
            context.next()
        }
    }
}
