package com.example.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.local.AiluaLocalStore
import com.example.data.mock.MockData
import com.example.data.model.CharacterProfile
import com.example.data.model.LetterDeliveryState
import com.example.data.model.isRead
import com.example.data.repository.MailboxRepository
import com.example.ui.components.AiluaAvatar
import com.example.ui.components.AppIconItem
import com.example.ui.components.HomeThemeCatalog
import com.example.ui.components.HomeThemeStore
import com.example.ui.components.ThemePickerSheet
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.components.WorldTimeDevSheet
import com.example.ui.components.themeWallpaper
import com.example.ui.theme.AiluaDustyRose
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold
import com.example.ui.theme.AiluaMutedLavender
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@OptIn(ExperimentalMaterial3Api::class)
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
    onNavigateToMailbox: () -> Unit = {},
    onNavigateToCall: () -> Unit = {},
    onNavigateToCallHistory: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onAppClick: (String) -> Unit = {},
    initialEditing: Boolean = false
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val worldClock by WorldHeartbeatEngine.worldClock.collectAsStateWithLifecycle()
    val heartbeatState by WorldHeartbeatEngine.heartbeatState.collectAsStateWithLifecycle()
    val letters by MailboxRepository.letters.collectAsStateWithLifecycle()
    val unreadLettersCount = letters.count { it.deliveryState == LetterDeliveryState.DELIVERED && !it.isRead }
    var showDevTimeSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    val homeTheme = HomeThemeCatalog.byId(HomeThemeStore.selectedId)

    // Edit mode: entered by long pressing the wallpaper or an app icon,
    // left by tapping blank space, the [完成] pill, the Home bar or Back.
    var isEditing by remember { mutableStateOf(initialEditing) }
    var isGridDragging by remember { mutableStateOf(false) }

    // Desktop grid only holds apps that are not already pinned in the dock
    val defaultHomeApps = listOf(
        HomeAppDef("mailbox", "信箱", "mailbox", if (unreadLettersCount > 0) "$unreadLettersCount" else null),
        HomeAppDef("gallery", "相册", "gallery", null),
        HomeAppDef("check_phone", "窥屏", "check_phone", null),
        HomeAppDef("memories", "记忆", "memories", null),
        HomeAppDef("relations", "关系谱", "relations", null),
        HomeAppDef("diary", "心声日记", "diary", null),
        HomeAppDef("theater", "沉浸剧场", "theater", null),
        HomeAppDef("call_history", "通话记录", "call", null)
    )

    // Saved order wins, unknown ids drop, brand new apps append at the end
    var homeAppOrder by remember {
        mutableStateOf(
            HomeAppOrder.normalize(
                savedIds = AiluaLocalStore.getHomeAppOrder(),
                availableIds = defaultHomeApps.map { it.id }
            )
        )
    }
    val homeApps = homeAppOrder.mapNotNull { id -> defaultHomeApps.firstOrNull { it.id == id } }
    val persistHomeAppOrder = { AiluaLocalStore.saveHomeAppOrder(homeAppOrder) }

    // Wallpaper: default theme tracks the AILUA world clock, the other themes stay fixed
    val wallpaper = themeWallpaper(
        theme = homeTheme,
        dayPhase = worldClock.dayPhase,
        weather = worldClock.weather,
        isDarkTheme = isDarkTheme
    )

    // Long press the wallpaper enters edit mode, tapping blank space leaves it
    BackHandler(enabled = isEditing) {
        persistHomeAppOrder()
        isEditing = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(wallpaper.colors))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (isEditing) {
                            persistHomeAppOrder()
                            isEditing = false
                        }
                    },
                    onLongPress = { isEditing = true }
                )
            }
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
                userScrollEnabled = !isGridDragging,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> PageMainHome(
                        character = character,
                        homeApps = homeApps,
                        worldClock = worldClock,
                        heartbeatState = heartbeatState,
                        accent = homeTheme.accent,
                        isEditing = isEditing,
                        onEnterEdit = { isEditing = true },
                        onOrderMove = { fromIndex, toIndex ->
                            homeAppOrder = HomeAppOrder.move(homeAppOrder, fromIndex, toIndex)
                            persistHomeAppOrder()
                        },
                        onDragStateChange = { isGridDragging = it },
                        onOpenDevTime = { showDevTimeSheet = true },
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

            // Pager Indicator Dots + lightweight edit control strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (isEditing) {
                        HomeEditPill(
                            label = "完成",
                            testTag = "home_edit_done",
                            accent = homeTheme.accent
                        ) {
                            persistHomeAppOrder()
                            isEditing = false
                        }
                    }
                }
                Row(
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
                                    if (isSelected) homeTheme.accent
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                )
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    if (isEditing) {
                        HomeEditPill(
                            label = "主题",
                            testTag = "home_edit_theme",
                            accent = homeTheme.accent
                        ) {
                            showThemeSheet = true
                        }
                    }
                }
            }

            // Persistent Virtual Phone Dock (system launcher style, translucent + theme tinted)
            HomeDock(
                accent = homeTheme.accent,
                onNavigateToMessages = onNavigateToMessages,
                onNavigateToMoments = onNavigateToMoments,
                onNavigateToLiving = onNavigateToLiving,
                onNavigateToContacts = onNavigateToContacts,
                onNavigateToApps = onNavigateToApps
            )

            // Virtual Home Indicator Bar
            VirtualPhoneHomeBar(
                canGoBack = false,
                onGoHome = {
                    if (isEditing) {
                        persistHomeAppOrder()
                        isEditing = false
                    }
                }
            )
        }

        if (showDevTimeSheet) {
            WorldTimeDevSheet(onDismiss = { showDevTimeSheet = false })
        }

        if (showThemeSheet) {
            ThemePickerSheet(
                selectedId = homeTheme.id,
                onSelect = { HomeThemeStore.selectedId = it },
                onDismiss = { showThemeSheet = false }
            )
        }
    }
}

