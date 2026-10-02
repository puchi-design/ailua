package com.example.ui.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.engine.WorldStateRepository
import com.example.data.model.DiaryEntry
import com.example.data.projection.projectDiary
import com.example.ui.designsystem.AiluaChip
import com.example.ui.designsystem.AiluaMediaFrame
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.themeengine.LocalAiluaTheme
import kotlinx.coroutines.launch

@Composable
fun DiaryScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    characterId: String = "mira",
    characterName: String = "小弥",
    onGoHome: () -> Unit = onBackToHome
) {
    val theme = LocalAiluaTheme.current
    val worldEvents by WorldStateRepository.events.collectAsStateWithLifecycle()
    val entries = remember(characterId, worldEvents) {
        projectDiary(characterId = characterId, runtimeEvents = worldEvents)
    }
    var selectedEntryId by remember(characterId) { mutableStateOf(entries.firstOrNull()?.id ?: "") }
    val currentEntry = entries.find { it.id == selectedEntryId } ?: entries.firstOrNull()
    var showReader by remember(characterId) { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    BackHandler(enabled = showReader) { showReader = false }

    AiluaScreenScaffold(
        title = "日记",
        onBack = { if (showReader) showReader = false else onBackToHome() },
        onGoHome = onGoHome,
        backTestTag = "diary_back_btn",
        modifier = Modifier.testTag("diary_screen"),
        bottomBar = { SnackbarHost(snackbarHostState) }
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .padding(horizontal = theme.layout.screenHorizontalPadding.dp),
            verticalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp)
        ) {
            if (showReader && currentEntry != null) {
                item(key = currentEntry.id) {
                    DiaryReader(
                        entry = currentEntry,
                        isLiked = isLiked,
                        onLike = {
                            isLiked = !isLiked
                            coroutineScope.launch {
                                if (isLiked) snackbarHostState.showSnackbar("已喜欢${characterName.ifBlank { "角色" }}的日记")
                            }
                        }
                    )
                }
            } else if (entries.isEmpty()) {
                item { Text("还没有日记", style = theme.text.body, color = theme.palette.onSurfaceMuted) }
            } else {
                items(entries, key = { it.id }) { entry ->
                    Column(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedEntryId = entry.id
                            isLiked = false
                            showReader = true
                        }.padding(vertical = theme.layout.itemGap.dp),
                        verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)
                    ) {
                        Text(entry.date, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                        Text(entry.title, style = theme.text.section, color = theme.palette.onSurface)
                        Text(
                            entry.excerpt.ifBlank { entry.content }, style = theme.text.body,
                            color = theme.palette.onSurfaceMuted, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(theme.layout.itemGap.dp))
                        HorizontalDivider(color = theme.surfaces.divider)
                    }
                }
            }
            item { Spacer(Modifier.height(theme.layout.sectionGap.dp)) }
        }
    }
}

@Composable
private fun DiaryReader(entry: DiaryEntry, isLiked: Boolean, onLike: () -> Unit) {
    val theme = LocalAiluaTheme.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = theme.layout.itemGap.dp),
        verticalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry.date, style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
            val context = listOf(entry.weather, entry.mood).filter { it.isNotBlank() }.joinToString(" · ")
            if (context.isNotBlank()) Text(context, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
        }
        Text("《${entry.title}》", style = theme.text.title, color = theme.palette.onSurface)
        Text(entry.content, style = theme.text.body, color = theme.palette.onSurface)
        entry.imageReference?.let {
            AiluaMediaFrame(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)
                ) {
                    Icon(Icons.Default.PhotoAlbum, null, Modifier.size(28.dp), tint = theme.palette.onSurfaceMuted)
                    Text("随文照片", style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                }
            }
        }
        Text("——${entry.authorName}", modifier = Modifier.align(Alignment.End), style = theme.text.body, color = theme.palette.onSurfaceMuted)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AiluaChip(
                label = if (isLiked) "已喜欢" else "喜欢",
                onClick = onLike,
                selected = isLiked,
                leading = {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "喜欢日记", modifier = Modifier.size(18.dp),
                        tint = if (isLiked) theme.palette.accent else theme.palette.onSurfaceMuted
                    )
                }
            )
        }
    }
}
