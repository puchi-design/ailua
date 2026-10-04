package com.example.ui.checkphone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.engine.WorldStateRepository
import com.example.data.model.PrivatePhoto
import com.example.data.projection.projectCheckPhone
import com.example.ui.designsystem.AiluaMediaFrame
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.designsystem.AiluaSectionHeader
import com.example.ui.designsystem.publicCharacterName
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun CheckPhoneScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    characterId: String = "mira",
    characterName: String = "苏晚宁",
    onGoHome: () -> Unit = onBackToHome,
) {
    val theme = LocalAiluaTheme.current
    val displayName = publicCharacterName(characterId, characterName)
    val worldEvents by WorldStateRepository.events.collectAsStateWithLifecycle()
    val data = remember(characterId, worldEvents) {
        projectCheckPhone(characterId = characterId, runtimeEvents = worldEvents)
    }

    AiluaScreenScaffold(
        title = "${displayName}的手机",
        onBack = onBackToHome,
        onGoHome = onGoHome,
        backTestTag = "check_phone_back_btn",
        modifier = Modifier.testTag("check_phone_screen"),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = theme.layout.screenHorizontalPadding.dp,
                vertical = theme.layout.itemGap.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                    AiluaSectionHeader("最近在听")
                    if (data.recentlyPlayed.isEmpty()) CheckPhoneEmptyHint()
                    data.recentlyPlayed.forEach { track ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
                        ) {
                            AiluaMediaFrame(Modifier.size(48.dp)) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = theme.palette.accent,
                                    modifier = Modifier.align(Alignment.Center).size(22.dp),
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(track.title, style = theme.text.body, color = theme.palette.onSurface)
                                Text(track.artist, style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                            }
                        }
                    }
                }
            }
            item { PhoneTextSection("搜索记录", data.searchHistory) }
            item { PhoneTextSection("备忘录", data.notes) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                    AiluaSectionHeader("照片")
                    if (data.privateGallery.isEmpty()) {
                        CheckPhoneEmptyHint()
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                            items(data.privateGallery) { photo -> PrivatePhotoItem(photo) }
                        }
                    }
                }
            }
            item { PhoneTextSection("未发送草稿", data.unsentDrafts) }
            item { PhoneTextSection("浏览记录", data.browsingHistory) }
            item { PhoneTextSection("收藏", data.savedItems) }
            item { PhoneTextSection("心里话", data.hiddenThoughts) }
            item {
                Text(
                    "这些内容来自角色的虚拟生活。",
                    style = theme.text.caption,
                    color = theme.palette.onSurfaceMuted,
                    modifier = Modifier.padding(bottom = theme.layout.itemGap.dp),
                )
            }
        }
    }
}

@Composable
private fun PhoneTextSection(title: String, entries: List<String>) {
    val theme = LocalAiluaTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
        AiluaSectionHeader(title)
        if (entries.isEmpty()) CheckPhoneEmptyHint()
        entries.forEachIndexed { index, entry ->
            Text(entry, style = theme.text.body, color = theme.palette.onSurface)
            if (index < entries.lastIndex) HorizontalDivider(color = theme.surfaces.divider)
        }
    }
}

@Composable
private fun CheckPhoneEmptyHint() {
    val theme = LocalAiluaTheme.current
    Text("暂无记录", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
}

@Composable
private fun PrivatePhotoItem(photo: PrivatePhoto) {
    val theme = LocalAiluaTheme.current
    Column(
        modifier = Modifier.width(200.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AiluaMediaFrame(Modifier.fillMaxWidth().height(132.dp)) {
            PrivatePhotoFallback(photo.imageType)
        }
        Text(photo.title, style = theme.text.body, color = theme.palette.onSurface)
        Text(photo.note, style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
        Text(photo.time, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
    }
}

/** Existing placeholder art stays isolated until media assets are available. */
@Composable
private fun PrivatePhotoFallback(imageType: String) {
    val gradientColors = when (imageType) {
        "flowers" -> listOf(Color(0xFFE4BCBC), Color(0xFFCCA2A2))
        "rain_window" -> listOf(Color(0xFF869EB5), Color(0xFF6B8399))
        else -> listOf(Color(0xFF655F7A), Color(0xFF4C4760))
    }
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(gradientColors)))
}
