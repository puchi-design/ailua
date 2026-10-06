package com.example.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.context.CharacterContext
import com.example.data.firstsession.FirstSessionStore
import com.example.data.projection.projectPresence
import com.example.data.model.isUserActivity
import com.example.data.model.sortedChronologically
import com.example.data.engine.WorldStateRepository
import com.example.data.desktop.DesktopItem
import com.example.data.desktop.DesktopContainer
import com.example.data.desktop.DesktopPage
import com.example.data.desktop.DesktopItemType
import com.example.data.desktop.DesktopPlacement
import com.example.data.desktop.WidgetPlacement
import com.example.data.desktop.CellRect
import com.example.ui.design.launcher.WorkspaceAppLabel
import com.example.ui.launcher.LauncherAppCatalog
import com.example.navigation.AppRouter
import com.example.navigation.AiluaDestinations
import com.example.ui.design.launcher.WorkspaceViewModel
import com.example.ui.design.launcher.DragLayer
import com.example.ui.design.launcher.layout.DragDirection
import com.example.ui.design.launcher.layout.LayoutSolution
import com.example.ui.home.workspace.DropPlan
import com.example.ui.home.workspace.DropResolver
import com.example.ui.home.workspace.EdgePageAction
import com.example.ui.home.workspace.EdgePageController
import com.example.ui.home.workspace.WorkspaceDragPhase
import com.example.ui.home.workspace.WorkspaceDragState
import com.example.ui.home.workspace.WorkspacePage
import com.example.ui.home.workspace.WorkspacePageGrid
import com.example.ui.home.workspace.FolderDropIntent
import com.example.ui.home.workspace.FolderDropResolver
import com.example.ui.home.hotseat.HomeHotseat
import com.example.ui.home.folder.FolderHoverController
import com.example.ui.home.folder.FolderOverlay
import com.example.ui.home.folder.FolderNameSuggester
import com.example.ui.home.folder.WorkspaceFolderItem
import com.example.ui.home.edit.HomeEditPanel
import com.example.ui.home.edit.HomeDisplayPreferencesStore
import com.example.ui.home.edit.HomeDisplaySettingsSheet
import com.example.ui.home.page.PageManagerSheet
import com.example.ui.home.special.LifeBentoPage
import com.example.ui.home.widget.WidgetPickerSheet
import com.example.ui.home.widget.WorkspaceWidgetItem
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.data.mock.MockData
import com.example.data.model.CharacterProfile
import com.example.data.model.LetterDeliveryState
import com.example.data.model.isRead
import com.example.data.repository.MailboxRepository
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.components.WorldTimeDevSheet
import com.example.ui.themeengine.LocalAiluaTheme
import com.example.ui.themeengine.rememberThemeWallpaperBitmap
import com.example.ui.themeengine.ThemeWallpaper
import com.example.ui.themeengine.material.AiluaBackdropProvider
import com.example.ui.themeengine.material.ailuaBackdropSource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.example.ui.themecenter.ThemeCenterSheet
import com.example.ui.themecenter.ThemeCenterSection
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

