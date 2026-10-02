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
import com.example.ui.designsystem.AiluaMediaFrame
import com.example.ui.designsystem.AiluaSectionHeader
import com.example.ui.designsystem.AiluaSurface
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
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

    BoxWithConstraints(Modifier.fillMaxSize().background(theme.surfaces.screen).testTag("life_bento_page")) {
        val heroHeight = (maxHeight * 0.43f).coerceIn(236.dp, 300.dp)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = theme.layout.screenHorizontalPadding.dp)
                .padding(top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("生活", style = theme.text.title, color = theme.palette.onSurface,
                    modifier = Modifier.weight(1f))
                Text("${worldClock.dateLabel} · ${worldClock.timeFormatted}",
                    style = theme.text.caption, color = theme.palette.onSurfaceMuted)
            }

            AiluaMediaFrame(modifier = Modifier.fillMaxWidth().height(heroHeight)) {
                Column(Modifier.fillMaxSize().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CharacterPortrait(
                        characterId = character.id,
                        variant = PortraitVariant.HERO,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(character.name, style = theme.text.title, color = theme.palette.onSurface)
                        Column(Modifier.weight(1f)) {
                            Text(presence.currentActivity, style = theme.text.secondary,
                                color = theme.palette.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(presence.currentLocation, style = theme.text.caption,
                                color = theme.palette.onSurfaceMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Text(latestLine, style = theme.text.secondary, color = theme.palette.onSurfaceMuted,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }

            AiluaSurface(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LifeEntry("消息", Icons.Default.ChatBubbleOutline, { onAppClick("messages") },
                        Modifier.weight(1f).testTag("life_bento_messages"))
                    LifeEntry("生活", Icons.Default.WbSunny, onNavigateToLiving,
                        Modifier.weight(1f).testTag("life_bento_living"))
                    LifeEntry("相册", Icons.Default.PhotoLibrary, { onAppClick("gallery") },
                        Modifier.weight(1f).testTag("life_bento_gallery"))
                }
            }

            Column(Modifier.fillMaxWidth()) {
                AiluaSectionHeader(title = "今日动态")
                if (todayEvents.isEmpty()) {
                    Text("今天还没有新的动态", style = theme.text.secondary,
                        color = theme.palette.onSurfaceMuted, modifier = Modifier.padding(vertical = 18.dp))
                }
                todayEvents.forEachIndexed { index, event ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(event.time, style = theme.text.caption, color = theme.palette.onSurfaceMuted,
                            modifier = Modifier.width(42.dp).padding(top = 3.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("${CharacterRegistry.getCharacter(event.characterId).name} · ${event.title}",
                                style = theme.text.body, color = theme.palette.onSurface,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (event.description.isNotBlank() && event.description != event.title) {
                                Text(event.description, style = theme.text.secondary, color = theme.palette.onSurfaceMuted,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (index != todayEvents.lastIndex) {
                        Box(Modifier.fillMaxWidth().padding(start = 58.dp).height(0.5.dp)
                            .background(theme.surfaces.divider))
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
        Icon(icon, contentDescription = null, tint = theme.palette.accent, modifier = Modifier.size(22.dp))
        Text(label, style = theme.text.secondary, color = theme.palette.onSurface)
    }
}
