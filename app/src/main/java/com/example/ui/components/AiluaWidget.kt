package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CharacterProfile
import com.example.ui.designsystem.AiluaChip
import com.example.ui.designsystem.AiluaSurface
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.themeengine.LocalAiluaTheme

/** Companion presentation; workspace placement and sizing remain with the widget host. */
@Composable
fun LivingCharacterWidget(
    character: CharacterProfile,
    modifier: Modifier = Modifier,
    onOpenLiving: () -> Unit = {},
    onOpenChat: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
) {
    val theme = LocalAiluaTheme.current
    AiluaSurface(modifier.fillMaxWidth().clickable(onClick = onOpenLiving).testTag("living_character_widget")) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(character.currentActivity, style = theme.text.caption, color = theme.palette.onSurfaceMuted,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(character.location, style = theme.text.caption, color = theme.palette.onSurfaceMuted,
                    modifier = Modifier.weight(1f).clickable(onClick = onOpenProfile), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CharacterPortrait(character.id, PortraitVariant.HERO, Modifier.size(64.dp), onClick = onOpenProfile)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(character.name, style = theme.text.title, color = theme.palette.onSurface)
                    Text(character.mood, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                    Text("“${character.contextualQuote}”", style = theme.text.secondary, color = theme.palette.onSurface,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AiluaChip("发消息", onClick = onOpenChat,
                    modifier = Modifier.weight(1f).testTag("widget_chat_action_btn"))
                AiluaChip("生活", onClick = onOpenLiving,
                    modifier = Modifier.weight(1f).testTag("widget_living_action_btn"))
            }
        }
    }
}

/** A relationship glance; the widget host supplies the material and placement. */
@Composable
fun BondProgressWidget(
    character: CharacterProfile,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val runtime = LocalAiluaTheme.current
    val foreground = runtime.widgets.foregroundColor
    val muted = foreground.copy(alpha = 0.72f)
    Box(
        modifier.clickable(onClick = onClick).testTag("bond_progress_widget"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(character.bondName, style = runtime.text.section, color = foreground,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("与你相伴的第${character.daysTogether}天", style = runtime.text.caption, color = muted)
            character.memories.firstOrNull()?.snippet?.takeIf { it.isNotBlank() }?.let { snippet ->
                Text(snippet, style = runtime.text.secondary, color = foreground,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun MemorySnippetWidget(
    title: String,
    snippet: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val runtime = LocalAiluaTheme.current
    val foreground = runtime.widgets.foregroundColor
    Box(
        modifier.clickable(onClick = onClick).testTag("memory_snippet_widget"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = runtime.text.section, color = foreground,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(snippet, style = runtime.text.secondary, color = foreground.copy(alpha = 0.78f),
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
