package com.flxrs.dankchat.server.twitch

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient

/** Twitch user lookups through Helix, which accepts up to 100 logins and ids per request. */
@ApplicationScoped
class TwitchUsers(
    @param:RestClient private val helix: HelixApi,
) {
    fun byLogin(login: String): HelixUserDto? = helix.users(logins = listOf(login), ids = emptyList()).data.firstOrNull()

    /** Resolves admin input: a login, optionally with @, or a numeric user id. A matching login wins over an id. */
    fun byLoginOrId(input: String): HelixUserDto? {
        val query = input.trim().removePrefix("@").lowercase()
        val ids =
            when {
                query.isNotEmpty() && query.all { it.isDigit() } -> listOf(query)
                else -> emptyList()
            }
        val users = helix.users(logins = listOf(query), ids = ids).data
        return users.firstOrNull { it.login == query } ?: users.firstOrNull()
    }

    fun byIds(ids: Collection<String>): List<HelixUserDto> = ids.distinct().chunked(MAX_PER_REQUEST).flatMap { chunk ->
        helix.users(logins = emptyList(), ids = chunk).data
    }

    companion object {
        private const val MAX_PER_REQUEST = 100
    }
}
