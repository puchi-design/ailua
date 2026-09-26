package com.example.ui.living

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.mock.MockData
import com.example.data.model.CharacterProfile
import com.example.data.model.TimelineEvent
import com.example.ui.components.AiluaAvatar
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.components.WorldTimeDevSheet
import com.example.ui.theme.AiluaDustyRose
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold
import com.example.ui.theme.AiluaMutedLavender
import kotlinx.coroutines.launch

@Composable
fun LivingScreen(
    character: CharacterProfile = MockData.sampleCharacter,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val worldEvents by WorldStateRepository.events.collectAsStateWithLifecycle()
    val worldClock by WorldHeartbeatEngine.worldClock.collectAsStateWithLifecycle()
    var showDevTimeSheet by remember { mutableStateOf(false) }

    val timelineEvents = remember(character.id, worldEvents) {
        val extraEvents = worldEvents.filter { it.characterId == character.id }.map { lifeEvent ->
            TimelineEvent(
                id = lifeEvent.id,
                time = lifeEvent.time,
                title = lifeEvent.title,
                description = lifeEvent.description,
                location = lifeEvent.location ?: character.location,
                mood = character.mood,
                relatedCharacterIds = lifeEvent.relatedCharacterIds
            )
        }
        val staticEvents = MockData.getTimelineForCharacter(character.id).ifEmpty { character.timeline }
        (extraEvents + staticEvents).distinctBy { it.id }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("living_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Virtual OS Status Bar
            VirtualPhoneStatusBar(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackToHome,
                        modifier = Modifier.testTag("living_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回AILUA主屏",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "生活 · Living",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "她在你视线之外的真实独立世界",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { showDevTimeSheet = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "时间跃迁",
                            tint = AiluaMistBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Chat direct shortcut
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                            .clickable { onNavigateToChat() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "找她说话",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Scrollable Living View
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Hero Living Presence Banner
                LivingHeroCard(
                    character = character,
                    onOpenProfile = onOpenProfile
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Touch Actions (敲敲窗户, 递热茶, 分享心绪)
                Text(
                    text = "轻触互动",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InteractionButton(
                        title = "敲敲飘窗",
                        icon = Icons.Default.TouchApp,
                        modifier = Modifier.weight(1f)
                    ) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Mira 听到轻叩玻璃的声音，转过头对你甜甜一笑：『你来啦？』")
                        }
                    }
                    InteractionButton(
                        title = "递一杯暖茶",
                        icon = Icons.Default.Coffee,
                        modifier = Modifier.weight(1f)
                    ) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Mira 双手捧过热茶，眼眸里泛着柔和的光泽：『正好有点口渴，谢谢你。』")
                        }
                    }
                    InteractionButton(
                        title = "静静陪坐",
                        icon = Icons.Default.Hearing,
                        modifier = Modifier.weight(1f)
                    ) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("你在窗边坐下，两人一起静静听着雨打在青石板上的声响…")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Daily Timeline: Schedule of the Day
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "今日生活轨迹 · Timeline",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "心网独立存续中",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = Color(0xFF6EC6A1)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Timeline List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.03f),
                            spotColor = Color.Black.copy(alpha = 0.06f)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    timelineEvents.forEachIndexed { index, event ->
                        TimelineEventRow(
                            event = event,
                            isLast = index == timelineEvents.size - 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Virtual Home Indicator Bar
            VirtualPhoneHomeBar(
                canGoBack = true,
                onBack = onBackToHome,
                onGoHome = onBackToHome
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp)
        )

        if (showDevTimeSheet) {
            WorldTimeDevSheet(onDismiss = { showDevTimeSheet = false })
        }
    }
}

@Composable
private fun LivingHeroCard(
    character: CharacterProfile,
    onOpenProfile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                RoundedCornerShape(24.dp)
            )
            .padding(18.dp)
            .testTag("living_hero_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Large Avatar with pulsating halo
            AiluaAvatar(
                size = 92.dp,
                showHalo = true,
                showLivingStatus = true,
                statusText = "生活进行中",
                onClick = onOpenProfile
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Name and Title
            Text(
                text = "${character.name} · ${character.englishName}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 19.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = character.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Status Metric Chips (Location, Mood, Energy, Bond)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusMetricItem(
                    label = "当前心境",
                    value = character.mood,
                    accentColor = AiluaMistBlue
                )
                StatusMetricItem(
                    label = "活力体能",
                    value = "充沛 · ${character.energyLevel}%",
                    accentColor = Color(0xFF6EC6A1)
                )
                StatusMetricItem(
                    label = "心契羁绊",
                    value = "Lv.${character.bondLevel} (${character.daysTogether}天)",
                    accentColor = AiluaDustyRose
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Environment context banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = AiluaMistBlue
                    )
                    Text(
                        text = "所在位置：${character.location}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusMetricItem(
    label: String,
    value: String,
    accentColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp
            ),
            color = accentColor
        )
    }
}

@Composable
private fun InteractionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(18.dp),
                tint = AiluaMutedLavender
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TimelineEventRow(
    event: TimelineEvent,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left: Time & Dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(44.dp)
        ) {
            Text(
                text = event.time,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (event.isCurrent) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                ),
                color = if (event.isCurrent) AiluaMistBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(if (event.isCurrent) 10.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (event.isCurrent) Color(0xFF6EC6A1) else MaterialTheme.colorScheme.outlineVariant)
            )
        }

        // Right: Event Details
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (event.isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else Color.Transparent
                )
                .padding(if (event.isCurrent) 8.dp else 0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (event.isCurrent) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF6EC6A1).copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "当前进行中",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF3B9B70)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
        }
    }
}
