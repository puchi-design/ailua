package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.model.CharacterProfile
import com.example.data.registry.CharacterRegistry
import com.example.ui.components.HideDialogStatusBar
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.themeengine.LocalAiluaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSwitcherSheet(selectedId: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val theme = LocalAiluaTheme.current
    val romance = CharacterRegistry.getOfficialRomanceCharacters()
    val friendship = CharacterRegistry.getOfficialFriendshipCharacters()
    val officialIds = CharacterRegistry.OFFICIAL_ROSTER_IDS.toSet()
    val more = CharacterRegistry.getAllCharacters().filterNot { it.id in officialIds }
        .let { characters ->
            if (selectedId !in officialIds && characters.none { it.id == selectedId }) {
                characters + CharacterRegistry.getCharacter(selectedId)
            } else characters
        }
    var showMore by remember { mutableStateOf(selectedId !in officialIds) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = theme.surfaces.raised,
        modifier = Modifier.testTag("character_switcher_sheet"),
    ) {
        HideDialogStatusBar()
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = theme.layout.screenHorizontalPadding.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("切换当前角色", style = theme.text.title, color = theme.palette.onSurface)
            Text("这部小手机会跟着你选择的人更新。", style = theme.text.secondary,
                color = theme.palette.onSurfaceMuted)
            CharacterGroup("心动对象", romance, selectedId, onSelect)
            CharacterGroup("我的朋友", friendship, selectedId, onSelect)
            if (more.isNotEmpty()) {
                Text(
                    if (showMore) "更多角色  收起" else "更多角色  展开",
                    modifier = Modifier.fillMaxWidth().clickable { showMore = !showMore }
                        .padding(vertical = 10.dp).testTag("character_switcher_more"),
                    style = theme.text.section, color = theme.palette.onSurface,
                )
                if (showMore) more.forEach { character ->
                    CharacterOption(character, selectedId == character.id) { onSelect(character.id) }
                }
            }
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun CharacterGroup(
    title: String,
    characters: List<CharacterProfile>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    val theme = LocalAiluaTheme.current
    Text(title, style = theme.text.section, color = theme.palette.onSurface,
        modifier = Modifier.padding(top = 8.dp))
    characters.forEach { character ->
        CharacterOption(character, selectedId == character.id) { onSelect(character.id) }
    }
}

@Composable
private fun CharacterOption(character: CharacterProfile, isSelected: Boolean, onClick: () -> Unit) {
    val theme = LocalAiluaTheme.current
    val age = CharacterRuntimeResolver.resolve(character.id).identity.age
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(theme.shapes.medium.dp))
            .background(if (isSelected) theme.palette.accent.copy(alpha = 0.14f) else theme.surfaces.inset)
            .clickable(onClick = onClick)
            .semantics { selected = isSelected }
            .testTag("character_switcher_option_${character.id}")
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CharacterPortrait(character.id, PortraitVariant.AVATAR, Modifier.size(48.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(listOfNotNull(character.name, age?.let { "$it 岁" }).joinToString(" · "),
                style = theme.text.body, color = theme.palette.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(character.currentActivity.ifBlank { character.title }, style = theme.text.secondary,
                color = theme.palette.onSurfaceMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isSelected) Icon(Icons.Default.Check, contentDescription = "当前角色",
            modifier = Modifier.size(20.dp), tint = theme.palette.accent)
    }
}
