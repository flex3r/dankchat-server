package com.flxrs.dankchat.server.badge

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BadgeSelectionTest {
    private val priorities = mapOf("developer" to 90, "contributor" to 70, "supporter" to 10)

    @Test
    fun `highest priority badge wins without a preference`() {
        val holdings = listOf(BadgeHolding("1", "supporter"), BadgeHolding("1", "contributor"))

        assertEquals(mapOf("1" to "contributor"), selectBadges(priorities, holdings, preferences = emptyMap()))
    }

    @Test
    fun `selected badge wins when the user owns it`() {
        val holdings = listOf(BadgeHolding("1", "supporter"), BadgeHolding("1", "contributor"))
        val preferences = mapOf("1" to BadgePreference(selectedBadgeId = "supporter", hideBadges = false))

        assertEquals(mapOf("1" to "supporter"), selectBadges(priorities, holdings, preferences))
    }

    @Test
    fun `selected badge the user does not own falls back to priority`() {
        val holdings = listOf(BadgeHolding("1", "supporter"))
        val preferences = mapOf("1" to BadgePreference(selectedBadgeId = "developer", hideBadges = false))

        assertEquals(mapOf("1" to "supporter"), selectBadges(priorities, holdings, preferences))
    }

    @Test
    fun `hidden users are left out`() {
        val holdings = listOf(BadgeHolding("1", "supporter"), BadgeHolding("2", "supporter"))
        val preferences = mapOf("1" to BadgePreference(selectedBadgeId = "supporter", hideBadges = true))

        assertEquals(mapOf("2" to "supporter"), selectBadges(priorities, holdings, preferences))
    }

    @Test
    fun `equal priorities resolve deterministically`() {
        val holdings = listOf(BadgeHolding("1", "b"), BadgeHolding("1", "a"))
        val reversed = holdings.reversed()

        assertEquals(selectBadges(emptyMap(), holdings, emptyMap()), selectBadges(emptyMap(), reversed, emptyMap()))
    }
}
