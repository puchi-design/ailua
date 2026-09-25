package com.example.ui.home

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.AiluaAvatar
import com.example.ui.components.AppIconItem
import com.example.ui.components.BondProgressWidget
import com.example.ui.components.LivingCharacterWidget
import com.example.ui.components.MemorySnippetWidget
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.theme.AiluaDustyRose
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold
import com.example.ui.theme.AiluaMutedLavender

@Composable
fun VirtualHomeScreen(
    character: CharacterProfile = MockData.sampleCharacter,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToGroupChat: () -> Unit = {},
    onNavigateToContacts: () -> Unit = {},
    onNavigateToRelations: () -> Unit = {},
    onNavigateToCheckPhone: () -> Unit = {},
    onNavigateToDiary: () -> Unit = {},
    onNavigateToMoments: () -> Unit = {},
    onNavigateToLiving: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    onNavigateToApps: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onAppClick: (String) -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    // 10 Desktop Apps on Page 1
    val homeApps = listOf(
        HomeAppDef("messages", "通讯", "chat", "2"),
        HomeAppDef("moments", "瞬间", "moments", "New"),
        HomeAppDef("living", "生活", "living", null),
        HomeAppDef("contacts", "联系人", "contacts", null),
        HomeAppDef("check_phone", "窥屏", "check_phone", "HOT"),
        HomeAppDef("memories", "记忆", "memories", null),
        HomeAppDef("relations", "关系谱", "relations", null),
        HomeAppDef("diary", "心声日记", "diary", null),
        HomeAppDef("theater", "沉浸剧场", "theater", "HOT"),
        HomeAppDef("apps", "应用库", "apps", null)
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

            // Paged Workspace (ARK Launcher Reference: multi-page workspace)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> PageMainHome(
                        character = character,
                        homeApps = homeApps,
                        onNavigateToMessages = onNavigateToMessages,
                        onNavigateToChat = onNavigateToChat,
                        onNavigateToMoments = onNavigateToMoments,
                        onNavigateToLiving = onNavigateToLiving,
                        onNavigateToContacts = onNavigateToContacts,
                        onNavigateToCheckPhone = onNavigateToCheckPhone,
                        onNavigateToDiary = onNavigateToDiary,
                        onNavigateToMemories = onNavigateToMemories,
                        onNavigateToRelations = onNavigateToRelations,
                        onNavigateToApps = onNavigateToApps,
                        onOpenProfile = onOpenProfile,
                        onAppClick = onAppClick
                    )
                    1 -> PageLifeBento(
                        onNavigateToGroupChat = onNavigateToGroupChat,
                        onNavigateToCheckPhone = onNavigateToCheckPhone,
                        onNavigateToDiary = onNavigateToDiary,
                        onNavigateToRelations = onNavigateToRelations,
                        onNavigateToLiving = onNavigateToLiving,
                        onAppClick = onAppClick
                    )
                }
            }

            // Pager Indicator Dots (ARK Launcher Smartspace/Workspace indicator)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(2) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 18.dp else 6.dp,
                        label = "dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isSelected) AiluaMoonGold
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            // Persistent Virtual Phone Dock (ARK Launcher Persistent Dock Pattern)
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
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("virtual_phone_dock")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconItem(
                            name = "消息",
                            iconKey = "chat",
                            badge = "2",
                            showLabel = false,
                            onClick = onNavigateToMessages
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
                            name = "联系人",
                            iconKey = "contacts",
                            showLabel = false,
                            onClick = onNavigateToContacts
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

@Composable
private fun PageMainHome(
    character: CharacterProfile,
    homeApps: List<HomeAppDef>,
    onNavigateToMessages: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToMoments: () -> Unit,
    onNavigateToLiving: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToCheckPhone: () -> Unit,
    onNavigateToDiary: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onNavigateToRelations: () -> Unit,
    onNavigateToApps: () -> Unit,
    onOpenProfile: () -> Unit,
    onAppClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
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

        Spacer(modifier = Modifier.height(18.dp))

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
                    text = "桌面应用",
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
                    text = "应用库 (17)",
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
                            dispatchAppAction(
                                app.id,
                                onNavigateToMessages,
                                onNavigateToMoments,
                                onNavigateToLiving,
                                onNavigateToContacts,
                                onNavigateToCheckPhone,
                                onNavigateToDiary,
                                onNavigateToMemories,
                                onNavigateToRelations,
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
                            dispatchAppAction(
                                app.id,
                                onNavigateToMessages,
                                onNavigateToMoments,
                                onNavigateToLiving,
                                onNavigateToContacts,
                                onNavigateToCheckPhone,
                                onNavigateToDiary,
                                onNavigateToMemories,
                                onNavigateToRelations,
                                onNavigateToApps,
                                onAppClick
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PageLifeBento(
    onNavigateToGroupChat: () -> Unit,
    onNavigateToCheckPhone: () -> Unit,
    onNavigateToDiary: () -> Unit,
    onNavigateToRelations: () -> Unit,
    onNavigateToLiving: () -> Unit,
    onAppClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val pulseEvents = MockData.unifiedLifeEvents.take(4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Bento Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "生命视界 · Life Bento",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "心网伴生世界实时脉搏",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AiluaMoonGold.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AiluaMoonGold)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "3位角色在线",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = AiluaMoonGold
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Widget 1: Real-time Life Pulse Stream (SillyTavern-GroupWorld Life Events integration)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(18.dp)
                )
                .clickable { onNavigateToLiving() }
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AiluaMoonGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "心网生活脉搏 · Life Pulse",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "实时更新",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = AiluaMistBlue
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                pulseEvents.forEachIndexed { index, event ->
                    val charName = when (event.characterId) {
                        "yuna" -> "悠奈"
                        "noa" -> "诺亚"
                        else -> "小弥"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AiluaAvatar(
                            avatarId = event.characterId,
                            size = 28.dp,
                            showHalo = false
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = charName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = event.time,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            Text(
                                text = event.description,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                maxLines = 1
                            )
                        }
                        event.location?.let { loc ->
                            Text(
                                text = loc,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = AiluaMutedLavender
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AiluaMutedLavender.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (index < pulseEvents.size - 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dual Bento Cards Row: Check Phone & Group Chat
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Left: Check Phone (窥屏助手)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                AiluaMoonGold.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onNavigateToCheckPhone() }
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = AiluaMoonGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AiluaMoonGold)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "窥屏",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "小弥的虚拟手机",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "正在听: 夜航星尘\n未发送草稿: 1 条",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            // Card Right: Tea Party Multi-character Group Chat
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                AiluaMutedLavender.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onNavigateToGroupChat() }
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = AiluaMutedLavender,
                            modifier = Modifier.size(20.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AiluaMutedLavender)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "多角色群",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "星尘茶会群聊",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "小弥 · 悠奈 · 诺亚\nAI自主演绎进行中",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Widget 2.5: Secret Diary Preview Card (Native Diary Screen Entry)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            AiluaDustyRose.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(18.dp)
                )
                .clickable { onNavigateToDiary() }
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = AiluaDustyRose,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "心声日记 · Secret Diary",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "9月25日",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = AiluaMistBlue
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "《风吹进来的时候》",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "“整理书页的时候，突然想起你昨天说，一忙起来就总忘记喝水。不知道你今天有没有记得…”",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Widget 3: Social Relations Graph Banner (SillyTavern-GroupWorld inspired)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(18.dp)
                )
                .clickable { onNavigateToRelations() }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AiluaDustyRose.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = AiluaDustyRose,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "角色关系网络 · Social Graph",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "小弥 ↔ 尤娜 ↔ 诺亚 情感羁绊共鸣",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Widget 4: Pass 2 World & Narrative Hub (Virtual Places & Lorebook)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left: Virtual Map
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                AiluaMistBlue.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onAppClick("world_map") }
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = AiluaMistBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AiluaMistBlue.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "6地点",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = AiluaMistBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "虚拟世界地图",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "青石街23号 · 木兰茶馆\n月光书阁 · 实时驻留",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            // Right: Lore Book
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                AiluaMoonGold.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onAppClick("lore_books") }
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = AiluaMoonGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AiluaMoonGold.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "8设定",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = AiluaMoonGold,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "世界设定秘典",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "地点 · 习惯 · 共同记忆\n动态词条激活引擎",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun dispatchAppAction(
    appId: String,
    onNavigateToMessages: () -> Unit,
    onNavigateToMoments: () -> Unit,
    onNavigateToLiving: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToCheckPhone: () -> Unit,
    onNavigateToDiary: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onNavigateToRelations: () -> Unit,
    onNavigateToApps: () -> Unit,
    onAppClick: (String) -> Unit
) {
    when (appId) {
        "messages", "chat" -> onNavigateToMessages()
        "moments" -> onNavigateToMoments()
        "living" -> onNavigateToLiving()
        "contacts" -> onNavigateToContacts()
        "check_phone" -> onNavigateToCheckPhone()
        "diary" -> onNavigateToDiary()
        "memories" -> onNavigateToMemories()
        "relations" -> onNavigateToRelations()
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
