package com.example.ui.world

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.engine.UserActivityRecorder
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.mock.WorldData
import com.example.data.model.VirtualPlace
import com.example.data.registry.CharacterRegistry
import com.example.ui.designsystem.AiluaChip
import com.example.ui.designsystem.AiluaMediaFrame
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.designsystem.AiluaSectionHeader
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun WorldPlacesScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {},
    onVisitPlaceChat: (String) -> Unit = {},
    onGoHome: () -> Unit = onBack,
) {
    val theme = LocalAiluaTheme.current
    val places = WorldData.virtualPlaces
    var selectedPlace by remember { mutableStateOf(places.first()) }
    val heartbeatState by WorldHeartbeatEngine.heartbeatState.collectAsStateWithLifecycle()

    AiluaScreenScaffold(
        title = "地点",
        onBack = onBack,
        onGoHome = onGoHome,
        backTestTag = "places_back_btn",
        modifier = Modifier.testTag("world_places_screen"),
        trailing = {
            AiluaChip(
                label = heartbeatState.currentPhase.label,
                onClick = { WorldHeartbeatEngine.cycleTimePhase() },
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = theme.layout.screenHorizontalPadding.dp,
                vertical = theme.layout.itemGap.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                    AiluaMediaFrame(Modifier.fillMaxWidth().height(120.dp)) {
                        PlaceMapFallback(places = places, selectedPlaceId = selectedPlace.id)
                    }
                    Text(
                        heartbeatState.currentPhase.atmosphere,
                        style = theme.text.secondary,
                        color = theme.palette.onSurfaceMuted,
                    )
                    Text("点击右上角切换时段", style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                    AiluaSectionHeader("附近地点", modifier = Modifier.padding(top = theme.layout.itemGap.dp))
                }
            }
            items(places, key = { it.id }) { place ->
                val isSelected = selectedPlace.id == place.id
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { selectedPlace = place }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                place.name,
                                style = theme.text.section,
                                color = if (isSelected) theme.palette.accent else theme.palette.onSurface,
                            )
                            Text("${place.type} · ${place.mood}", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                        }
                        Icon(
                            if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isSelected) "已展开" else "查看地点",
                            tint = theme.palette.onSurfaceMuted,
                        )
                    }
                    AnimatedVisibility(isSelected) {
                        PlaceDetails(place = place, onVisitPlaceChat = onVisitPlaceChat)
                    }
                    HorizontalDivider(color = theme.surfaces.divider)
                }
            }
        }
    }
}

@Composable
private fun PlaceDetails(place: VirtualPlace, onVisitPlaceChat: (String) -> Unit) {
    val theme = LocalAiluaTheme.current
    Column(
        modifier = Modifier.padding(bottom = theme.layout.sectionGap.dp),
        verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
    ) {
        Text(place.description, style = theme.text.body, color = theme.palette.onSurface)
        if (place.currentCharacterIds.isNotEmpty()) {
            Text("当前在这里", style = theme.text.caption, color = theme.palette.onSurfaceMuted)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                items(place.currentCharacterIds) { characterId ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CharacterPortrait(characterId, PortraitVariant.AVATAR, Modifier.size(40.dp))
                        Text(CharacterRegistry.getCharacter(characterId).name, style = theme.text.secondary, color = theme.palette.onSurface)
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = theme.palette.onSurfaceMuted, modifier = Modifier.size(18.dp))
            Text(place.ambientAudioNote, style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
        }
        if (place.recentEvents.isNotEmpty()) {
            Text("最近发生的事", style = theme.text.caption, color = theme.palette.onSurfaceMuted)
            place.recentEvents.forEach { event ->
                Text(event, style = theme.text.body, color = theme.palette.onSurface)
            }
        }
        Button(
            onClick = {
                val targetChar = place.currentCharacterIds.firstOrNull()
                    ?: place.residentCharacterIds.firstOrNull()
                    ?: "mira"
                UserActivityRecorder.recordPlaceVisit(
                    characterId = targetChar,
                    placeName = place.name,
                )
                onVisitPlaceChat(targetChar)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                if (place.currentCharacterIds.isNotEmpty()) "拜访并聊天" else "去这里走走",
                style = theme.text.body,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** Lightweight existing map geometry, kept separate from place selection and details. */
@Composable
private fun PlaceMapFallback(places: List<VirtualPlace>, selectedPlaceId: String) {
    val theme = LocalAiluaTheme.current
    val routeColor = theme.palette.onSurfaceMuted.copy(alpha = 0.18f)
    val nodeColor = theme.palette.onSurfaceMuted.copy(alpha = 0.45f)
    val selectedColor = theme.palette.accent
    Canvas(Modifier.fillMaxSize().padding(16.dp)) {
        places.forEach { place ->
            val start = Offset(place.coordinateX * size.width, place.coordinateY * size.height)
            place.connectedPlaceIds.forEach { targetId ->
                places.firstOrNull { it.id == targetId }?.let { target ->
                    drawLine(
                        color = routeColor,
                        start = start,
                        end = Offset(target.coordinateX * size.width, target.coordinateY * size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            val isSelected = selectedPlaceId == place.id
            drawCircle(
                color = if (isSelected) selectedColor else nodeColor,
                radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                center = start,
            )
        }
    }
}