private const val LIFE_BENTO_PAGE_ID = "life_bento"

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
    onNavigateToApps: (String?) -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onNavigateToMailbox: () -> Unit = {},
    onNavigateToCall: () -> Unit = {},
    onNavigateToCallHistory: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onAppClick: (String) -> Unit = {},
    initialEditing: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val workspaceViewModel: WorkspaceViewModel = viewModel(factory = WorkspaceViewModel.factory(context))
    val workspace by workspaceViewModel.workspace.collectAsStateWithLifecycle()
    val workspaceLoaded by workspaceViewModel.isLoaded.collectAsStateWithLifecycle()
    var temporaryPage by remember { mutableStateOf<DesktopPage?>(null) }
    val workspacePages = workspace.pages.sortedBy { it.rank } +
        listOfNotNull(temporaryPage?.takeUnless { transient -> workspace.pages.any { it.id == transient.id } })
    val pagerState = rememberPagerState(pageCount = { workspacePages.size + 1 })
    var initialHomeApplied by remember { mutableStateOf(false) }
    var currentPageId by remember { mutableStateOf<String?>(null) }
    val workspaceError by workspaceViewModel.error.collectAsStateWithLifecycle()
    val worldClock by WorldHeartbeatEngine.worldClock.collectAsStateWithLifecycle()
    val heartbeatState by WorldHeartbeatEngine.heartbeatState.collectAsStateWithLifecycle()
    val firstSession by FirstSessionStore.state.collectAsStateWithLifecycle()
    val letters by MailboxRepository.letters.collectAsStateWithLifecycle()
    val unreadLettersCount = letters.count { it.deliveryState == LetterDeliveryState.DELIVERED && !it.isRead }
    var showDevTimeSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showCharacterSwitcher by remember { mutableStateOf(false) }
    var themeInitialSection by remember { mutableStateOf(ThemeCenterSection.THEMES) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var showPageManager by remember { mutableStateOf(false) }
    var showDisplaySettings by remember { mutableStateOf(false) }
    var widgetMessage by remember { mutableStateOf<String?>(null) }
    var selectedWidgetId by remember { mutableStateOf<String?>(null) }
    var resizeItemId by remember { mutableStateOf<String?>(null) }
    var resizeSize by remember { mutableStateOf<WidgetSize?>(null) }
    var resizePlan by remember { mutableStateOf<DropPlan?>(null) }
    remember(context) {
        HomeDisplayPreferencesStore.initialize(context)
        true
    }
    val homeDisplayPreferences = HomeDisplayPreferencesStore.current
    val themeRuntime = LocalAiluaTheme.current

    // Edit mode: entered by long pressing the wallpaper or an app icon,
    // left by tapping blank space, the [完成] pill, the Home bar or Back.
    var isEditing by remember { mutableStateOf(initialEditing) }
    var dragState by remember { mutableStateOf(WorkspaceDragState()) }
    var dropPlan by remember { mutableStateOf<DropPlan?>(null) }
    var folderCandidate by remember { mutableStateOf<FolderDropIntent>(FolderDropIntent.None) }
    var activeFolderIntent by remember { mutableStateOf<FolderDropIntent>(FolderDropIntent.None) }
    var openFolderId by remember { mutableStateOf<String?>(null) }
    val gridBounds = remember { mutableStateMapOf<String, Rect>() }
    val gridRows = remember { mutableStateMapOf<String, Int>() }
    var hotseatBounds by remember { mutableStateOf<Rect?>(null) }
    var pagerBounds by remember { mutableStateOf<Rect?>(null) }
    var rootWidth by remember { mutableIntStateOf(0) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var edgeSwitching by remember { mutableStateOf(false) }
    var edgeJob by remember { mutableStateOf<Job?>(null) }

    val labels = (LauncherAppCatalog.all().map { it.id } + workspace.items.map { it.sourceId })
        .distinct().mapNotNull { id ->
            LauncherAppCatalog.get(id)?.let { entry ->
                id to WorkspaceAppLabel(
                    LauncherAppCatalog.label(id), entry.iconKey,
                    if (id == "mailbox" && unreadLettersCount > 0) "$unreadLettersCount" else null,
                )
            }
        }.toMap()
    val latestWorkspace by rememberUpdatedState(workspace)
    val latestPages by rememberUpdatedState(workspacePages)
    val latestDrag by rememberUpdatedState(dragState)
    val latestDropPlan by rememberUpdatedState(dropPlan)
    val latestFolderIntent by rememberUpdatedState(activeFolderIntent)

    LaunchedEffect(pagerState.settledPage) {
        if (initialHomeApplied) currentPageId = workspacePages.getOrNull(pagerState.settledPage)?.id
            ?: LIFE_BENTO_PAGE_ID
    }
    LaunchedEffect(workspaceLoaded, workspacePages.map { it.id }, currentPageId,
        dragState.isDragging) {
        if (!workspaceLoaded || dragState.isDragging) return@LaunchedEffect
        if (!initialHomeApplied) {
            val homePage = workspacePages.firstOrNull { it.isHome } ?: workspacePages.firstOrNull()
            currentPageId = homePage?.id
            val homeIndex = workspacePages.indexOfFirst { it.id == homePage?.id }
            if (homeIndex >= 0) pagerState.scrollToPage(homeIndex)
            initialHomeApplied = true
        } else {
            val selected = currentPageId?.takeIf { id ->
                id == LIFE_BENTO_PAGE_ID || workspacePages.any { it.id == id }
            }
                ?: workspacePages.firstOrNull { it.isHome }?.id
                ?: workspacePages.firstOrNull()?.id
            currentPageId = selected
            val newIndex = if (selected == LIFE_BENTO_PAGE_ID) workspacePages.size
                else workspacePages.indexOfFirst { it.id == selected }
            if (newIndex >= 0 && pagerState.currentPage != newIndex) pagerState.scrollToPage(newIndex)
        }
    }

    val widgetContext = WidgetHostContext(
        character = character, accent = themeRuntime.palette.accent, worldClock = worldClock,
        heartbeatState = heartbeatState,
        onOpenDevTime = if (isEditing) ({}) else ({ showDevTimeSheet = true }),
        onOpenLiving = if (isEditing) ({}) else onNavigateToLiving,
        onOpenChat = if (isEditing) ({}) else onNavigateToChat,
        onOpenProfile = if (isEditing) ({}) else onOpenProfile,
        onSwitchCharacter = if (isEditing) ({}) else ({ showCharacterSwitcher = true }),
        onNavigateToMemories = if (isEditing) ({}) else onNavigateToMemories,
    )

    fun previewResize(itemId: String, size: WidgetSize) {
        val item = workspace.items.firstOrNull { it.id == itemId } ?: return
        if (size.spanX == item.spanX && size.spanY == item.spanY) {
            resizeItemId = null
            resizeSize = null
            resizePlan = null
            return
        }
        resizeItemId = itemId
        resizeSize = size
        resizePlan = DropResolver.resolveResize(workspace, item, size.spanX, size.spanY)
    }

    fun commitResize(itemId: String, size: WidgetSize) {
        val item = workspace.items.firstOrNull { it.id == itemId } ?: return
        val plan = DropResolver.resolveResize(workspace, item, size.spanX, size.spanY)
        resizeItemId = null
        resizeSize = null
        resizePlan = null
        if (plan is DropPlan.Accept && plan.commit.placements.isNotEmpty()) {
            scope.launch { workspaceViewModel.applyDrop(plan.commit) }
        } else if (plan is DropPlan.Reject) widgetMessage = "当前位置放不下这个尺寸"
    }

    fun localBounds(rect: Rect): Rect = Rect(
        rect.left - rootOrigin.x, rect.top - rootOrigin.y,
        rect.right - rootOrigin.x, rect.bottom - rootOrigin.y,
    )

    suspend fun finishDrag(restoreSourcePage: Boolean) {
        edgeJob?.cancel()
        edgeJob = null
        edgeSwitching = false
        val sourcePageId = dragState.sourcePageId
        if (restoreSourcePage && sourcePageId != null) {
            val sourceIndex = latestPages.indexOfFirst { it.id == sourcePageId }
            if (sourceIndex >= 0) pagerState.scrollToPage(sourceIndex)
        }
        temporaryPage = null
        dropPlan = null
        folderCandidate = FolderDropIntent.None
        activeFolderIntent = FolderDropIntent.None
        dragState = WorkspaceDragState()
    }

    val updateHover: (Offset, DragDirection) -> Unit = { point, direction ->
        val item = workspace.items.firstOrNull { it.id == dragState.draggedItemId }
        val page = workspacePages.getOrNull(pagerState.currentPage)
        val dockRect = hotseatBounds?.let(::localBounds)
        val pageRect = page?.let { gridBounds[it.id] }?.let(::localBounds)
        val target = when {
            item == null -> null
            dockRect?.contains(point) == true -> {
                val slot = ((point.x - dockRect.left) / (dockRect.width / 5f)).toInt().coerceIn(0, 4)
                Triple(DesktopContainer.HOTSEAT, null, CellRect(slot, 0))
            }
            page != null && pageRect?.contains(point) == true &&
                pagerBounds?.let(::localBounds)?.contains(point) == true -> {
                val rows = gridRows[page.id] ?: 6
                val anchor = if (item.type == DesktopItemType.AILUA_WIDGET)
                    point - dragState.grabOffset else point
                val x = ((anchor.x - pageRect.left) / (pageRect.width / 4f)).toInt()
                    .coerceIn(0, 4 - item.spanX)
                val y = ((anchor.y - pageRect.top) / (pageRect.height / rows.toFloat())).toInt()
                    .coerceIn(0, rows - item.spanY)
                Triple(DesktopContainer.WORKSPACE, page.id, CellRect(x, y, item.spanX, item.spanY))
            }
            else -> null
        }
        val candidate = if (item != null && target != null) {
            val rect = if (target.first == DesktopContainer.HOTSEAT) dockRect else pageRect
            val columns = if (target.first == DesktopContainer.HOTSEAT) 5 else 4
            val rows = if (target.first == DesktopContainer.HOTSEAT) 1 else page?.let { gridRows[it.id] } ?: 6
            if (rect != null && FolderHoverController.isCentered(point, rect, target.third, columns, rows)) {
                FolderDropResolver.resolve(workspace, item, target.first, target.second, target.third)
            } else FolderDropIntent.None
        } else FolderDropIntent.None
        if (candidate != folderCandidate) {
            folderCandidate = candidate
            activeFolderIntent = FolderDropIntent.None
        }
        dropPlan = if (item != null && target != null) DropResolver.resolveDrop(
            snapshot = workspace.copy(pages = workspacePages), item = item,
            targetContainer = target.first, targetPageId = target.second, targetCell = target.third,
            direction = direction, temporaryPage = temporaryPage,
        ) else null
        dragState = dragState.copy(
            currentPageId = page?.id, targetContainer = target?.first, hoverCell = target?.third,
            pointerPosition = point, direction = direction,
        )
    }
    val latestUpdateHover by rememberUpdatedState(updateHover)

    LaunchedEffect(folderCandidate, dragState.draggedItemId) {
        if (folderCandidate == FolderDropIntent.None || !dragState.isDragging) return@LaunchedEffect
        delay(FolderHoverController.DWELL_MS)
        if (dragState.isDragging && folderCandidate != FolderDropIntent.None) {
            activeFolderIntent = folderCandidate
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val pagerRect = pagerBounds?.let(::localBounds)
    val edgeAction = if (dragState.phase == WorkspaceDragPhase.DRAGGING &&
        pagerRect?.contains(dragState.pointerPosition) == true) {
        EdgePageController.action(
            pointerX = dragState.pointerPosition.x, viewportWidth = rootWidth.toFloat(),
            currentPage = pagerState.currentPage, workspacePageCount = workspacePages.size,
            thresholdPx = with(density) { 32.dp.toPx() }, allowCreate = temporaryPage == null,
        )
    } else EdgePageAction.NONE

    LaunchedEffect(edgeAction, pagerState.currentPage, edgeSwitching) {
        if (edgeAction == EdgePageAction.NONE || edgeSwitching) return@LaunchedEffect
        delay(EdgePageController.DWELL_MS)
        if (!dragState.isDragging || dragState.phase == WorkspaceDragPhase.DROPPING) return@LaunchedEffect
        edgeSwitching = true
        dragState = dragState.copy(phase = WorkspaceDragPhase.EDGE_DWELL,
            targetContainer = null, hoverCell = null)
        dropPlan = null
        folderCandidate = FolderDropIntent.None
        activeFolderIntent = FolderDropIntent.None
        val from = pagerState.currentPage
        edgeJob = scope.launch {
            try {
                val destination = when (edgeAction) {
                    EdgePageAction.PREVIOUS -> from - 1
                    EdgePageAction.NEXT -> from + 1
                    EdgePageAction.CREATE_PAGE -> {
                        val oldCount = pagerState.pageCount
                        temporaryPage = DesktopPage(UUID.randomUUID().toString(), workspacePages.size, false)
                        snapshotFlow { pagerState.pageCount }.first { it > oldCount }
                        from + 1
                    }
                    EdgePageAction.NONE -> from
                }
                pagerState.animateScrollToPage(destination)
                if (dragState.isDragging && dragState.phase != WorkspaceDragPhase.DROPPING) {
                    dragState = dragState.copy(phase = WorkspaceDragPhase.DRAGGING,
                        currentPageId = latestPages.getOrNull(destination)?.id)
                    latestUpdateHover(dragState.pointerPosition, dragState.direction)
                }
            } finally {
                edgeSwitching = false
                edgeJob = null
            }
        }
    }

    LaunchedEffect(pagerState.settledPage, workspacePages.size,
        gridBounds[workspacePages.getOrNull(pagerState.settledPage)?.id], dragState.isDragging) {
        if (dragState.isDragging && dragState.phase != WorkspaceDragPhase.DROPPING) {
            delay(16)
            latestUpdateHover(latestDrag.pointerPosition, latestDrag.direction)
        }
    }

    // Long press the wallpaper enters edit mode, tapping blank space leaves it
    BackHandler(enabled = isEditing || dragState.isDragging) {
        if (dragState.isDragging) scope.launch { finishDrag(restoreSourcePage = true) }
        else isEditing = false
    }

    AiluaBackdropProvider {
        Box(
            modifier = Modifier
                .fillMaxSize()
            .onGloballyPositioned {
                rootOrigin = it.positionInRoot()
                rootWidth = it.size.width
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (isEditing) {
                            isEditing = false
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { start ->
                        val selectedPage = latestPages.getOrNull(pagerState.currentPage)
                        var item: DesktopItem? = null
                        var cellWidth = 0f
                        var cellHeight = 0f
                        val dock = hotseatBounds?.let(::localBounds)
                        val pageGrid = selectedPage?.let { gridBounds[it.id] }?.let(::localBounds)
                        if (dock?.contains(start) == true) {
                            cellWidth = dock.width / 5f
                            cellHeight = dock.height
                            val slot = ((start.x - dock.left) / cellWidth).toInt().coerceIn(0, 4)
                            item = latestWorkspace.hotseatItems().firstOrNull { it.cellX == slot }
                        } else if (selectedPage != null && pageGrid?.contains(start) == true &&
                            pagerBounds?.let(::localBounds)?.contains(start) == true) {
                            val rows = gridRows[selectedPage.id] ?: 6
                            cellWidth = pageGrid.width / 4f
                            cellHeight = pageGrid.height / rows.toFloat()
                            val x = ((start.x - pageGrid.left) / cellWidth).toInt().coerceIn(0, 3)
                            val y = ((start.y - pageGrid.top) / cellHeight).toInt().coerceIn(0, rows - 1)
                            item = latestWorkspace.itemsFor(selectedPage.id).firstOrNull {
                                x in it.cellX until it.cellX + it.spanX &&
                                    y in it.cellY until it.cellY + it.spanY
                            }
                        }
                        isEditing = true
                        selectedWidgetId = item?.takeIf { it.type == DesktopItemType.AILUA_WIDGET }?.id
                        if (item != null) {
                            val bounds = if (item.container == DesktopContainer.HOTSEAT) dock!! else pageGrid!!
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            dragState = WorkspaceDragState(
                                draggedItemId = item.id,
                                sourceContainer = item.container,
                                sourcePageId = item.pageId,
                                sourceCell = CellRect(item.cellX, item.cellY, item.spanX, item.spanY),
                                currentPageId = selectedPage?.id,
                                pointerPosition = start,
                                grabOffset = Offset(start.x - bounds.left - item.cellX * cellWidth,
                                    start.y - bounds.top - item.cellY * cellHeight),
                                previewWidth = (cellWidth * item.spanX).roundToInt(),
                                previewHeight = (cellHeight * item.spanY).roundToInt(),
                                phase = WorkspaceDragPhase.DRAGGING,
                            )
                            dropPlan = null
                            folderCandidate = FolderDropIntent.None
                            activeFolderIntent = FolderDropIntent.None
                        }
                    },
                    onDrag = { change, delta ->
                        if (latestDrag.isDragging && latestDrag.phase != WorkspaceDragPhase.DROPPING) {
                            change.consume()
                            val direction = if (abs(delta.x) >= abs(delta.y)) {
                                if (delta.x >= 0) DragDirection.RIGHT else DragDirection.LEFT
                            } else if (delta.y >= 0) DragDirection.DOWN else DragDirection.UP
                            latestUpdateHover(latestDrag.pointerPosition + delta, direction)
                        }
                    },
                    onDragEnd = {
                        val accepted = latestDropPlan as? DropPlan.Accept
                        val folderIntent = latestFolderIntent
                        if (latestDrag.isDragging && folderIntent != FolderDropIntent.None) {
                            dragState = latestDrag.copy(phase = WorkspaceDragPhase.DROPPING)
                            scope.launch {
                                val saved = when (folderIntent) {
                                    is FolderDropIntent.Create -> workspaceViewModel.createFolder(
                                        folderIntent.draggedAppId, folderIntent.targetAppId,
                                        FolderNameSuggester.suggest(
                                            latestWorkspace.items.firstOrNull { it.id == folderIntent.draggedAppId }?.sourceId,
                                            latestWorkspace.items.firstOrNull { it.id == folderIntent.targetAppId }?.sourceId,
                                        ),
                                    )
                                    is FolderDropIntent.Add -> workspaceViewModel.addItemToFolder(
                                        folderIntent.draggedAppId, folderIntent.folderId,
                                    )
                                    FolderDropIntent.None -> false
                                }
                                finishDrag(restoreSourcePage = !saved)
                            }
                        } else if (accepted == null || !latestDrag.isDragging) {
                            scope.launch { finishDrag(restoreSourcePage = true) }
                        } else if (accepted.commit.placements.isEmpty() && accepted.commit.newPage == null) {
                            scope.launch { finishDrag(restoreSourcePage = false) }
                        } else {
                            dragState = latestDrag.copy(phase = WorkspaceDragPhase.DROPPING)
                            scope.launch {
                                val saved = workspaceViewModel.applyDrop(accepted.commit)
                                finishDrag(restoreSourcePage = !saved)
                            }
                        }
                    },
                    onDragCancel = { scope.launch { finishDrag(restoreSourcePage = true) } },
                )
            }
            .testTag("virtual_home_screen")
    ) {
        ThemeWallpaper(Modifier.fillMaxSize().ailuaBackdropSource(), runtime = themeRuntime)
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding()
        ) {
            // Virtual OS Status Bar
            VirtualPhoneStatusBar(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )

            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = themeRuntime.layout.screenHorizontalPadding.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(24.dp))
                        .background(themeRuntime.widgets.backgroundColor.copy(alpha = 0.44f))
                        .clickable(enabled = !isEditing) { showCharacterSwitcher = true }
                        .testTag("home_character_switcher")
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CharacterPortrait(character.id, PortraitVariant.AVATAR, Modifier.size(24.dp))
                    Text(character.name, style = themeRuntime.text.caption,
                        color = themeRuntime.widgets.foregroundColor)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "切换当前角色",
                        modifier = Modifier.size(16.dp), tint = themeRuntime.widgets.foregroundColor)
                }
            }

            // Paged Workspace (ARK Launcher Reference: multi-page workspace)
            HorizontalPager(
                state = pagerState,
                key = { index -> workspacePages.getOrNull(index)?.id ?: LIFE_BENTO_PAGE_ID },
                userScrollEnabled = !dragState.isDragging,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { pagerBounds = it.boundsInRoot() }
            ) { page ->
                val workspacePage = workspacePages.getOrNull(page)
                if (workspacePage != null) {
                    val pageItems = workspace.itemsFor(workspacePage.id)
                    val hover = if (dragState.currentPageId == workspacePage.id &&
                        dragState.targetContainer == DesktopContainer.WORKSPACE &&
                        activeFolderIntent == FolderDropIntent.None) dragState.hoverCell else null
                    val preview = if (hover != null) (dropPlan as? DropPlan.Accept)?.preview else null
                    WorkspacePage(
                        items = pageItems, snapshot = workspace, labels = labels, isEditing = isEditing,
                        isDragging = dragState.isDragging, draggedItemId = dragState.draggedItemId,
                        preview = preview, hoverCell = hover, canDrop = dropPlan is DropPlan.Accept,
                        widgetContext = widgetContext,
                        selectedWidgetId = selectedWidgetId,
                        resizeOutline = if (resizeItemId != null &&
                            pageItems.any { it.id == resizeItemId }) {
                            val resized = pageItems.first { it.id == resizeItemId }
                            resizeSize?.let { CellRect(resized.cellX, resized.cellY, it.spanX, it.spanY) }
                        } else null,
                        resizeValid = resizePlan is DropPlan.Accept,
                        onWidgetSelect = { selectedWidgetId = it },
                        onWidgetDelete = { id ->
                            scope.launch {
                                if (workspaceViewModel.deleteWidget(id)) selectedWidgetId = null
                            }
                        },
                        onWidgetResizePreview = ::previewResize,
                        onWidgetResizeCommit = ::commitResize,
                        onBounds = { gridBounds[workspacePage.id] = it; gridRows[workspacePage.id] = 6 },
                        onAppClick = { id -> dispatchAppAction(
                            id, onNavigateToMessages, onNavigateToMoments, onNavigateToLiving,
                            onNavigateToContacts, onNavigateToCheckPhone, onNavigateToDiary,
                            onNavigateToMemories, onNavigateToRelations, onNavigateToApps,
                            workspacePages.getOrNull(pagerState.currentPage)?.id, onAppClick,
                        ) },
                        onFolderClick = { openFolderId = it },
                        folderHoverTargetId = FolderDropResolver.targetId(activeFolderIntent),
                        showLabels = homeDisplayPreferences.showLabels,
                        iconScale = homeDisplayPreferences.iconScale,
                    )
                } else {
                    LifeBentoPage(
                        onNavigateToGroupChat = onNavigateToGroupChat,
                        onNavigateToCheckPhone = onNavigateToCheckPhone,
                        onNavigateToDiary = onNavigateToDiary,
                        onNavigateToRelations = onNavigateToRelations,
                        onNavigateToLiving = onNavigateToLiving,
                        onSwitchCharacter = { if (!isEditing) showCharacterSwitcher = true },
                        onAppClick = onAppClick
                    )
                }
            }

            if (homeDisplayPreferences.showPageIndicator) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(workspacePages.size + 1) { index ->
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
                                    if (isSelected) themeRuntime.palette.accent
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                )
                        )
                    }
                }
            }
            if (isEditing) HomeEditPanel(
                onDone = { isEditing = false },
                onWidgets = { showWidgetPicker = true },
                onWallpaper = {
                    themeInitialSection = ThemeCenterSection.WALLPAPERS
                    showThemeSheet = true
                },
                onTheme = {
                    themeInitialSection = ThemeCenterSection.THEMES
                    showThemeSheet = true
                },
                onPages = { showPageManager = true },
                onSettings = { showDisplaySettings = true },
            )

            workspaceError?.let { Text(it, modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.error) }
            widgetMessage?.let { Text(it, modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.error) }

            if (!firstSession.journeyComplete) {
                val guideText = if (!firstSession.receivedFirstReply) {
                    "给${character.name}发一条消息"
                } else {
                    "看看${character.name}的近况"
                }
                Text(
                    text = guideText,
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = themeRuntime.layout.screenHorizontalPadding.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(themeRuntime.shapes.small.dp))
                        .background(themeRuntime.widgets.backgroundColor.copy(alpha = 0.14f))
                        .clickable { if (firstSession.receivedFirstReply) onNavigateToLiving() else onNavigateToChat() }
                        .padding(12.dp).testTag("first_session_guide"),
                    style = themeRuntime.text.secondary,
                    color = themeRuntime.widgets.foregroundColor,
                )
            }

            // Persistent five-slot dock; appearance does not change workspace placement.
            HomeHotseat(
                items = workspace.hotseatItems(), snapshot = workspace, labels = labels,
                accent = themeRuntime.palette.accent,
                isEditing = isEditing,
                draggedItemId = dragState.draggedItemId,
                hoverSlot = if (dragState.targetContainer == DesktopContainer.HOTSEAT &&
                    activeFolderIntent == FolderDropIntent.None)
                    dragState.hoverCell?.x else null,
                canDrop = dropPlan is DropPlan.Accept,
                onBounds = { hotseatBounds = it },
                onAppClick = { id -> dispatchAppAction(
                    id, onNavigateToMessages, onNavigateToMoments, onNavigateToLiving,
                    onNavigateToContacts, onNavigateToCheckPhone, onNavigateToDiary,
                    onNavigateToMemories, onNavigateToRelations, onNavigateToApps,
                    workspacePages.getOrNull(pagerState.currentPage)?.id, onAppClick,
                ) },
                onFolderClick = { openFolderId = it },
                folderHoverTargetId = FolderDropResolver.targetId(activeFolderIntent),
                iconScale = homeDisplayPreferences.iconScale,
            )

            // Virtual Home Indicator Bar
            VirtualPhoneHomeBar(
                canGoBack = false,
                onGoHome = {
                    if (isEditing) {
                        isEditing = false
                    }
                }
            )
        }

        val draggedItem = workspace.items.firstOrNull { it.id == dragState.draggedItemId }
        val draggedLabel = draggedItem?.let { labels[it.sourceId] }
        if (dragState.isDragging && draggedItem?.type == DesktopItemType.AILUA_WIDGET) {
            val item = draggedItem
            Box(Modifier.offset {
                IntOffset((dragState.pointerPosition.x - dragState.grabOffset.x).roundToInt(),
                    (dragState.pointerPosition.y - dragState.grabOffset.y).roundToInt())
            }.size(with(density) { dragState.previewWidth.toDp() },
                with(density) { dragState.previewHeight.toDp() })) {
                WorkspaceWidgetItem(item, widgetContext, false, false,
                    onSelect = {}, onDelete = {}, onResizePreview = {}, onResizeCommit = {})
            }
        } else if (dragState.isDragging && draggedItem?.type == DesktopItemType.FOLDER) {
            Box(Modifier.offset {
                IntOffset((dragState.pointerPosition.x - dragState.grabOffset.x).roundToInt(),
                    (dragState.pointerPosition.y - dragState.grabOffset.y).roundToInt())
            }.size(with(density) { dragState.previewWidth.toDp() },
                with(density) { dragState.previewHeight.toDp() }),
                contentAlignment = Alignment.Center) {
                WorkspaceFolderItem(
                    folder = workspace.folder(draggedItem.id)
                        ?: com.example.data.desktop.DesktopFolder(draggedItem.id, "文件夹"),
                    children = workspace.folderItems(draggedItem.id),
                    labels = labels, isEditing = true,
                    showLabel = homeDisplayPreferences.showLabels,
                    size = com.example.ui.components.AppIconDefaults.ContainerSize * homeDisplayPreferences.iconScale,
                    onClick = {},
                )
            }
        } else if (dragState.isDragging && draggedLabel != null) {
            DragLayer(
                label = draggedLabel,
                position = IntOffset(
                    (dragState.pointerPosition.x - dragState.grabOffset.x).roundToInt(),
                    (dragState.pointerPosition.y - dragState.grabOffset.y).roundToInt(),
                ),
                width = dragState.previewWidth,
                height = dragState.previewHeight,
            )
        }

        if (showDevTimeSheet) {
            WorldTimeDevSheet(onDismiss = { showDevTimeSheet = false })
        }

        if (showCharacterSwitcher) CharacterSwitcherSheet(
            selectedId = character.id,
            onSelect = { id ->
                CharacterContext.select(id)
                showCharacterSwitcher = false
            },
            onDismiss = { showCharacterSwitcher = false },
        )

        if (showThemeSheet) {
            ThemeCenterSheet(
                onDismiss = { showThemeSheet = false },
                initialSection = themeInitialSection,
                isDarkTheme = isDarkTheme,
                dayPhase = worldClock.dayPhase,
                weather = worldClock.weather,
            )
        }
        if (showWidgetPicker) {
            WidgetPickerSheet(onDismiss = { showWidgetPicker = false }) { sourceId, size ->
                val page = workspacePages.getOrNull(pagerState.currentPage)
                if (page == null) {
                    widgetMessage = "请先切换到桌面页"
                } else scope.launch {
                    when (workspaceViewModel.addWidget(sourceId, page.id, size.spanX, size.spanY)) {
                        true -> { showWidgetPicker = false; widgetMessage = null }
                        false -> widgetMessage = "这一页放不下啦"
                        null -> widgetMessage = "组件未添加，请重试"
                    }
                }
            }
        }
        if (showDisplaySettings) HomeDisplaySettingsSheet(
            preferences = homeDisplayPreferences,
            onChange = HomeDisplayPreferencesStore::update,
            onDismiss = { showDisplaySettings = false },
        )
        if (showPageManager) PageManagerSheet(
            snapshot = workspace,
            currentPageId = currentPageId,
            onDismiss = { showPageManager = false },
            onCreatePage = {
                scope.launch {
                    val page = workspaceViewModel.createPage()
                    if (page != null) currentPageId = page.id
                }
            },
            onDeletePage = { id -> scope.launch { workspaceViewModel.deletePage(id) } },
            onReorderPages = { ids -> scope.launch { workspaceViewModel.reorderPages(ids) } },
            onSetHomePage = { id -> scope.launch { workspaceViewModel.setHomePage(id) } },
        )
        val folderId = openFolderId
        val folder = folderId?.let(workspace::folder)
        if (folder != null) {
            FolderOverlay(
                folder = folder,
                children = workspace.folderItems(folder.id),
                labels = labels,
                onDismiss = { openFolderId = null },
                onRename = { title -> scope.launch { workspaceViewModel.renameFolder(folder.id, title) } },
                onOpenApp = { sourceId ->
                    openFolderId = null
                    dispatchAppAction(sourceId, onNavigateToMessages, onNavigateToMoments,
                        onNavigateToLiving, onNavigateToContacts, onNavigateToCheckPhone,
                        onNavigateToDiary, onNavigateToMemories, onNavigateToRelations,
                        onNavigateToApps, workspacePages.getOrNull(pagerState.currentPage)?.id,
                        onAppClick)
                },
                onMoveOut = { itemId ->
                    val pages = listOfNotNull(workspacePages.getOrNull(pagerState.currentPage)) +
                        workspacePages.filterNot { it.id == workspacePages.getOrNull(pagerState.currentPage)?.id }
                    val destination = pages.firstNotNullOfOrNull { page ->
                        WidgetPlacement.firstVacant(workspace.itemsFor(page.id), 1, 1)
                            ?.let { page to it }
                    }
                    if (destination == null) widgetMessage = "桌面没有空位"
                    else scope.launch {
                        val (page, cell) = destination
                        val saved = workspaceViewModel.moveItemOutOfFolder(itemId,
                            DesktopPlacement(DesktopContainer.WORKSPACE, page.id,
                                cell.x, cell.y, rank = cell.y * 4 + cell.x))
                        if (saved) openFolderId = null
                    }
                },
                onReorder = { itemId, index ->
                    scope.launch { workspaceViewModel.moveFolderItem(itemId, folder.id, index) }
                },
            )
        }
    }
    }
}

