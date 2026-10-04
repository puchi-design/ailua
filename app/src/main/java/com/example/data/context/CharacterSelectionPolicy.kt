package com.example.data.context

/** New installs start on the official route; saved identities are never remapped. */
object CharacterSelectionPolicy {
    const val DEFAULT_CHARACTER_ID = "hewenchuan"
    const val LEGACY_CHARACTER_ID = "mira"

    fun resolve(savedId: String?, hasLegacyData: Boolean): String =
        savedId?.takeIf { it.isNotBlank() }
            ?: if (hasLegacyData) LEGACY_CHARACTER_ID else DEFAULT_CHARACTER_ID
}
