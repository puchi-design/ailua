package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CharacterProfile
import com.example.ui.designsystem.AiluaChip
import com.example.ui.designsystem.AiluaSurface
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.themeengine.LocalAiluaTheme
import com.example.ui.themeengine.WidgetBackgroundStyle

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

/** Keep the selected widget skin's surface and contrast, with the shared shape scale. */
@Composable
private fun Modifier.themedSnippetSurface(): Modifier {
    val runtime = LocalAiluaTheme.current
    return clip(RoundedCornerShape(runtime.shapes.large.dp))
        .background(runtime.widgets.backgroundColor.copy(alpha = runtime.widgets.surfaceAlpha))
}

@Composable
private fun snippetAccent(): Color {
    val runtime = LocalAiluaTheme.current
    return when (runtime.widgets.backgroundStyle) {
        WidgetBackgroundStyle.FLAT, WidgetBackgroundStyle.TRANSPARENT -> runtime.widgets.foregroundColor
        else -> runtime.palette.accent
    }
}

/** The user-added relationship widget retains its level, progress and days together. */
@Composable
fun BondProgressWidget(
    character: CharacterProfile,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val runtime = LocalAiluaTheme.current
    val foreground = runtime.widgets.foregroundColor
    val muted = foreground.copy(alpha = 0.72f)
    val accent = snippetAccent()
    Box(
        modifier.themedSnippetSurface().clickable(onClick = onClick)
            .padding(runtime.widgets.contentPaddingDp.dp).testTag("bond_progress_widget"),
    ) {
        Column {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("关系 Lv.${character.bondLevel}", style = runtime.text.secondary, color = foreground)
                Text("${character.daysTogether}天相伴", style = runtime.text.caption, color = muted)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { character.bondProgress / 100f },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(runtime.shapes.pill.dp)),
                color = accent,
                trackColor = foreground.copy(alpha = 0.14f),
            )
            Spacer(Modifier.height(6.dp))
            Text(character.bondName, style = runtime.text.caption, color = muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        modifier.themedSnippetSurface().clickable(onClick = onClick)
            .padding(runtime.widgets.contentPaddingDp.dp).testTag("memory_snippet_widget"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = runtime.text.secondary, color = foreground,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(snippet, style = runtime.text.secondary, color = foreground.copy(alpha = 0.78f),
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
