package com.example.ui.living

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.mock.MockData
import com.example.data.model.CharacterProfile
import com.example.data.model.DayPhase
import com.example.data.model.TimelineEvent
import com.example.data.model.WeatherState
import com.example.data.model.WorldClock
import com.example.ui.components.AiluaAvatar
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.components.WorldTimeDevSheet
import com.example.ui.components.wallpaperPalette
import com.example.ui.theme.AiluaDustyRose
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold
import com.example.ui.theme.AiluaMutedLavender
import com.example.ui.theme.AiluaNightTextPrimary
import com.example.ui.theme.AiluaNightTextSecondary
import com.example.ui.theme.AiluaCharcoal
import com.example.ui.theme.AiluaTextSecondary

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
    onOpenProfile: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val worldEvents by WorldStateRepository.events.collectAsStateWithLifecycle()
    val worldClock by WorldHeartbeatEngine.worldClock.collectAsStateWithLifecycle()
    var showDevTimeSheet by remember { mutableStateOf(false) }
    val scene = worldClockOverride ?: worldClock

    val timelineEvents = remember(character.id, worldEvents) {
        val staticEvents = MockData.getTimelineForCharacter(character.id).ifEmpty { character.timeline }
        val worldMapped = worldEvents.filter { it.characterId == character.id }.map { lifeEvent ->
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
        (staticEvents + worldMapped).distinctBy { it.time }
    }
    val timelineGroups = remember(timelineEvents, scene.minutesOfDay) {
        groupLivingTimeline(timelineEvents, scene.minutesOfDay)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("living_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VirtualPhoneStatusBar(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )

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
                            text = "推开窗，看看她现在正在过的生活",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

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
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                LivingSceneHero(
                    character = character,
                    scene = scene,
                    isDarkTheme = isDarkTheme,
                    onOpenProfile = onOpenProfile
                )

                if (character.contextualQuote.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LivingQuoteLine(quote = character.contextualQuote)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "和她互动",
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
                    LivingActionPill(
                        title = "和她说说话",
                        icon = Icons.Default.ChatBubbleOutline,
                        accent = AiluaMutedLavender,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToChat
                    )
                    LivingActionPill(
                        title = "打个电话",
                        icon = Icons.Default.Call,
                        accent = AiluaMistBlue,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCall
                    )
                    LivingActionPill(
                        title = "给她写信",
                        icon = Icons.Default.Email,
                        accent = AiluaMoonGold,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMailbox
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "今天 · 生活记录",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = scene.dateLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }

                timelineGroups.forEach { group ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(AiluaMoonGold)
                        )
                        Text(
                            text = group.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    group.events.forEach { event ->
                        TimelineEventRow(event = event)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LivingMetricChip(
                        label = "心境",
                        value = character.mood,
                        accentColor = AiluaMistBlue,
                        modifier = Modifier.weight(1f)
                    )
                    LivingMetricChip(
                        label = "活力",
                        value = "${character.energyLevel}%",
                        accentColor = Color(0xFF6EC6A1),
                        modifier = Modifier.weight(1f)
                    )
                    LivingMetricChip(
                        label = "羁绊",
                        value = "Lv.${character.bondLevel} · ${character.daysTogether}天",
                        accentColor = AiluaDustyRose,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            VirtualPhoneHomeBar(
                canGoBack = true,
                onBack = onBackToHome,
                onGoHome = onBackToHome
            )
        }

        if (showDevTimeSheet) {
            WorldTimeDevSheet(onDismiss = { showDevTimeSheet = false })
        }
    }
}

@Composable
private fun LivingSceneHero(
    character: CharacterProfile,
    scene: WorldClock,
    isDarkTheme: Boolean,
    onOpenProfile: () -> Unit
) {
    val palette = remember(scene.dayPhase, scene.weather, isDarkTheme) {
        wallpaperPalette(scene.dayPhase, scene.weather, isDarkTheme)
    }
    val heroText = if (isDarkTheme) AiluaNightTextPrimary else AiluaCharcoal
    val heroTextSecondary = if (isDarkTheme) AiluaNightTextSecondary else AiluaTextSecondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(26.dp))
            .testTag("living_scene_hero")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(palette.colors))
            )
            LivingSceneAtmosphere(
                dayPhase = scene.dayPhase,
                weather = scene.weather
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SceneChip(text = scene.dayPhase.label, contentColor = heroText)
                        SceneChip(text = scene.weather.label, contentColor = heroText)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(heroText.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = heroText.copy(alpha = 0.7f)
                        )
                        Text(
                            text = character.location,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = heroText.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 150.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AiluaAvatar(
                        size = 92.dp,
                        showHalo = true,
                        showLivingStatus = true,
                        statusText = "生活进行中",
                        onClick = onOpenProfile
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${character.name} · ${character.englishName}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        ),
                        color = heroText
                    )
                    Text(
                        text = character.title,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = heroTextSecondary.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                        .border(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6EC6A1))
                    )
                    Text(
                        text = "此刻",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = character.currentActivity,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LivingSceneAtmosphere(
    dayPhase: DayPhase,
    weather: WeatherState
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val isNight = dayPhase == DayPhase.EVENING ||
            dayPhase == DayPhase.NIGHT ||
            dayPhase == DayPhase.LATE_NIGHT
        val discColor = if (isNight) {
            Color(0xFFF3F0FA).copy(alpha = 0.55f)
        } else {
            Color(0xFFFFE3AE).copy(alpha = 0.65f)
        }
        val discCenter = Offset(w * 0.78f, h * 0.2f)
        drawCircle(
            color = discColor.copy(alpha = 0.22f),
            radius = 78.dp.toPx(),
            center = discCenter
        )
        drawCircle(
            color = discColor,
            radius = 34.dp.toPx(),
            center = discCenter
        )

        when (weather) {
            WeatherState.CLEAR -> Unit
            WeatherState.CLOUDY -> {
                drawOval(
                    color = Color.White.copy(alpha = if (isNight) 0.08f else 0.35f),
                    topLeft = Offset(w * 0.08f, h * 0.1f),
                    size = androidx.compose.ui.geometry.Size(w * 0.42f, h * 0.09f)
                )
                drawOval(
                    color = Color.White.copy(alpha = if (isNight) 0.06f else 0.28f),
                    topLeft = Offset(w * 0.46f, h * 0.16f),
                    size = androidx.compose.ui.geometry.Size(w * 0.34f, h * 0.07f)
                )
            }
            WeatherState.RAIN, WeatherState.HEAVY_RAIN -> {
                val rainColor = if (isNight) Color(0xFFAFC4DC) else Color(0xFF9FB6CE)
                val alpha = if (weather == WeatherState.HEAVY_RAIN) 0.28f else 0.16f
                val streak = w * 0.05f
                repeat(8) { i ->
                    val startX = w * (0.06f + 0.12f * i)
                    val startY = h * (0.04f + 0.05f * (i % 3))
                    drawLine(
                        color = rainColor.copy(alpha = alpha),
                        start = Offset(startX, startY),
                        end = Offset(startX - streak, startY + streak * 1.6f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            WeatherState.SNOW -> {
                val dots = listOf(
                    0.12f to 0.1f, 0.3f to 0.22f, 0.52f to 0.08f, 0.7f to 0.3f,
                    0.88f to 0.14f, 0.2f to 0.34f, 0.44f to 0.28f, 0.94f to 0.36f,
                    0.62f to 0.16f, 0.05f to 0.26f
                )
                dots.forEachIndexed { i, (fx, fy) ->
                    drawCircle(
                        color = Color.White.copy(alpha = if (isNight) 0.5f else 0.85f),
                        radius = (2.4f + (i % 3) * 0.9f).dp.toPx(),
                        center = Offset(w * fx, h * fy)
                    )
                }
            }
        }
    }
}

@Composable
private fun SceneChip(
    text: String,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(contentColor.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = contentColor.copy(alpha = 0.75f),
            maxLines = 1
        )
    }
}

@Composable
private fun LivingQuoteLine(quote: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("living_quote_line"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.FormatQuote,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = AiluaMoonGold
        )
        Text(
            text = quote,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontStyle = FontStyle.Italic
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun LivingActionPill(
    title: String,
    icon: ImageVector,
    accent: Color,
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
            .padding(vertical = 11.dp, horizontal = 6.dp),
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
                tint = accent
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LivingMetricChip(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TimelineEventRow(
    event: TimelineEvent
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
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
                            text = "现在进行中",
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
