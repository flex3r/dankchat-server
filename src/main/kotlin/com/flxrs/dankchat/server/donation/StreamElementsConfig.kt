package com.flxrs.dankchat.server.donation

import io.smallrye.config.ConfigMapping
import java.util.Optional

@ConfigMapping(prefix = "dankchat.streamelements")
interface StreamElementsConfig {
    fun channelId(): String

    fun token(): Optional<String>
}
