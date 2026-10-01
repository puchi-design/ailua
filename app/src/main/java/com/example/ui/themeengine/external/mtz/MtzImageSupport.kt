package com.example.ui.themeengine.external.mtz

internal object MtzImageSupport {
    /** Detect actual image bytes, including extensionless MIUI module entries. */
    fun extension(bytes: ByteArray): String? = when {
        bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4e.toByte() && bytes[3] == 0x47.toByte() &&
            bytes[4] == 0x0d.toByte() && bytes[5] == 0x0a.toByte() &&
            bytes[6] == 0x1a.toByte() && bytes[7] == 0x0a.toByte() -> "png"
        bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() &&
            bytes[2] == 0xff.toByte() -> "jpg"
        bytes.size >= 12 && bytes.copyOfRange(0, 4).contentEquals("RIFF".toByteArray()) &&
            bytes.copyOfRange(8, 12).contentEquals("WEBP".toByteArray()) -> "webp"
        else -> null
    }
}

