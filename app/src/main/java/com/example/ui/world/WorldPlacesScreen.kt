package com.example.ui.world

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.mock.WorldData
import com.example.data.model.VirtualPlace
import com.example.ui.components.AiluaAvatar
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.theme.AiluaDustyRose
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold
import com.example.ui.theme.AiluaMutedLavender

@Composable
fun WorldPlacesScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {},
    onVisitPlaceChat: (String) -> Unit = {}
) {
    val places = WorldData.virtualPlaces
    var selectedPlace by remember { mutableStateOf(places.first()) }
    val heartbeatState by WorldHeartbeatEngine.heartbeatState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("world_places_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Status bar
            VirtualPhoneStatusBar(isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme)

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("places_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "心网世界 · World Map",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "AILUA 伴生生命体栖居空间与地理脉络",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                // Heartbeat Phase Cycler
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AiluaMoonGold.copy(alpha = 0.18f))
                        .clickable { WorldHeartbeatEngine.cycleTimePhase() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(heartbeatState.currentPhase.icon, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = heartbeatState.currentPhase.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AiluaMoonGold
                        )
                    }
                }
            }

            // Atmosphere Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = AiluaMistBlue
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${heartbeatState.currentPhase.atmosphere} (点击右上角可切换时辰)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Fictional Interactive World Map Canvas & Nodes
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF2C3140),
                                Color(0xFF1E222D),
                                Color(0xFF161922)
                            )
                        )
                    )
            ) {
                // Background constellation / grid routes
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw connecting pathways between places
                    places.forEach { place ->
                        val startX = place.coordinateX * w
                        val startY = place.coordinateY * h

                        place.connectedPlaceIds.forEach { targetId ->
                            val target = places.firstOrNull { it.id == targetId }
                            if (target != null) {
                                val endX = target.coordinateX * w
                                val endY = target.coordinateY * h
                                drawLine(
                                    color = Color.White.copy(alpha = 0.15f),
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = 2f
                                )
                            }
                        }
                    }
                }

                // Place Interactive Nodes
                places.forEach { place ->
                    val isSelected = selectedPlace.id == place.id
                    val hasCurrentChar = place.currentCharacterIds.isNotEmpty()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .align(
                                    when {
                                        place.coordinateX < 0.35f && place.coordinateY < 0.4f -> Alignment.TopStart
                                        place.coordinateX > 0.65f && place.coordinateY < 0.4f -> Alignment.TopEnd
                                        place.coordinateX < 0.35f -> Alignment.BottomStart
                                        place.coordinateX > 0.65f -> Alignment.BottomEnd
                                        place.coordinateY < 0.45f -> Alignment.TopCenter
                                        else -> Alignment.Center
                                    }
                                )
                                .clickable { selectedPlace = place },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 36.dp else 28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) AiluaMistBlue
                                        else if (hasCurrentChar) AiluaMoonGold
                                        else Color.White.copy(alpha = 0.2f)
                                    )
                                    .border(
                                        2.dp,
                                        if (isSelected) Color.White else Color.Transparent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasCurrentChar) {
                                    AiluaAvatar(avatarId = place.currentCharacterIds.first(), size = 26.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = place.name,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AiluaMoonGold else Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Places Horizontal Selector
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(places) { place ->
                    val isSelected = selectedPlace.id == place.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) AiluaMistBlue.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                1.dp,
                                if (isSelected) AiluaMistBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedPlace = place }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = place.name,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AiluaMistBlue else MaterialTheme.colorScheme.onSurface
                            )
                            if (place.currentCharacterIds.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AiluaMoonGold)
                                )
                            }
                        }
                    }
                }
            }

            // Selected Place Detailed Info Sheet
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = selectedPlace.name,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(AiluaMistBlue.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(selectedPlace.type, fontSize = 10.sp, color = AiluaMistBlue)
                                        }
                                    }
                                    Text(
                                        text = "氛围: ${selectedPlace.mood}",
                                        fontSize = 11.sp,
                                        color = AiluaMoonGold
                                    )
                                }

                                if (selectedPlace.currentCharacterIds.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("当前在此:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        selectedPlace.currentCharacterIds.forEach { cid ->
                                            AiluaAvatar(avatarId = cid, size = 32.dp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = selectedPlace.description,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Environmental sound
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = AiluaDustyRose,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "空间音景: ${selectedPlace.ambientAudioNote}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Recent events in this place
                            Text(
                                text = "近期空间痕迹:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            selectedPlace.recentEvents.forEach { ev ->
                                Text(
                                    text = "• $ev",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action: Visit & Chat with present character
                            Button(
                                onClick = {
                                    val targetChar = selectedPlace.currentCharacterIds.firstOrNull()
                                        ?: selectedPlace.residentCharacterIds.firstOrNull()
                                        ?: "mira"
                                    onVisitPlaceChat(targetChar)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AiluaMistBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedPlace.currentCharacterIds.isNotEmpty())
                                        "拜访此地 · 与角色开启交谈"
                                    else "漫步驻足 · 感受静谧空间",
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(14.dp)) }
            }

            // Home bar
            VirtualPhoneHomeBar(canGoBack = true, onBack = onBack, onGoHome = onBack)
        }
    }
}
