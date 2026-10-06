package com.example.ui.home.special

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.context.CharacterContext
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.model.isUserActivity
import com.example.data.model.sortedChronologically
import com.example.data.projection.projectPresence
import com.example.data.registry.CharacterRegistry
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.home.widget.ThemeWidgetFrame
import com.example.ui.themeengine.LocalAiluaTheme

/** A quiet view into the current companion's day; the workspace owns navigation and layout. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun LifeBentoPage(
    onNavigateToGroupChat: () -> Unit,
    onNavigateToCheckPhone: () -> Unit,
    onNavigateToDiary: () -> Unit,
    onNavigateToRelations: () -> Unit,
    onNavigateToLiving: () -> Unit,
    onAppClick: (String) -> Unit
) {
    val theme = LocalAiluaTheme.current
    val wallpaperForeground = theme.icons.labelColor
    val wallpaperSecondary = wallpaperForeground.copy(alpha = 0.8f)
    val allLifeEvents by WorldStateRepository.events.collectAsStateWithLifecycle()
    val worldClock by WorldHeartbeatEngine.worldClock.collectAsStateWithLifecycle()
    val selectedCharacterId by CharacterContext.selectedId.collectAsStateWithLifecycle()
    val cards by CharacterRegistry.allCards.collectAsStateWithLifecycle()
    val character = remember(selectedCharacterId, cards) { CharacterRegistry.getCharacter(selectedCharacterId) }
    val presence = remember(character, allLifeEvents) { projectPresence(character, allLifeEvents) }
    val characterEvents = remember(character.id, allLifeEvents) {
        allLifeEvents.filter { it.characterId == character.id && !it.isUserActivity() }.sortedChronologically()
    }
    val todayEvents = remember(allLifeEvents, worldClock.dateLabel) {
        allLifeEvents.filter { !it.isUserActivity() && it.worldDateLabel == worldClock.dateLabel }
            .sortedChronologically().asReversed().take(6)
    }
    val latestLine = characterEvents.lastOrNull()?.description ?: character.contextualQuote

    // This is a launcher page: the wallpaper remains the page's canvas.
    BoxWithConstraints(Modifier.fillMaxSize().testTag("life_bento_page")) {
        val heroHeight = (maxHeight * 0.43f).coerceIn(236.dp, 300.dp)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = theme.layout.screenHorizontalPadding.dp)
                .padding(top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("生活", style = theme.text.title, color = wallpaperForeground,
                    modifier = Modifier.weight(1f))
                Text("${worldClock.dateLabel} · ${worldClock.timeFormatted}",
                    style = theme.text.caption, color = wallpaperSecondary)
            }

            ThemeWidgetFrame(selected = false,
                modifier = Modifier.fillMaxWidth().height(heroHeight).clickable(onClick = onNavigateToLiving)) {
                Column(Modifier.fillMaxSize().padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(character.name, style = theme.text.title, color = theme.widgets.foregroundColor)
                            Text(presence.currentLocation, style = theme.text.caption,
                                color = theme.widgets.foregroundColor.copy(alpha = 0.78f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        CharacterPortrait(
                            characterId = character.id,
                            variant = PortraitVariant.HERO,
                            modifier = Modifier.size(108.dp),
                            onClick = onNavigateToLiving,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(latestLine, style = theme.text.section, color = theme.widgets.foregroundColor,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(presence.currentActivity, style = theme.text.caption,
                            color = theme.widgets.foregroundColor.copy(alpha = 0.78f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                LifeEntry("消息", Icons.Default.ChatBubbleOutline, { onAppClick("messages") },
                    Modifier.weight(1f).testTag("life_bento_messages"))
                LifeEntry("生活", Icons.Default.WbSunny, onNavigateToLiving,
                    Modifier.weight(1f).testTag("life_bento_living"))
                LifeEntry("相册", Icons.Default.PhotoLibrary, { onAppClick("gallery") },
                    Modifier.weight(1f).testTag("life_bento_gallery"))
            }

            Column(Modifier.fillMaxWidth()) {
                Text("今日动态", style = theme.text.section, color = wallpaperForeground)
                if (todayEvents.isEmpty()) {
                    Text("今天还没有新的动态", style = theme.text.secondary,
                        color = wallpaperSecondary, modifier = Modifier.padding(vertical = 18.dp))
                }
                todayEvents.forEachIndexed { index, event ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(event.time, style = theme.text.caption, color = wallpaperSecondary,
                            modifier = Modifier.width(42.dp).padding(top = 3.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("${CharacterRegistry.getCharacter(event.characterId).name} · ${com.example.ui.designsystem.CharacterDisplayNames.projectLegacyMentions(event.title)}",
                                style = theme.text.body, color = wallpaperForeground,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (event.description.isNotBlank() && event.description != event.title) {
                                Text(com.example.ui.designsystem.CharacterDisplayNames.projectLegacyMentions(event.description),
                                    style = theme.text.secondary, color = wallpaperSecondary,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (index != todayEvents.lastIndex) {
                        Box(Modifier.fillMaxWidth().padding(start = 58.dp).height(0.5.dp)
                            .background(wallpaperForeground.copy(alpha = 0.12f)))
                    }
                }
            }
        }
    }
}

@Composable
private fun LifeEntry(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalAiluaTheme.current
    Column(modifier.clip(RoundedCornerShape(theme.shapes.small.dp)).clickable(onClick = onClick)
        .padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = theme.icons.labelColor, modifier = Modifier.size(22.dp))
        Text(label, style = theme.text.secondary, color = theme.icons.labelColor)
    }
}
