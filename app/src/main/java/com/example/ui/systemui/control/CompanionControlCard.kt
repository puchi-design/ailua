package com.example.ui.systemui.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.context.CharacterContext
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.registry.CharacterRegistry
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun CompanionControlCard(onLaunchRoute: (String) -> Unit, modifier: Modifier = Modifier) {
    val characterId by CharacterContext.selectedId.collectAsStateWithLifecycle()
    val cards by CharacterRegistry.allCards.collectAsStateWithLifecycle()
    val events by WorldStateRepository.events.collectAsStateWithLifecycle()
    val clock by WorldHeartbeatEngine.worldClock.collectAsStateWithLifecycle()
    val character = remember(characterId, cards) { CharacterRegistry.getCharacter(characterId) }
    val latest = remember(characterId, events) { WorldStateRepository.latestForCharacter(characterId) }
    val spec = LocalAiluaTheme.current.controlCenter.tileStyle
    Surface(
        color = spec.backgroundColor.copy(alpha = spec.surfaceAlpha), contentColor = spec.foregroundColor,
        shape = RoundedCornerShape(LocalAiluaTheme.current.shapes.large.dp),
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth().testTag("control_companion").clickable { onLaunchRoute("living") },
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(character.name, style = LocalAiluaTheme.current.text.section,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            Text(latest?.title ?: character.currentActivity, style = LocalAiluaTheme.current.text.secondary,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${clock.timeFormatted} · ${clock.weather.label}",
                color = spec.foregroundColor.copy(alpha = 0.7f), style = LocalAiluaTheme.current.text.caption)
        }
    }
}
