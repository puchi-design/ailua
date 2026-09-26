package com.example.ui.apps

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.mock.MockData
import com.example.data.model.AiluaApp
import com.example.ui.components.AppIconItem
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.theme.AiluaMistBlue

@Composable
fun AppLibraryScreen(
    isDarkTheme: Boolean = false,
    initialSearchQuery: String = "",
    onToggleTheme: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToContacts: () -> Unit = {},
    onNavigateToMoments: () -> Unit = {},
    onNavigateToLiving: () -> Unit = {},
    onNavigateToDiary: () -> Unit = {},
    onNavigateToCheckPhone: () -> Unit = {},
    onNavigateToRelations: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    onNavigateToCharacterCreator: () -> Unit = {},
    onNavigateToWorldBook: () -> Unit = {},
    onNavigateToWorldMap: () -> Unit = {},
    onNavigateToTheater: () -> Unit = {},
    onNavigateToMailbox: () -> Unit = {},
    onNavigateToCall: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf(initialSearchQuery) }
    var selectedCategory by remember { mutableStateOf(APP_LIBRARY_ALL) }
    var detailsApp by remember { mutableStateOf<AiluaApp?>(null) }

    val categories = remember { appLibraryCategories(MockData.appLibraryList) }
    val filteredApps = remember(searchQuery, selectedCategory) {
        filterAppLibrary(MockData.appLibraryList, searchQuery, selectedCategory)
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToHome,
                modifier = Modifier.testTag("apps_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回AILUA主屏",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "应用库 · App Library",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
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
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
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
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = AiluaMistBlue.copy(alpha = 0.7f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                ),
                singleLine = true
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            1.dp,
                            if (isSelected) AiluaMistBlue.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 11.5.sp
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

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
                            when (app.id) {
                                "chat", "messages" -> onNavigateToMessages()
                                "contacts" -> onNavigateToContacts()
                                "moments" -> onNavigateToMoments()
                                "living" -> onNavigateToLiving()
                                "diary" -> onNavigateToDiary()
                                "check_phone" -> onNavigateToCheckPhone()
                                "relations" -> onNavigateToRelations()
                                "memories" -> onNavigateToMemories()
                                "character_creation" -> onNavigateToCharacterCreator()
                                "lore_books" -> onNavigateToWorldBook()
                                "world_map" -> onNavigateToWorldMap()
                                "theater" -> onNavigateToTheater()
                                "mailbox" -> onNavigateToMailbox()
                                "call", "companion_call" -> onNavigateToCall()
                                "gallery" -> onNavigateToGallery()
                                else -> detailsApp = app
                            }
                        },
                        onOpenDetails = { detailsApp = app }
                    )
                }
            }
        }

        VirtualPhoneHomeBar(
            canGoBack = true,
            onBack = onBackToHome,
            onGoHome = onBackToHome
        )
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
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = app.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            },
            text = {
                Text(
                    text = app.description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp)
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
    onOpenDetails: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxWidth(),
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
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
