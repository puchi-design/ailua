package com.example.ui.memories

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.memory.model.MemoryEntry
import com.example.data.memory.model.MemoryType
import com.example.data.memory.repository.MemoryGraph
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.designsystem.AiluaSectionHeader
import com.example.ui.themeengine.LocalAiluaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MemoriesScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    characterId: String = "mira",
    onGoHome: () -> Unit = onBackToHome
) {
    val theme = LocalAiluaTheme.current
    val memories by MemoryGraph.repository.observeMemories(characterId).collectAsState(initial = emptyList())
    val dateGroups = remember(memories) { memories.groupBy { formatMemoryDate(it.createdAtEpochMs) } }
    AiluaScreenScaffold(
        title = "记忆",
        onBack = onBackToHome,
        onGoHome = onGoHome,
        modifier = Modifier.testTag("memories_screen"),
        backTestTag = "memories_back_btn"
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp),
            verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)
        ) {
            if (memories.isEmpty()) {
                item { Text("还没有保存的记忆", style = theme.text.body, color = theme.palette.onSurfaceMuted) }
            }
            dateGroups.forEach { (date, entries) ->
                item(key = "date_$date") {
                    Column(
                        Modifier.padding(top = theme.layout.sectionGap.dp),
                        verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)
                    ) {
                        AiluaSectionHeader(title = date)
                        HorizontalDivider(color = theme.surfaces.divider)
                    }
                }
                items(entries, key = { it.id }) { memory ->
                    MemoryItemCard(memory, onDelete = { MemoryGraph.repository.deleteMemory(memory.id) })
                }
            }
            item { Spacer(Modifier.height(theme.layout.sectionGap.dp)) }
        }
    }
}

@Composable
private fun MemoryItemCard(memory: MemoryEntry, onDelete: () -> Unit) {
    val theme = LocalAiluaTheme.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = theme.layout.itemGap.dp).testTag("memory_card_${memory.id}"),
        verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)
    ) {
        Text(memory.content, style = theme.text.body, color = theme.palette.onSurface)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${if (memory.type == MemoryType.CORE) "核心记忆" else "长期记忆"} · ${if (memory.sourceAppId == "chat") "聊天" else memory.sourceAppId}",
                    style = theme.text.caption, color = theme.palette.onSurfaceMuted
                )
                Text(formatMemoryTime(memory.createdAtEpochMs), style = theme.text.caption, color = theme.palette.onSurfaceMuted)
            }
            IconButton(onClick = onDelete, modifier = Modifier.testTag("memory_delete_${memory.id}")) {
                Icon(Icons.Default.Delete, "删除记忆", Modifier.size(18.dp), tint = theme.palette.onSurfaceMuted)
            }
        }
    }
}

private fun formatMemoryDate(epochMs: Long): String =
    SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date(epochMs))

private fun formatMemoryTime(epochMs: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))
