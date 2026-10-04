package com.example.data.characterassets

import com.example.data.registry.CharacterIdentityAliases

enum class CharacterAvatarSlot(val fileName: String, val tag: String) {
    MAIN("avatar_main.webp", "main"),
    ALT("avatar_alt.webp", "alt"),
}

data class ResolvedCharacterAvatar(val uri: String, val slot: CharacterAvatarSlot, val builtIn: Boolean)

/** Image references only: persisted character IDs and user cards are never rewritten. */
object CharacterAssetResolver {
    private val romanceIds = setOf("hewenchuan", "zhoujianye", "peixubai")

    fun officialUri(characterId: String, slot: CharacterAvatarSlot): String? {
        val slug = CharacterIdentityAliases.publicSlugByLegacyId[characterId]
            ?: characterId.takeIf { it in romanceIds } ?: return null
        return "file:///android_asset/characters/$slug/${slot.fileName}"
    }

    fun resolve(
        characterId: String,
        slot: CharacterAvatarSlot,
        avatarReference: String = "",
        visualAvatar: String = "",
        visualPortrait: String = "",
        explicitOverride: String? = null,
        unmodifiedOfficial: Boolean = false,
    ): ResolvedCharacterAvatar? {
        // An editor's explicit empty/placeholder reference means no image, not "restore official".
        if (explicitOverride != null) {
            return localUri(explicitOverride)?.let { ResolvedCharacterAvatar(it, slot, isBuiltIn(it)) }
        }
        val references = listOf(
            avatarReference,
            if (slot == CharacterAvatarSlot.ALT) visualPortrait else visualAvatar,
            visualAvatar,
        ).mapNotNull(::localUri)
        // Local imported photos take precedence over packaged asset declarations.
        references.firstOrNull { !isBuiltIn(it) }?.let {
            return ResolvedCharacterAvatar(it, slot, builtIn = false)
        }
        if (unmodifiedOfficial) {
            officialUri(characterId, slot)?.let { return ResolvedCharacterAvatar(it, slot, builtIn = true) }
        }
        return references.firstOrNull()?.let { ResolvedCharacterAvatar(it, slot, isBuiltIn(it)) }
    }

    private fun localUri(reference: String): String? = reference.trim().takeIf {
        it.startsWith("content://") || it.startsWith("file://") || it.startsWith("android.resource://")
    }

    private fun isBuiltIn(reference: String): Boolean =
        reference.startsWith("file:///android_asset/characters/")
}
