package com.flxrs.dankchat.server.badge

/**
 * Fired inside a transaction that changed badges, holdings or user preferences. The snapshot rebuilds once it commits.
 */
data class BadgesChanged(
    val reason: String,
)