@Composable
private fun PageMainHome(
    character: CharacterProfile,
    homeApps: List<HomeAppDef>,
    worldClock: com.example.data.model.WorldClock,
    heartbeatState: com.example.data.engine.WorldHeartbeatState,
    accent: Color,
    isEditing: Boolean,
    onEnterEdit: () -> Unit,
    onOrderMove: (Int, Int) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    onOpenDevTime: () -> Unit,
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
    val widgetContext = WidgetHostContext(
        character = character,
        accent = accent,
        worldClock = worldClock,
        heartbeatState = heartbeatState,
        onOpenDevTime = onOpenDevTime,
        onOpenLiving = onNavigateToLiving,
        onOpenChat = onNavigateToChat,
        onOpenProfile = onOpenProfile,
        onNavigateToMemories = onNavigateToMemories
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        // Desktop widgets are rendered through the lightweight widget host
        AiluaWidgetHost(
            widgetIds = WidgetRegistry.defaultOrder.take(2),
            context = widgetContext,
            spacing = 18.dp
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Launcher icon grid fills the rest of the desktop page (scrolls itself)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AppIconGrid(
                apps = homeApps,
                isEditing = isEditing,
                onEnterEdit = onEnterEdit,
                onOrderMove = onOrderMove,
                onDragStateChange = onDragStateChange,
                onAppClick = { app ->
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

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom desktop widgets: Memory Echo + Bond (same registry, real state)
        AiluaWidgetHost(
            widgetIds = WidgetRegistry.defaultOrder.drop(2),
            context = widgetContext,
            layout = WidgetHostLayout.Row,
            spacing = 12.dp
        )
    }
}

@Composable
internal fun DesktopWorldClock(
    worldClock: com.example.data.model.WorldClock,
    heartbeatState: com.example.data.engine.WorldHeartbeatState,
    accent: Color,
    onOpenDevTime: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDevTime() }
            .testTag("desktop_world_clock"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = worldClock.timeFormatted,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.18f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${worldClock.dayPhase.label} · ${worldClock.weather.label}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = worldClock.dateLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                )
                Text(
                    text = heartbeatState.currentPhase.atmosphere,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(AiluaMistBlue.copy(alpha = 0.16f))
                .clickable { onOpenDevTime() }
                .padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "跃迁",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = AiluaMistBlue
            )
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "时间跃迁",
                tint = AiluaMistBlue,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
internal fun LivingPresenceStrip(
    character: CharacterProfile,
    onOpenChat: () -> Unit,
    onOpenLiving: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenLiving() }
            .testTag("living_character_widget"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AiluaAvatar(
            size = 58.dp,
            showHalo = true,
            showLivingStatus = true,
            onClick = onOpenProfile
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "· ${character.englishName}",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = character.mood,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6EC6A1))
                    )
                    Text(
                        text = character.currentActivity,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = character.location,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "“${character.contextualQuote}”",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                .clickable { onOpenChat() }
                .testTag("home_quick_chat"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = "找她聊天",
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AppIconGrid(
    apps: List<HomeAppDef>,
    isEditing: Boolean,
    onEnterEdit: () -> Unit,
    onOrderMove: (Int, Int) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    onAppClick: (HomeAppDef) -> Unit
) {
    val lazyGridState = rememberLazyGridState()
    val reorderableLazyGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
        onOrderMove(from.index, to.index)
    }

    LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_app_grid"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(bottom = 10.dp)
    ) {
        items(apps, key = { it.id }) { app ->
            ReorderableItem(reorderableLazyGridState, key = app.id) { isDraggingItem ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(app.id) {
                            detectTapGestures(
                                onLongPress = { if (!isEditing) onEnterEdit() }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AppIconItem(
                        name = app.name,
                        iconKey = app.iconKey,
                        badge = app.badge,
                        editMode = isEditing,
                        isDragging = isDraggingItem,
                        onClick = if (isEditing) ({}) else ({ onAppClick(app) }),
                        modifier = if (isEditing) {
                            Modifier.draggableHandle(
                                onDragStarted = { onDragStateChange(true) },
                                onDragStopped = { onDragStateChange(false) }
                            )
                        } else {
                            Modifier
                        }
                    )
                }
            }
        }
    }
}

/** Lightweight pill used for the edit mode control strip: [完成] / [主题]. */
@Composable
private fun HomeEditPill(
    label: String,
    testTag: String,
    accent: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.40f), shape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = accent
        )
    }
}

/**
 * Bottom dock: translucent surface derived from the active [HomeTheme] so it
 * follows wallpaper and theme switches without a per-theme copy.
 * Badges are intentionally omitted — the dock only shows real state.
 */
@Composable
private fun HomeDock(
    accent: Color,
    onNavigateToMessages: () -> Unit,
    onNavigateToMoments: () -> Unit,
    onNavigateToLiving: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToApps: () -> Unit
) {
    val dockShape = RoundedCornerShape(26.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 3.dp,
                    shape = dockShape,
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.16f)
                )
                .clip(dockShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f))
                .background(accent.copy(alpha = 0.10f))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                )
                .border(
                    1.dp,
                    accent.copy(alpha = 0.30f),
                    dockShape
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("virtual_phone_dock"),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconItem(
                name = "消息",
                iconKey = "chat",
                showLabel = false,
                onClick = onNavigateToMessages
            )
            AppIconItem(
                name = "动态",
                iconKey = "moments",
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
    val allLifeEvents by WorldStateRepository.events.collectAsStateWithLifecycle()
    val pulseEvents = allLifeEvents.take(4)

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
