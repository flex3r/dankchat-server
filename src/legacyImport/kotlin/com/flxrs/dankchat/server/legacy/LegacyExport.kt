package com.flxrs.dankchat.server.legacy

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.sql.DriverManager
import java.util.HexFormat
import kotlin.system.exitProcess

@Serializable
data class LegacyBadge(
    val type: String,
    val url: String,
    val users: List<String>,
)

data class LegacyDonor(
    val seChannelId: String,
    val twitchId: String,
    val name: String,
)

data class LegacyData(
    val donors: List<LegacyDonor>,
    val contributors: List<String>,
    val optouts: List<String>,
    val danks: List<LegacyBadge>,
)

class LegacyImage(
    val name: String,
    val contentType: String,
    val bytes: ByteArray,
)

private val imageContentTypes = mapOf("png" to "image/png", "gif" to "image/gif", "webp" to "image/webp")

private const val DEVELOPER_TWITCH_ID = "73697410"
private const val CUSTOM_BADGE_PRIORITY = 100
private const val LEGACY_IMAGE_BASE_URL = "https://flxrs.com/dankchat/badges/"

private val systemBadgeIdsByTitle =
    mapOf(
        "DankChat Developer" to "developer",
        "DankChat Contributor" to "contributor",
        "DankChat Supporter" to "supporter",
    )

fun main(args: Array<String>) {
    val dataDir = args.getOrNull(0)?.takeIf { it.isNotBlank() }?.let(::File)
    val output = args.getOrNull(1)?.takeIf { it.isNotBlank() }?.let(::File)
    val imagesDir = args.getOrNull(2)?.takeIf { it.isNotBlank() }?.let(::File)
    if (dataDir == null || output == null) {
        System.err.println(
            "Usage: ./gradlew legacyExport -PlegacyDir=<copy of /opt/dankchat-api> -PlegacyOut=<file.sql> [-PlegacyImages=<copy of the badge image directory>]",
        )
        exitProcess(1)
    }

    val data = readLegacyData(dataDir)
    val images = imagesDir?.let(::readLegacyImages).orEmpty()
    output.writeText(toSql(data, images))
    println("Wrote ${data.donors.size} donors, ${data.contributors.size} contributors, ${data.danks.size} custom badge lists, ${images.size} images to $output")
}

fun readLegacyData(dataDir: File): LegacyData {
    val donors =
        DriverManager.getConnection("jdbc:h2:file:${dataDir.resolve("db").absolutePath};IFEXISTS=TRUE").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("""SELECT "seId", "twitchId", NAME FROM USERS ORDER BY ID""").use { result ->
                    buildList {
                        while (result.next()) {
                            add(LegacyDonor(seChannelId = result.getString(1), twitchId = result.getString(2).trim(), name = result.getString(3)))
                        }
                    }
                }
            }
        }

    return LegacyData(
        donors = donors.filter { it.twitchId.isNotBlank() },
        contributors = readIds(dataDir.resolve("contributors.txt")),
        optouts = readIds(dataDir.resolve("optouts.txt")),
        danks =
            dataDir
                .resolve("danks.json")
                .takeIf { it.exists() }
                ?.let { Json.decodeFromString<List<LegacyBadge>>(it.readText()) }
                .orEmpty(),
    )
}

private fun readIds(file: File): List<String> = when {
    file.exists() -> file.readLines().map { it.trim() }.filter { it.isNotEmpty() }
    else -> emptyList()
}

fun readLegacyImages(imagesDir: File): List<LegacyImage> = imagesDir
    .listFiles()
    .orEmpty()
    .filter { it.isFile }
    .sortedBy { it.name }
    .map { file ->
        val contentType = requireNotNull(imageContentTypes[file.extension.lowercase()]) { "Unsupported badge image ${file.name}" }
        LegacyImage(name = file.name, contentType = contentType, bytes = file.readBytes())
    }

fun toSql(
    data: LegacyData,
    images: List<LegacyImage> = emptyList(),
): String {
    val customBadges = data.danks.filter { it.type !in systemBadgeIdsByTitle }
    val grants =
        buildList {
            add(DEVELOPER_TWITCH_ID to "developer")
            data.contributors.forEach { add(it to "contributor") }
            data.danks.forEach { badge ->
                val badgeId = systemBadgeIdsByTitle[badge.type] ?: badgeSlug(badge.type)
                badge.users.filter { it.isNotBlank() }.forEach { add(it to badgeId) }
            }
        }.distinct()

    val donorLogins = data.donors.associate { it.twitchId to it.name }
    val userIds = (data.donors.map { it.twitchId } + grants.map { it.first } + data.optouts).distinct()

    return buildString {
        appendLine("begin;")
        images.forEach { image ->
            appendLine(
                "insert into badge_images (name, content_type, data) values (${sql(
                    image.name,
                )}, ${sql(image.contentType)}, decode('${HexFormat.of().formatHex(image.bytes)}', 'hex')) on conflict (name) do nothing;",
            )
        }
        customBadges.forEach { badge ->
            appendLine(
                "insert into badges (id, title, image, priority) values (${sql(
                    badgeSlug(badge.type),
                )}, ${sql(badge.type)}, ${sql(imageFileName(badge.url))}, $CUSTOM_BADGE_PRIORITY) on conflict (id) do nothing;",
            )
        }
        userIds.forEach { twitchId ->
            appendLine("insert into users (twitch_id, login) values (${sql(twitchId)}, ${sqlOrNull(donorLogins[twitchId])}) on conflict (twitch_id) do nothing;")
        }
        data.donors.forEach { donor ->
            appendLine(
                "insert into donations (se_channel_id, twitch_id, name) values (${sql(donor.seChannelId)}, ${sql(donor.twitchId)}, ${sql(donor.name)}) on conflict (se_channel_id) do nothing;",
            )
        }
        data.donors.map { it.twitchId }.distinct().forEach { twitchId ->
            appendLine(
                "insert into user_badges (twitch_id, badge_id, source) values (${sql(twitchId)}, 'supporter', 'DONATION') on conflict (twitch_id, badge_id) do nothing;",
            )
        }
        grants.forEach { (twitchId, badgeId) ->
            appendLine("insert into user_badges (twitch_id, badge_id, source) values (${sql(twitchId)}, ${sql(badgeId)}, 'GRANT') on conflict (twitch_id, badge_id) do nothing;")
        }
        data.optouts.distinct().forEach { twitchId ->
            appendLine("update users set hide_badges = true where twitch_id = ${sql(twitchId)};")
        }
        appendLine("commit;")
    }
}

private fun imageFileName(url: String): String {
    require(url.startsWith(LEGACY_IMAGE_BASE_URL)) { "Badge image $url is not hosted under $LEGACY_IMAGE_BASE_URL" }
    return url.removePrefix(LEGACY_IMAGE_BASE_URL)
}

fun badgeSlug(title: String): String = title
    .lowercase()
    .replace(Regex("[^a-z0-9]+"), "-")
    .trim('-')

private fun sql(value: String): String = "'${value.replace("'", "''")}'"

private fun sqlOrNull(value: String?): String = value?.let(::sql) ?: "null"
