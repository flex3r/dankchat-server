package com.flxrs.dankchat.server.badge

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "dankchat.badges")
interface BadgesConfig {
    fun imageBaseUrl(): String
}

fun BadgesConfig.imageUrl(image: String): String = "${imageBaseUrl().trimEnd('/')}/$image"
