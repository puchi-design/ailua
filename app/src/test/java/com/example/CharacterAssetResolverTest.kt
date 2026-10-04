package com.example

import com.example.data.characterassets.CharacterAssetResolver
import com.example.data.characterassets.CharacterAvatarSlot
import com.example.ui.designsystem.CharacterDisplayNames
import org.junit.Assert.*
import org.junit.Test

class CharacterAssetResolverTest {
    @Test fun femaleAssetSlugsDoNotBecomeNewStorageIdentities() {
        val names = mapOf("mira" to "suwanning", "yuna" to "xuchaoyan", "noa" to "songzhiwei")
        names.forEach { (stableId, slug) ->
            assertEquals("file:///android_asset/characters/$slug/avatar_main.webp",
                CharacterAssetResolver.officialUri(stableId, CharacterAvatarSlot.MAIN))
            assertNull(CharacterAssetResolver.officialUri(slug, CharacterAvatarSlot.MAIN))
        }
    }

    @Test fun allSixOfficialCharactersResolveDistinctMainAndAlternateAvatars() {
        val ids = listOf("hewenchuan", "zhoujianye", "peixubai", "mira", "yuna", "noa")
        val main = ids.map { CharacterAssetResolver.resolve(it, CharacterAvatarSlot.MAIN, unmodifiedOfficial = true)!! }
        val alt = ids.map { CharacterAssetResolver.resolve(it, CharacterAvatarSlot.ALT, unmodifiedOfficial = true)!! }
        assertEquals(12, (main + alt).map { it.uri }.toSet().size)
        assertTrue(main.all { it.uri.endsWith("avatar_main.webp") && it.builtIn })
        assertTrue(alt.all { it.uri.endsWith("avatar_alt.webp") && it.builtIn })
    }

    @Test fun officialMainDeclarationDoesNotMaskAlternateSlot() {
        val main = CharacterAssetResolver.officialUri("mira", CharacterAvatarSlot.MAIN)!!
        val result = CharacterAssetResolver.resolve("mira", CharacterAvatarSlot.ALT,
            avatarReference = main, visualAvatar = main, unmodifiedOfficial = true)!!
        assertEquals("file:///android_asset/characters/suwanning/avatar_alt.webp", result.uri)
    }

    @Test fun importedLocalAvatarOverridesBuiltInHeroForEveryOfficialId() {
        CharacterDisplayNames.officialIds.forEach { id ->
            val local = "content://saved-character-avatar/$id"
            val result = CharacterAssetResolver.resolve(id, CharacterAvatarSlot.ALT,
                avatarReference = local,
                visualPortrait = CharacterAssetResolver.officialUri(id, CharacterAvatarSlot.ALT).orEmpty(),
                unmodifiedOfficial = true)!!
            assertEquals(local, result.uri)
            assertFalse(result.builtIn)
        }
    }

    @Test fun editorImageOverrideWinsAndBlankPreviewDoesNotRestoreOfficialArt() {
        val official = CharacterAssetResolver.officialUri("mira", CharacterAvatarSlot.MAIN)!!
        val custom = CharacterAssetResolver.resolve("mira", CharacterAvatarSlot.MAIN,
            avatarReference = official, explicitOverride = "file:///local/new-avatar.webp", unmodifiedOfficial = true)!!
        assertEquals("file:///local/new-avatar.webp", custom.uri)
        listOf("", "mira", "suwanning").forEach { placeholder ->
            assertNull(CharacterAssetResolver.resolve("mira", CharacterAvatarSlot.MAIN,
                avatarReference = official, explicitOverride = placeholder, unmodifiedOfficial = true))
        }
    }

    @Test fun customSameIdWithNoAssetDoesNotInheritOfficialImage() {
        assertNull(CharacterAssetResolver.resolve("mira", CharacterAvatarSlot.MAIN,
            avatarReference = "mira", unmodifiedOfficial = false))
        assertNull(CharacterAssetResolver.resolve("hewenchuan", CharacterAvatarSlot.ALT,
            avatarReference = "", unmodifiedOfficial = false))
        assertNull(CharacterAssetResolver.resolve("custom_rose", CharacterAvatarSlot.MAIN, unmodifiedOfficial = true))
    }

    @Test fun existingAuthorSnapshotProjectsCurrentPublicNameWithoutEditingText() {
        assertEquals("苏晚宁", CharacterDisplayNames.project("mira", "小弥", "苏晚宁"))
        assertEquals("我自己编辑的名字", CharacterDisplayNames.project("mira", "小弥", "我自己编辑的名字"))
        assertEquals("旧作者", CharacterDisplayNames.project("custom_author", "旧作者", "后来改名"))
        assertEquals("小弥", CharacterDisplayNames.project("mira", "小弥", null))
    }

    @Test fun namesOnlyAuthorInferenceMatchesLabelsNotBodySubstrings() {
        assertEquals("mira", CharacterDisplayNames.knownLegacyAuthorId("小弥"))
        assertEquals("yuna", CharacterDisplayNames.knownLegacyAuthorId("悠奈"))
        assertEquals("noa", CharacterDisplayNames.knownLegacyAuthorId("Noa"))
        assertNull(CharacterDisplayNames.knownLegacyAuthorId("和小弥去喝茶"))
        assertNull(CharacterDisplayNames.knownLegacyAuthorId("我的朋友Mira"))
    }
}
