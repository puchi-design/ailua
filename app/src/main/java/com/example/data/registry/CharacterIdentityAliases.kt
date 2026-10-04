package com.example.data.registry

/** Asset/share slugs only. These aliases must never replace a persisted character key. */
object CharacterIdentityAliases {
    val publicSlugByLegacyId = mapOf(
        "mira" to "suwanning",
        "yuna" to "xuchaoyan",
        "noa" to "songzhiwei",
    )

    fun publicSlug(characterId: String): String = publicSlugByLegacyId[characterId] ?: characterId
}
