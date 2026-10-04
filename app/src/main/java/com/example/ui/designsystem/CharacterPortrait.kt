package com.example.ui.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.ui.components.AiluaAvatar
import com.example.data.registry.CharacterRegistry
import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.characterassets.CharacterAssetResolver
import com.example.data.characterassets.CharacterAvatarSlot
import com.example.ui.themeengine.LocalAiluaTheme

enum class PortraitVariant { AVATAR, HERO, PROFILE }

/** Circular avatar assets, not full-body artwork. Local user photos take precedence. */
@Composable
fun CharacterPortrait(
    characterId: String,
    variant: PortraitVariant = PortraitVariant.HERO,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    avatarReferenceOverride: String? = null,
    characterNameOverride: String? = null,
) {
    val card = if (avatarReferenceOverride == null || characterNameOverride == null) {
        val cards by CharacterRegistry.allCards.collectAsState()
        cards[characterId]
    } else null
    val unmodifiedOfficial = avatarReferenceOverride == null && CharacterRegistry.isUnmodifiedBuiltIn(characterId)
    val visual = card?.data?.let(CharacterRuntimeResolver::resolve)?.visual
    val slot = if (variant == PortraitVariant.HERO) CharacterAvatarSlot.ALT else CharacterAvatarSlot.MAIN
    val asset = CharacterAssetResolver.resolve(
        characterId = characterId,
        slot = slot,
        avatarReference = card?.data?.avatarReference.orEmpty(),
        visualAvatar = visual?.avatar.orEmpty(),
        visualPortrait = visual?.portrait.orEmpty(),
        explicitOverride = avatarReferenceOverride,
        unmodifiedOfficial = unmodifiedOfficial,
    )
    val displayName = characterNameOverride ?: card?.data?.name ?: characterId
    val theme = LocalAiluaTheme.current
    val defaultSize = when (variant) {
        PortraitVariant.AVATAR -> 40.dp
        PortraitVariant.HERO -> 160.dp
        PortraitVariant.PROFILE -> 192.dp
    }
    BoxWithConstraints(
        modifier.size(defaultSize)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag("character_portrait_${characterId}_${slot.tag}"),
        contentAlignment = Alignment.Center,
    ) {
        // Caller constraints win: an explicit 64dp avatar must remain 64dp.
        val size = minOf(maxWidth, maxHeight)
        val fallback: @Composable () -> Unit = {
            if ((unmodifiedOfficial && characterId in setOf("mira", "yuna", "noa")) || characterId == "group_tea") {
                AiluaAvatar(avatarId = characterId, size = size, showHalo = false)
            } else {
                Box(Modifier.size(size).clip(CircleShape).background(theme.surfaces.inset), contentAlignment = Alignment.Center) {
                    Text(displayName.take(1),
                        style = if (variant == PortraitVariant.AVATAR) theme.text.section else theme.text.display,
                        color = theme.palette.onSurface)
                }
            }
        }
        if (asset != null) {
            SubcomposeAsyncImage(model = asset.uri, contentDescription = "$displayName 的头像",
                modifier = Modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop,
                loading = { fallback() }, error = { fallback() },
                success = {
                    SubcomposeAsyncImageContent(modifier = Modifier.testTag("character_portrait_loaded_${characterId}_${slot.tag}"))
                })
        } else fallback()
    }
}