@Composable
internal fun DesktopWorldClock(
    worldClock: com.example.data.model.WorldClock,
    heartbeatState: com.example.data.engine.WorldHeartbeatState,
    accent: Color,
    onOpenDevTime: () -> Unit
) {
    val theme = LocalAiluaTheme.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDevTime).testTag("desktop_world_clock"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(worldClock.timeFormatted, style = theme.text.display, color = theme.palette.onSurface)
                Text(worldClock.weather.label, style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
            }
            Text("${worldClock.dateLabel} · ${worldClock.dayPhase.label}",
                style = theme.text.caption, color = theme.palette.onSurfaceMuted)
        }
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(theme.shapes.small.dp))
            .background(theme.surfaces.inset).clickable(onClick = onOpenDevTime),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.FastForward, contentDescription = "调整世界时间",
                tint = accent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun LivingPresenceStrip(
    character: CharacterProfile,
    onOpenChat: () -> Unit,
    onOpenLiving: () -> Unit,
    onSwitchCharacter: () -> Unit,
    compact: Boolean = false,
) {
    val theme = LocalAiluaTheme.current
    val events by WorldStateRepository.events.collectAsStateWithLifecycle()
    val presence = projectPresence(character, events)
    val latest = events.filter { it.characterId == character.id && !it.isUserActivity() }
        .sortedChronologically().lastOrNull()
    val latestDescription = latest?.description?.takeIf { it.isNotBlank() } ?: character.contextualQuote
    val sentenceEnd = latestDescription.indexOfAny(charArrayOf('，', '。', '\n', '\r', ',', '.'))
    val latestSentence = (if (sentenceEnd >= 0) latestDescription.take(sentenceEnd) else latestDescription).trim()
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenLiving).testTag("living_character_widget"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CharacterPortrait(
                characterId = character.id,
                variant = if (compact) PortraitVariant.AVATAR else PortraitVariant.HERO,
                modifier = Modifier.size(if (compact) 44.dp else 82.dp),
                onClick = onSwitchCharacter,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(character.name, style = theme.text.section, color = theme.widgets.foregroundColor,
                    modifier = Modifier.clickable(onClick = onSwitchCharacter)
                        .testTag("home_hero_switch_character"),
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(presence.currentActivity,
                    style = theme.text.secondary, color = theme.widgets.foregroundColor,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Box(
                modifier = Modifier.size(44.dp).clickable(onClick = onOpenChat).testTag("home_quick_chat"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "发消息",
                    modifier = Modifier.size(22.dp), tint = theme.widgets.foregroundColor)
            }
        }
        if (!compact && latestSentence.isNotBlank()) {
            Text(latestSentence,
                style = theme.text.secondary, color = theme.widgets.foregroundColor.copy(alpha = 0.80f),
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
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
    onNavigateToApps: (String?) -> Unit,
    preferredPageId: String?,
    onAppClick: (String) -> Unit
) {
    when (AppRouter.destinationOrNull(appId)) {
        AiluaDestinations.MESSAGES -> onNavigateToMessages()
        AiluaDestinations.MOMENTS -> onNavigateToMoments()
        AiluaDestinations.LIVING -> onNavigateToLiving()
        AiluaDestinations.CONTACTS -> onNavigateToContacts()
        AiluaDestinations.CHECK_PHONE -> onNavigateToCheckPhone()
        AiluaDestinations.DIARY -> onNavigateToDiary()
        AiluaDestinations.MEMORIES -> onNavigateToMemories()
        AiluaDestinations.RELATIONS -> onNavigateToRelations()
        AiluaDestinations.APPS -> onNavigateToApps(preferredPageId)
        else -> onAppClick(appId)
    }
}
