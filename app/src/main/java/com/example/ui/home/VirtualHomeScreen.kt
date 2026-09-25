package com.example.ui.home

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.mock.MockData
import com.example.data.model.CharacterProfile
import com.example.ui.components.AppIconItem
import com.example.ui.components.BondProgressWidget
import com.example.ui.components.LivingCharacterWidget
import com.example.ui.components.MemorySnippetWidget
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold
import com.example.ui.theme.AiluaMutedLavender

@Composable
fun VirtualHomeScreen(
    character: CharacterProfile = MockData.sampleCharacter,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToMoments: () -> Unit = {},
    onNavigateToLiving: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    onNavigateToApps: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onAppClick: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // 10 Apps on Virtual Phone Home Grid
    val homeApps = listOf(
        HomeAppDef("chat", "通讯", "chat", "2"),
        HomeAppDef("moments", "瞬间", "moments", "New"),
        HomeAppDef("living", "生活", "living", null),
        HomeAppDef("memories", "记忆", "memories", null),
        HomeAppDef("gallery", "影集", "gallery", null),
        HomeAppDef("world", "拟界", "world", "Beta"),
        HomeAppDef("games", "游艺", "games", null),
        HomeAppDef("apps", "应用库", "apps", null),
        HomeAppDef("bridge", "现实桥", "bridge", null),
        HomeAppDef("assistant", "执事", "assistant", null)
    )

    // Wallpaper subtle gradient (soft mist cream in light mode, deep starry twilight in dark)
    val wallpaperGradient = if (isDarkTheme) {
        listOf(
            Color(0xFF13121C),
            Color(0xFF191724),
            Color(0xFF1F1B2F)
        )
    } else {
        listOf(
            Color(0xFFF9F6F0),
            Color(0xFFF4F0E8),
            Color(0xFFEBE6DC)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(wallpaperGradient))
            .testTag("virtual_home_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Virtual OS Status Bar
            VirtualPhoneStatusBar(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )

            // Scrollable Virtual Desktop
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // Hero Living Character Widget
                LivingCharacterWidget(
                    character = character,
                    onOpenLiving = onNavigateToLiving,
                    onOpenChat = onNavigateToChat,
                    onOpenProfile = onOpenProfile
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Secondary Widgets Row: Bond & Memory
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BondProgressWidget(
                        character = character,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenProfile
                    )

                    MemorySnippetWidget(
                        title = "记忆回响",
                        snippet = character.memories.firstOrNull()?.snippet ?: "城市夜晚很温柔…",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMemories
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Desktop Apps Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(AiluaMoonGold)
                        )
                        Text(
                            text = "心网应用",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onNavigateToApps() }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "全部 17 款",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = AiluaMistBlue
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "查看全部应用",
                            modifier = Modifier.size(14.dp),
                            tint = AiluaMistBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2 Rows of 5 App Icons
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        homeApps.take(5).forEach { app ->
                            AppIconItem(
                                name = app.name,
                                iconKey = app.iconKey,
                                badge = app.badge,
                                onClick = {
                                    handleAppNavigation(
                                        app.id,
                                        onNavigateToChat,
                                        onNavigateToMoments,
                                        onNavigateToLiving,
                                        onNavigateToMemories,
                                        onNavigateToApps,
                                        onAppClick
                                    )
                                }
                            )
                        }
                    }

                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        homeApps.drop(5).take(5).forEach { app ->
                            AppIconItem(
                                name = app.name,
                                iconKey = app.iconKey,
                                badge = app.badge,
                                onClick = {
                                    handleAppNavigation(
                                        app.id,
                                        onNavigateToChat,
                                        onNavigateToMoments,
                                        onNavigateToLiving,
                                        onNavigateToMemories,
                                        onNavigateToApps,
                                        onAppClick
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Virtual Phone Dock (Frosted / Soft Pill)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(26.dp),
                            ambientColor = Color.Black.copy(alpha = 0.05f),
                            spotColor = Color.Black.copy(alpha = 0.1f)
                        )
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(26.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                        .testTag("virtual_phone_dock")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconItem(
                            name = "通讯",
                            iconKey = "chat",
                            badge = "2",
                            showLabel = false,
                            onClick = onNavigateToChat
                        )
                        AppIconItem(
                            name = "动态",
                            iconKey = "moments",
                            badge = "New",
                            showLabel = false,
                            onClick = onNavigateToMoments
                        )
                        AppIconItem(
                            name = "生活",
                            iconKey = "living",
                            showLabel = false,
                            onClick = onNavigateToLiving
                        )
                        AppIconItem(
                            name = "应用",
                            iconKey = "apps",
                            showLabel = false,
                            onClick = onNavigateToApps
                        )
                    }
                }
            }

            // Virtual Home Indicator Bar
            VirtualPhoneHomeBar(
                canGoBack = false,
                onGoHome = {}
            )
        }
    }
}

private fun handleAppNavigation(
    appId: String,
    onNavigateToChat: () -> Unit,
    onNavigateToMoments: () -> Unit,
    onNavigateToLiving: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onNavigateToApps: () -> Unit,
    onAppClick: (String) -> Unit
) {
    when (appId) {
        "chat" -> onNavigateToChat()
        "moments" -> onNavigateToMoments()
        "living" -> onNavigateToLiving()
        "memories" -> onNavigateToMemories()
        "apps" -> onNavigateToApps()
        else -> onAppClick(appId)
    }
}

private data class HomeAppDef(
    val id: String,
    val name: String,
    val iconKey: String,
    val badge: String?
)
