package com.example.ui.character

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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.mock.MockData
import com.example.data.model.CharacterProfile
import com.example.data.context.CharacterContext
import com.example.data.projection.projectLiving
import com.example.ui.designsystem.AiluaChip
import com.example.ui.designsystem.AiluaMediaFrame
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.designsystem.AiluaSectionHeader
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.living.groupLivingTimeline
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun CharacterProfileScreen(
    character: CharacterProfile = MockData.sampleCharacter,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onClose: () -> Unit = {},
    onStartChat: () -> Unit = {},
    onOpenLiving: () -> Unit = {},
    onGoHome: () -> Unit = onClose
) {
    val theme = LocalAiluaTheme.current
    val scrollState = rememberScrollState()
    val worldEvents by WorldStateRepository.events.collectAsStateWithLifecycle()
    val worldClock by WorldHeartbeatEngine.worldClock.collectAsStateWithLifecycle()
    val selectedId by CharacterContext.selectedId.collectAsStateWithLifecycle()
    val projection = remember(character.id, worldEvents) {
        projectLiving(
            character = character,
            seedTimeline = MockData.getTimelineForCharacter(character.id).ifEmpty { character.timeline },
            runtimeEvents = worldEvents,
            seedEventIds = MockData.unifiedLifeEvents.mapTo(HashSet()) { it.id }
        )
    }
    val recentEvents = remember(projection.timeline, worldClock.minutesOfDay) {
        groupLivingTimeline(projection.timeline, worldClock.minutesOfDay)
            .flatMap { it.events }
            .take(3)
    }

    AiluaScreenScaffold(
        title = "资料",
        onBack = onClose,
        onGoHome = onGoHome,
        modifier = Modifier.testTag("character_profile_screen")
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = theme.layout.screenHorizontalPadding.dp),
            verticalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                AiluaMediaFrame(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                    CharacterPortrait(
                        characterId = character.id,
                        variant = PortraitVariant.PROFILE,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(text = character.name, style = theme.text.display, color = theme.palette.onSurface)
                Text(text = character.title, style = theme.text.body, color = theme.palette.onSurfaceMuted)
                if (selectedId != character.id) {
                    TextButton(onClick = { CharacterContext.select(character.id) }, modifier = Modifier.testTag("profile_select_character")) {
                        Text("在主屏显示", style = theme.text.secondary)
                    }
                }
                Text(
                    text = listOf(projection.currentActivity, projection.currentLocation)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = theme.text.secondary,
                    color = theme.palette.onSurfaceMuted
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)
            ) {
                AiluaChip(
                    label = "发消息",
                    onClick = onStartChat,
                    selected = true,
                    modifier = Modifier.weight(1f),
                    leading = {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = theme.palette.onSurface, modifier = Modifier.size(18.dp))
                    }
                )
                AiluaChip(
                    label = "生活",
                    onClick = onOpenLiving,
                    modifier = Modifier.weight(1f),
                    leading = {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = theme.palette.onSurfaceMuted, modifier = Modifier.size(18.dp))
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                AiluaSectionHeader(title = "关于${character.name}")
                Text(text = character.bio, style = theme.text.body, color = theme.palette.onSurface)
                if (character.personalityTags.isNotEmpty()) {
                    Text(
                        text = character.personalityTags.joinToString(" · "),
                        style = theme.text.secondary,
                        color = theme.palette.onSurfaceMuted
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                AiluaSectionHeader(title = "你们之间")
                if (character.relationshipType.isNotBlank()) {
                    Text(
                        text = character.relationshipType,
                        style = theme.text.secondary,
                        color = theme.palette.onSurfaceMuted
                    )
                }
                if (character.memories.isEmpty()) {
                    Text("还没有留下共同记忆", style = theme.text.body, color = theme.palette.onSurfaceMuted)
                }
                character.memories.forEach { memory ->
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = theme.layout.itemGap.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = memory.date, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                        Text(text = memory.title, style = theme.text.section, color = theme.palette.onSurface)
                        Text(text = memory.snippet, style = theme.text.body, color = theme.palette.onSurfaceMuted)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                AiluaSectionHeader(title = "最近的事", actionLabel = "查看生活", onAction = onOpenLiving)
                if (recentEvents.isEmpty()) {
                    Text("还没有新的动态", style = theme.text.body, color = theme.palette.onSurfaceMuted)
                }
                recentEvents.forEach { event ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = theme.layout.itemGap.dp),
                        horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)
                    ) {
                        Text(
                            text = event.time,
                            style = theme.text.secondary,
                            color = theme.palette.onSurfaceMuted,
                            modifier = Modifier.width(48.dp)
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = com.example.ui.designsystem.CharacterDisplayNames.projectLegacyMentions(event.title),
                                style = theme.text.body, color = theme.palette.onSurface)
                            if (event.description.isNotBlank() && event.description != event.title) {
                                Text(
                                    text = com.example.ui.designsystem.CharacterDisplayNames.projectLegacyMentions(event.description),
                                    style = theme.text.secondary,
                                    color = theme.palette.onSurfaceMuted
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(theme.layout.itemGap.dp))
        }
    }
}
