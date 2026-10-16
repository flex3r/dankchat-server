package com.flxrs.dankchat.server.auth

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class TestClock(
    private var now: Instant = Instant.parse("2026-10-16T12:00:00Z"),
) : Clock() {
    fun advance(duration: Duration) {
        now = now.plus(duration)
    }

    override fun instant(): Instant = now

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
}
