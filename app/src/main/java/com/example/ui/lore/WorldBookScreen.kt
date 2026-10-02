package com.example.ui.lore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.mock.WorldData
import com.example.data.model.LoreEntry
import com.example.ui.designsystem.AiluaChip
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.designsystem.AiluaSectionHeader
import com.example.ui.designsystem.AiluaSurface
import com.example.ui.designsystem.SurfaceTone
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun WorldBookScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {},
    onGoHome: () -> Unit = onBack,
) {
    val theme = LocalAiluaTheme.current
    val loreEntries = remember { mutableStateListOf(*WorldData.sampleLoreEntries.toTypedArray()) }
    var selectedCategory by remember { mutableStateOf("全部") }
    var searchQuery by remember { mutableStateOf("") }

    // Live contextual trigger simulation state
    var simulatedLocationId by remember { mutableStateOf("place_street_23") }
    var simulatedText by remember { mutableStateOf("旧书 唱片 雨夜") }
    var showSimulatorPanel by remember { mutableStateOf(false) }

    val categories = listOf("全部", "世界设定", "地点", "人物关系", "事件", "习惯", "秘密", "共同记忆")

    // Active lore evaluated dynamically via pure helper
    val activeResults by remember(simulatedLocationId, simulatedText, loreEntries) {
        derivedStateOf {
            WorldData.getActiveLore(
                characterId = "mira",
                locationId = simulatedLocationId,
                recentText = simulatedText,
                lifeEventTitle = "便利店寻宝"
            )
        }
    }

    val activeEntryIds = remember(activeResults) {
        activeResults.map { it.entry.id }.toSet()
    }

    val filteredEntries = loreEntries.filter { entry ->
        val matchesCategory = selectedCategory == "全部" || entry.category == selectedCategory
        val matchesSearch = searchQuery.isBlank() ||
                entry.title.contains(searchQuery, ignoreCase = true) ||
                entry.content.contains(searchQuery, ignoreCase = true) ||
                entry.keywords.any { it.contains(searchQuery, ignoreCase = true) }
        matchesCategory && matchesSearch
    }


    AiluaScreenScaffold(
        title = "世界书",
        onBack = onBack,
        onGoHome = onGoHome,
        backTestTag = "lore_back_btn",
        modifier = Modifier.testTag("world_book_screen"),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp),
            verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索条目、地点或关键词", style = theme.text.body) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                textStyle = theme.text.body,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(theme.shapes.medium.dp),
                singleLine = true,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    AiluaChip(
                        label = category,
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = theme.layout.screenHorizontalPadding.dp,
                vertical = theme.layout.itemGap.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
        ) {
            item {
                AiluaSectionHeader(
                    title = "${filteredEntries.size} 条设定",
                    actionLabel = if (showSimulatorPanel) "收起高级" else "高级",
                    onAction = { showSimulatorPanel = !showSimulatorPanel },
                )
                AnimatedVisibility(showSimulatorPanel) {
                    AiluaSurface(Modifier.fillMaxWidth().padding(top = theme.layout.itemGap.dp), tone = SurfaceTone.INSET) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
                        ) {
                            Text("激活模拟器", style = theme.text.section, color = theme.palette.onSurface)
                            Text("当前激活 ${activeResults.size} 条", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                            OutlinedTextField(
                                value = simulatedText,
                                onValueChange = { simulatedText = it },
                                label = { Text("对话文本 / 触发关键词", style = theme.text.secondary) },
                                textStyle = theme.text.body,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(theme.shapes.small.dp),
                            )
                            Text("模拟地点", style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(
                                    listOf(
                                        "place_street_23" to "青石街23号",
                                        "place_moonlight" to "月光书阁",
                                        "place_mulan" to "木兰茶馆",
                                    )
                                ) { (id, label) ->
                                    AiluaChip(
                                        label = label,
                                        selected = simulatedLocationId == id,
                                        onClick = { simulatedLocationId = id },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (filteredEntries.isEmpty()) {
                item { Text("没有找到相关条目", style = theme.text.body, color = theme.palette.onSurfaceMuted) }
            }
            items(filteredEntries, key = { it.id }) { entry ->
                val isActive = activeEntryIds.contains(entry.id)
                val activeResult = activeResults.firstOrNull { it.entry.id == entry.id }
                LoreEntryRow(
                    entry = entry,
                    isActive = isActive,
                    activationReasons = activeResult?.activationReasons ?: emptyList(),
                    onToggleEnabled = {
                        val idx = loreEntries.indexOfFirst { it.id == entry.id }
                        if (idx >= 0) {
                            loreEntries[idx] = entry.copy(enabled = !entry.enabled)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LoreEntryRow(
    entry: LoreEntry,
    isActive: Boolean,
    activationReasons: List<String>,
    onToggleEnabled: () -> Unit,
) {
    val theme = LocalAiluaTheme.current
    var expanded by remember(entry.id) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).clickable { expanded = !expanded }.padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(entry.title, style = theme.text.section, color = theme.palette.onSurface)
                Text(entry.category, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                Text(
                    entry.content,
                    style = theme.text.secondary,
                    color = theme.palette.onSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(checked = entry.enabled, onCheckedChange = { onToggleEnabled() })
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起详情" else "查看详情",
                    tint = theme.palette.onSurfaceMuted,
                )
            }
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                Text(entry.content, style = theme.text.body, color = theme.palette.onSurface)
                AiluaSectionHeader("高级字段")
                LoreField("模式", entry.activationMode.name)
                LoreField("优先级", entry.priority.toString())
                LoreField("关键词", entry.keywords.joinToString("、").ifBlank { "无" })
                if (entry.secondaryKeywords.isNotEmpty()) LoreField("辅助关键词", entry.secondaryKeywords.joinToString("、"))
                LoreField("地点", entry.locationIds.joinToString("、").ifBlank { "不限" })
                if (entry.characterIds.isNotEmpty()) LoreField("角色", entry.characterIds.joinToString("、"))
                if (entry.notes.isNotBlank()) LoreField("备注", entry.notes)
                LoreField("模拟状态", if (isActive) "当前激活" else "未激活")
                if (isActive && activationReasons.isNotEmpty()) {
                    Text(activationReasons.joinToString(" ｜ "), style = theme.text.secondary, color = theme.palette.accent)
                }
            }
        }
        HorizontalDivider(color = theme.surfaces.divider)
    }
}

@Composable
private fun LoreField(label: String, value: String) {
    val theme = LocalAiluaTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
        Text(value, style = theme.text.secondary, color = theme.palette.onSurface)
    }
}
