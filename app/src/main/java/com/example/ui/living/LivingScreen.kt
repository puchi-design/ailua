package com.example.ui.living

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.mock.MockData
import com.example.data.model.CharacterProfile
import com.example.data.model.TimelineEvent
import com.example.data.model.WorldClock
import com.example.data.projection.projectLiving
import com.example.ui.components.WorldTimeDevSheet
import com.example.ui.designsystem.AiluaChip
import com.example.ui.designsystem.AiluaMediaFrame
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.designsystem.AiluaSectionHeader
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.themeengine.LocalAiluaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivingScreen(
    character: CharacterProfile = MockData.sampleCharacter,
    isDarkTheme: Boolean = false,
    worldClockOverride: WorldClock? = null,
    onToggleTheme: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToCall: () -> Unit = {},
    onNavigateToMailbox: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onGoHome: () -> Unit = onBackToHome
) {
    val theme = LocalAiluaTheme.current
    val scrollState = rememberScrollState()
    val worldEvents by WorldStateRepository.events.collectAsStateWithLifecycle()
    val worldClock by WorldHeartbeatEngine.worldClock.collectAsStateWithLifecycle()
    var showDevTimeSheet by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    val scene = worldClockOverride ?: worldClock

    val seedTimeline = remember(character.id) {
        MockData.getTimelineForCharacter(character.id).ifEmpty { character.timeline }
    }
    val projection = remember(character.id, worldEvents) {
        projectLiving(
            character = character,
            seedTimeline = seedTimeline,
            runtimeEvents = worldEvents,
            seedEventIds = MockData.unifiedLifeEvents.mapTo(HashSet()) { it.id }
        )
    }
    val timelineEvents = projection.timeline
    val timelineGroups = remember(timelineEvents, scene.minutesOfDay) {
        groupLivingTimeline(timelineEvents, scene.minutesOfDay)
    }

    AiluaScreenScaffold(
        title = "生活",
        onBack = onBackToHome,
        backTestTag = "living_back_btn",
        onGoHome = onGoHome,
        modifier = Modifier.testTag("living_screen"),
        trailing = {
            IconButton(onClick = { showMore = true }) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "更多生活操作",
                    tint = theme.palette.onSurface
                )
            }
            DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                DropdownMenuItem(
                    text = { Text("写封信", style = theme.text.body) },
                    onClick = {
                        showMore = false
                        onNavigateToMailbox()
                    }
                )
                DropdownMenuItem(
                    text = { Text("调整世界时间", style = theme.text.body) },
                    onClick = {
                        showMore = false
                        showDevTimeSheet = true
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = theme.layout.screenHorizontalPadding.dp),
            verticalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp)
        ) {
            LivingSceneHero(
                character = character,
                scene = scene,
                currentActivity = projection.currentActivity,
                currentLocation = projection.currentLocation,
                onOpenProfile = onOpenProfile
            )

            if (character.contextualQuote.isNotBlank()) {
                Text(
                    text = character.contextualQuote,
                    style = theme.text.body,
                    color = theme.palette.onSurfaceMuted,
                    modifier = Modifier.testTag("living_quote_line")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)
            ) {
                AiluaChip(
                    label = "发消息",
                    onClick = onNavigateToChat,
                    selected = true,
                    modifier = Modifier.weight(1f),
                    leading = {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = theme.palette.onSurface, modifier = Modifier.size(18.dp))
                    }
                )
                AiluaChip(
                    label = "打电话",
                    onClick = onNavigateToCall,
                    modifier = Modifier.weight(1f),
                    leading = {
                        Icon(Icons.Default.Call, contentDescription = null, tint = theme.palette.onSurfaceMuted, modifier = Modifier.size(18.dp))
                    }
                )
            }

            Column {
                // The existing projection carries clock times without dates, so do not label historical events as today.
                AiluaSectionHeader(title = "最近动态")
                Spacer(modifier = Modifier.height(theme.layout.itemGap.dp))
                if (timelineGroups.isEmpty()) {
                    Text("还没有新的动态", style = theme.text.body, color = theme.palette.onSurfaceMuted)
                } else {
                    timelineGroups.forEach { group ->
                        group.events.forEach { event -> TimelineEventRow(event) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(theme.layout.itemGap.dp))
        }
    }

    if (showDevTimeSheet) {
        WorldTimeDevSheet(onDismiss = { showDevTimeSheet = false })
    }
}

@Composable
private fun LivingSceneHero(
    character: CharacterProfile,
    scene: WorldClock,
    currentActivity: String,
    currentLocation: String,
    onOpenProfile: () -> Unit
) {
    val theme = LocalAiluaTheme.current
    AiluaMediaFrame(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .testTag("living_scene_hero")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(theme.layout.screenHorizontalPadding.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${scene.dayPhase.label} · ${scene.weather.label}",
                    style = theme.text.caption,
                    color = theme.palette.onSurfaceMuted
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = currentLocation,
                    style = theme.text.caption,
                    color = theme.palette.onSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            CharacterPortrait(
                characterId = character.id,
                variant = PortraitVariant.HERO,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(onClickLabel = "查看${character.name}的资料", onClick = onOpenProfile)
            )
            Text(text = character.name, style = theme.text.title, color = theme.palette.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentActivity,
                style = theme.text.body,
                color = theme.palette.onSurfaceMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TimelineEventRow(event: TimelineEvent) {
    val theme = LocalAiluaTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = theme.layout.itemGap.dp),
        horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = event.time,
            style = theme.text.secondary,
            color = if (event.isCurrent) theme.palette.accent else theme.palette.onSurfaceMuted,
            modifier = Modifier.width(48.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = com.example.ui.designsystem.CharacterDisplayNames.projectLegacyMentions(event.title),
                style = theme.text.body, color = theme.palette.onSurface)
            if (event.description.isNotBlank() && event.description != event.title) {
                Text(text = com.example.ui.designsystem.CharacterDisplayNames.projectLegacyMentions(event.description),
                    style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
            }
        }
    }
}
