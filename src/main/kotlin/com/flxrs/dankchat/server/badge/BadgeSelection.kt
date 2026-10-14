package com.flxrs.dankchat.server.badge

import java.util.TreeMap

data class BadgeHolding(
    val twitchId: String,
    val badgeId: String,
)

data class BadgePreference(
    val selectedBadgeId: String?,
    val hideBadges: Boolean,
)

/**
 * Picks the one badge shown per user: nothing if hidden, the selected badge if still owned, otherwise the highest priority one.
 */
fun selectBadges(
    priorities: Map<String, Int>,
    holdings: List<BadgeHolding>,
    preferences: Map<String, BadgePreference>,
): Map<String, String> {
    val byPriority = compareBy<String> { priorities[it] ?: 0 }.thenByDescending { it }
    return holdings
        .groupBy(keySelector = { it.twitchId }, valueTransform = { it.badgeId })
        .mapNotNull { (twitchId, owned) ->
            val preference = preferences[twitchId]
            val selected = preference?.selectedBadgeId
            when {
                preference?.hideBadges == true -> null
                selected != null && selected in owned -> twitchId to selected
                else -> twitchId to owned.maxWith(byPriority)
            }
        }.toMap(TreeMap())
}
