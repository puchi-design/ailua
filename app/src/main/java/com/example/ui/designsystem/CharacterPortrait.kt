package com.example.ui.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.ui.components.AiluaAvatar
import com.example.data.registry.CharacterRegistry
import com.example.data.codec.AiluaCharacterExtensionCodec
import com.example.ui.themeengine.LocalAiluaTheme

enum class PortraitVariant { AVATAR, HERO, PROFILE }

/** Local imported photos or an explicit placeholder; asset pack references stay dormant until ART. */
@Composable
fun CharacterPortrait(
    characterId: String,
    variant: PortraitVariant = PortraitVariant.HERO,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    avatarReferenceOverride: String? = null,
) {
    val cards by CharacterRegistry.allCards.collectAsState()
    val card = cards[characterId]
    val visual = card?.data?.let(AiluaCharacterExtensionCodec::readOrNull)?.visual
    val standardReference = card?.data?.avatarReference.orEmpty()
    val visualReference = if (variant == PortraitVariant.AVATAR) visual?.avatar.orEmpty() else visual?.portrait.orEmpty()
    val reference = avatarReferenceOverride ?: standardReference.takeIf { it.contains("://") }
        ?: visualReference.takeIf { it.contains("://") } ?: standardReference
    val localImage = reference.takeIf { it.startsWith("content://") || it.startsWith("file://") || it.startsWith("android.resource://") }
    val theme = LocalAiluaTheme.current
    val defaultSize = when (variant) {
        PortraitVariant.AVATAR -> 40.dp
        PortraitVariant.HERO -> 160.dp
        PortraitVariant.PROFILE -> 192.dp
    }
    BoxWithConstraints(
        modifier.defaultMinSize(minWidth = defaultSize, minHeight = defaultSize)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val size = minOf(maxWidth, maxHeight, defaultSize)
        val fallback: @Composable () -> Unit = {
            val legacyId = reference.ifBlank { characterId }
            if (characterId in setOf("mira", "yuna", "group_tea") && legacyId in setOf("mira", "yuna", "group_tea")) {
                AiluaAvatar(avatarId = legacyId, size = size, showHalo = false)
            } else {
                Box(Modifier.size(size).clip(CircleShape).background(theme.surfaces.inset), contentAlignment = Alignment.Center) {
                    Text((card?.data?.name ?: characterId).take(1),
                        style = if (variant == PortraitVariant.AVATAR) theme.text.section else theme.text.display,
                        color = theme.palette.onSurface)
                }
            }
        }
        if (localImage != null) {
            SubcomposeAsyncImage(model = localImage, contentDescription = card?.data?.name ?: "角色照片",
                modifier = Modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop,
                loading = { fallback() }, error = { fallback() })
        } else fallback()
    }
}
