package com.flxrs.dankchat.server.badge

import java.security.MessageDigest
import java.util.HexFormat

enum class BadgeImageType(
    val contentType: String,
    val extension: String,
) {
    PNG("image/png", "png"),
    GIF("image/gif", "gif"),
    WEBP("image/webp", "webp"),
    ;

    companion object {
        fun detect(bytes: ByteArray): BadgeImageType? = when {
            bytes.startsWith(PNG_SIGNATURE) -> PNG
            bytes.startsWith(GIF87_SIGNATURE) || bytes.startsWith(GIF89_SIGNATURE) -> GIF
            bytes.startsWith(RIFF_SIGNATURE) && bytes.copyOfRange(WEBP_OFFSET, WEBP_OFFSET + WEBP_SIGNATURE.size).contentEquals(WEBP_SIGNATURE) -> WEBP
            else -> null
        }

        private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A)
        private val GIF87_SIGNATURE = "GIF87a".toByteArray()
        private val GIF89_SIGNATURE = "GIF89a".toByteArray()
        private val RIFF_SIGNATURE = "RIFF".toByteArray()
        private val WEBP_SIGNATURE = "WEBP".toByteArray()
        private const val WEBP_OFFSET = 8
    }
}

/** Content-addressed name, so a replaced image never reuses a cached URL. */
fun badgeImageName(
    badgeId: String,
    bytes: ByteArray,
    type: BadgeImageType,
): String {
    val hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).take(HASH_LENGTH)
    return "$badgeId-$hash.${type.extension}"
}

private const val HASH_LENGTH = 12

private fun ByteArray.startsWith(prefix: ByteArray): Boolean = size >= prefix.size && copyOfRange(0, prefix.size).contentEquals(prefix)
