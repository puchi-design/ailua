package com.example.ui.apps

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.desktop.DesktopItemType
import com.example.data.desktop.WorkspaceGraph
import com.example.data.model.AiluaApp
import com.example.ui.components.AppIconItem
import com.example.ui.designsystem.AiluaChip
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.design.launcher.WorkspaceViewModel
import com.example.ui.launcher.LauncherAppCatalog
import com.example.ui.themeengine.LocalAiluaTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppLibraryScreen(
    isDarkTheme: Boolean = false,
    initialSearchQuery: String = "",
    preferredPageId: String? = null,
    onToggleTheme: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    onOpenApp: (String) -> Unit = {},
    onGoHome: () -> Unit = onBackToHome,
) {
    val context = LocalContext.current
    val themeRuntime = LocalAiluaTheme.current
    val workspaceViewModel: WorkspaceViewModel = viewModel(factory = WorkspaceViewModel.factory(context))
    val workspace by workspaceViewModel.workspace.collectAsStateWithLifecycle()
    val placedApps = workspace.items.asSequence()
        .filter { it.type == DesktopItemType.APP }
        .map { LauncherAppCatalog.canonicalId(it.sourceId) }
        .toSet()
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf(initialSearchQuery) }
    var selectedCategory by remember { mutableStateOf(APP_LIBRARY_ALL) }
    var detailsApp by remember { mutableStateOf<AiluaApp?>(null) }

    val apps = LauncherAppCatalog.drawerApps()
    val categories = remember { appLibraryCategories(apps) }
    val filteredApps = remember(searchQuery, selectedCategory) {
        filterAppLibrary(apps, searchQuery, selectedCategory)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AiluaScreenScaffold(
            title = "应用库",
            onBack = onBackToHome,
            onGoHome = onGoHome,
            backTestTag = "apps_back_btn",
            modifier = Modifier.testTag("app_library_screen"),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = themeRuntime.layout.screenHorizontalPadding.dp, vertical = themeRuntime.layout.itemGap.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_library_search"),
                    placeholder = {
                        Text(
                            text = "搜索应用名称或分类",
                            style = themeRuntime.text.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    shape = RoundedCornerShape(themeRuntime.shapes.medium.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = themeRuntime.palette.accent.copy(alpha = 0.7f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    singleLine = true,
                    textStyle = themeRuntime.text.body,
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = themeRuntime.layout.screenHorizontalPadding.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    AiluaChip(
                        label = category,
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = { onOpenApp("reality") }, modifier = Modifier.padding(horizontal = themeRuntime.layout.screenHorizontalPadding.dp).testTag("reality_bridge_entry")) {
                Text("现实感知", style = themeRuntime.text.secondary)
            }
            TextButton(onClick = { onOpenApp("settings") }, modifier = Modifier.padding(horizontal = themeRuntime.layout.screenHorizontalPadding.dp).testTag("settings_entry")) {
                Text("设置", style = themeRuntime.text.secondary)
            }

            if (filteredApps.isEmpty()) {
                AppLibraryEmptyState(
                    modifier = Modifier.weight(1f),
                    query = searchQuery,
                    category = selectedCategory
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(filteredApps, key = { it.id }) { app ->
                        AppLibraryGridCell(
                            app = app,
                            onOpen = {
                                if (app.route != null) onOpenApp(app.id)
                                else detailsApp = app
                            },
                            alreadyOnHome = LauncherAppCatalog.canonicalId(app.id) in placedApps,
                            canAddToHome = LauncherAppCatalog.canAddToHome(app.id),
                            onAddToHome = {
                                scope.launch {
                                    val added = try {
                                        withContext(Dispatchers.IO) {
                                            WorkspaceGraph.init(context)
                                            WorkspaceGraph.repository.addAppToWorkspace(app.id, preferredPageId)
                                        } != null
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (_: Exception) {
                                        false
                                    }
                                    snackbarHost.showSnackbar(if (added) "已添加到桌面" else "暂时无法添加到桌面")
                                }
                            },
                            onOpenDetails = { detailsApp = app }
                        )
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp))
    }

    detailsApp?.let { app ->
        AlertDialog(
            onDismissRequest = { detailsApp = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppIconItem(
                        name = "",
                        iconKey = app.iconKey,
                        size = 40.dp,
                        showLabel = false,
                        onClick = {}
                    )
                    Column {
                        Text(
                            text = app.name,
                            style = themeRuntime.text.section
                        )
                        Text(
                            text = app.category,
                            style = themeRuntime.text.caption,
                            color = themeRuntime.palette.onSurfaceMuted
                        )
                    }
                }
            },
            text = {
                Text(
                    text = app.description,
                    style = themeRuntime.text.body
                )
            },
            confirmButton = {
                TextButton(onClick = { detailsApp = null }) {
                    Text("知道了")
                }
            }
        )
    }
}

@Composable
private fun AppLibraryGridCell(
    app: AiluaApp,
    onOpen: () -> Unit,
    alreadyOnHome: Boolean,
    canAddToHome: Boolean,
    onAddToHome: () -> Unit,
    onOpenDetails: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxWidth().testTag("app_library_item_${app.id}"),
        contentAlignment = Alignment.Center
    ) {
        AppIconItem(
            name = appLibraryDisplayName(app),
            iconKey = app.iconKey,
            onClick = onOpen,
            onLongClick = { menuOpen = true }
        )
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false }
        ) {
            DropdownMenuItem(
                text = { Text("打开") },
                onClick = {
                    menuOpen = false
                    onOpen()
                }
            )
            DropdownMenuItem(
                text = { Text(if (alreadyOnHome) "已在桌面" else "添加到桌面") },
                modifier = Modifier.testTag("app_library_add_${app.id}"),
                enabled = canAddToHome && !alreadyOnHome,
                onClick = {
                    menuOpen = false
                    onAddToHome()
                }
            )
            DropdownMenuItem(
                text = { Text("应用信息") },
                onClick = {
                    menuOpen = false
                    onOpenDetails()
                }
            )
        }
    }
}

@Composable
private fun AppLibraryEmptyState(
    modifier: Modifier = Modifier,
    query: String,
    category: String
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .testTag("app_library_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (query.isNotBlank()) {
                "没有找到「${query.trim()}」相关的应用"
            } else {
                "「$category」下暂无应用"
            },
            style = LocalAiluaTheme.current.text.secondary,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
