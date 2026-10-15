package com.flxrs.dankchat.server.admin

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "dankchat.admin")
interface AdminConfig {
    fun twitchIds(): Set<String>
}
