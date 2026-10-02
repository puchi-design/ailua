package com.example.ui.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.AiluaAvatar
import com.example.data.registry.CharacterRegistry

enum class PortraitVariant { AVATAR, HERO, PROFILE }

/** The UI's asset entry point. Until portraits exist, render the existing avatar unchanged. */
@Composable
fun CharacterPortrait(
    characterId: String,
    variant: PortraitVariant = PortraitVariant.HERO,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
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
        AiluaAvatar(
            avatarId = CharacterRegistry.getCharacter(characterId).avatarId,
            size = minOf(maxWidth, maxHeight, defaultSize),
            showHalo = false,
        )
    }
}
