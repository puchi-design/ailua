package com.example.ui.checkphone

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.engine.CallStateEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.local.AiluaLocalStore
import com.example.data.model.CallState
import com.example.data.projection.checkphone.CheckPhonePlusSnapshot
import com.example.data.projection.checkphone.CharacterPhoneNote
import com.example.data.projection.checkphone.PhoneSection
import com.example.data.projection.checkphone.PhonePrivacyLevel
import com.example.data.projection.checkphone.projectCheckPhonePlus
import com.example.data.registry.CharacterRegistry
import com.example.data.relationship.repository.RelationshipStateRepository
import com.example.data.relationship.romance.RomanceRepository
import com.example.data.repository.GalleryRepository
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.designsystem.AiluaSectionHeader
import com.example.ui.designsystem.publicCharacterName
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
@Suppress("UNUSED_PARAMETER")
fun CheckPhoneScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    characterId: String = "mira",
    characterName: String = "苏晚宁",
    onGoHome: () -> Unit = onBackToHome,
    onOpenGallery: ((String) -> Unit)? = null,
) {
    val theme = LocalAiluaTheme.current
    val displayName = publicCharacterName(characterId, characterName)
    val events by WorldStateRepository.events.collectAsStateWithLifecycle()
    val galleryAssets by GalleryRepository.assets.collectAsStateWithLifecycle()
    val callHistory by CallStateEngine.callHistory.collectAsStateWithLifecycle()
    val worldPlan by AiluaLocalStore.savedWorldPlan.collectAsStateWithLifecycle()
    val worldClock by WorldHeartbeatEngine.worldClock.collectAsStateWithLifecycle()
    val relationships by RelationshipStateRepository.states.collectAsStateWithLifecycle()
    val romances by RomanceRepository.records.collectAsStateWithLifecycle()
    val cards by CharacterRegistry.allCards.collectAsStateWithLifecycle()
    val profile = remember(characterId, cards) { CharacterRuntimeResolver.resolve(characterId) }
    val firedActionIds = remember(worldClock, events) { AiluaLocalStore.getFiredWorldActionIds() }
    val snapshot = remember(characterId, events, galleryAssets, callHistory, worldPlan,
        relationships, romances, profile, worldClock, firedActionIds) {
        projectCheckPhonePlus(characterId, events, profile, galleryAssets, callHistory, worldPlan,
            relationships, romances.firstOrNull { it.characterId == characterId },
            firedActionIds = firedActionIds, clock = worldClock)
    }
    var section by rememberSaveable(characterId) { mutableStateOf<PhoneSection?>(null) }
    val goBack = {
        if (section == null) onBackToHome() else section = null
    }
    BackHandler(enabled = section != null) { section = null }
    val phoneSections = listOf(PhoneSection.SEARCH, PhoneSection.DRAFTS, PhoneSection.PHOTOS,
        PhoneSection.CALLS, PhoneSection.MUSIC, PhoneSection.NOTES, PhoneSection.USAGE) +
        if (snapshot.privacy >= PhonePrivacyLevel.PRIVATE) {
            listOf(PhoneSection.BROWSING, PhoneSection.SAVED)
        } else emptyList()
    val visibleSections = phoneSections.filter(snapshot::canSee)
    val lockedSections = phoneSections.filterNot(snapshot::canSee)

    AiluaScreenScaffold(
        title = section?.title ?: "${displayName}的手机",
        onBack = goBack,
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
            verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
        ) {
            if (section == null) {
                item { AiluaSectionHeader("生活痕迹") }
                items(visibleSections, key = { it.name }) { entry ->
                    PhoneSectionRow(entry, snapshot, onClick = { section = entry })
                }
                if (lockedSections.isNotEmpty()) {
                    item {
                        AiluaSectionHeader("更多记录",
                            modifier = Modifier.padding(top = theme.layout.sectionGap.dp))
                    }
                    item {
                        Text("有些内容只有更熟悉以后才能看到。", style = theme.text.caption,
                            color = theme.palette.onSurfaceMuted)
                    }
                    items(lockedSections, key = { it.name }) { entry ->
                        PhoneSectionRow(entry, snapshot, onClick = { section = entry })
                    }
                }
            } else {
                val current = checkNotNull(section)
                if (!snapshot.canSee(current)) {
                    item { LockedSection() }
                } else {
                    when (current) {
                        PhoneSection.SEARCH -> {
                            if (snapshot.searches.isEmpty()) item { EmptySection("还没有搜索记录") }
                            items(snapshot.searches, key = { it.id }) { record ->
                                PhoneTextEntry(record.query, record.time)
                            }
                        }
                        PhoneSection.DRAFTS -> {
                            if (snapshot.drafts.isEmpty()) item { EmptySection("还没有未发出的草稿") }
                            items(snapshot.drafts, key = { it.id }) { draft ->
                                PhoneTextEntry(draft.text, draft.createdAt?.let { "$it · 未发送" } ?: "未发送")
                            }
                        }
                        PhoneSection.PHOTOS -> {
                            if (snapshot.photos.isEmpty()) item { EmptySection("还没有照片") }
                            items(snapshot.photos, key = { it.id }) { photo ->
                                PhoneTextEntry(photo.title, photo.createdAtVirtualTime,
                                    onClick = onOpenGallery?.let { open -> { open(photo.id) } })
                            }
                            if (snapshot.photos.isNotEmpty() && onOpenGallery != null) item {
                                Text("点开照片可进入相册", style = theme.text.caption,
                                    color = theme.palette.onSurfaceMuted)
                            }
                        }
                        PhoneSection.CALLS -> {
                            if (snapshot.calls.isEmpty()) item { EmptySection("还没有通话记录") }
                            items(snapshot.calls, key = { it.id }) { call ->
                                val detail = when {
                                    call.state == CallState.DECLINED -> "已拒接"
                                    call.state == CallState.MISSED -> "未接通"
                                    call.durationSeconds > 0 -> "%02d:%02d".format(
                                        call.durationSeconds / 60, call.durationSeconds % 60)
                                    else -> "已结束"
                                }
                                PhoneTextEntry(if (call.userInitiated) "呼出" else "呼入",
                                    "${call.scheduledAtTime} · $detail")
                            }
                        }
                        PhoneSection.NOTES -> {
                            if (snapshot.notes.isEmpty()) item { EmptySection("还没有备忘记录") }
                            items(snapshot.notes, key = { it.id }) { note -> PhoneNoteEntry(note) }
                        }
                        PhoneSection.MUSIC -> {
                            if (snapshot.music.isEmpty()) item { EmptySection("还没有听歌记录") }
                            items(snapshot.music, key = { "${it.title}:${it.artist}" }) { track ->
                                PhoneTextEntry(track.title, track.artist.ifBlank { "最近在听" })
                            }
                            if (snapshot.music.isNotEmpty()) item {
                                Text("这里只记录听过的音乐。", style = theme.text.caption,
                                    color = theme.palette.onSurfaceMuted)
                            }
                        }
                        PhoneSection.USAGE -> {
                            if (snapshot.usage.isEmpty()) item { EmptySection("还没有应用使用记录") }
                            items(snapshot.usage, key = { it.id }) { usage ->
                                PhoneTextEntry(usage.appName, usage.time)
                            }
                            if (snapshot.usage.isNotEmpty()) item {
                                Text("根据角色已发生的生活记录整理。", style = theme.text.caption,
                                    color = theme.palette.onSurfaceMuted)
                            }
                        }
                        PhoneSection.BROWSING -> {
                            if (snapshot.browsing.isEmpty()) item { EmptySection("还没有浏览记录") }
                            items(snapshot.browsing) { entry -> PhoneTextEntry(entry, null) }
                        }
                        PhoneSection.SAVED -> {
                            if (snapshot.saved.isEmpty()) item { EmptySection("还没有收藏记录") }
                            items(snapshot.saved) { entry -> PhoneTextEntry(entry, null) }
                        }
                    }
                }
            }
        }
    }
}

private val PhoneSection.title: String get() = when (this) {
    PhoneSection.SEARCH -> "搜索记录"
    PhoneSection.DRAFTS -> "未发草稿"
    PhoneSection.PHOTOS -> "最近照片"
    PhoneSection.CALLS -> "最近通话"
    PhoneSection.NOTES -> "备忘记录"
    PhoneSection.MUSIC -> "音乐记录"
    PhoneSection.USAGE -> "使用记录"
    PhoneSection.BROWSING -> "浏览记录"
    PhoneSection.SAVED -> "收藏"
}

private val PhoneSection.icon: ImageVector get() = when (this) {
    PhoneSection.SEARCH -> Icons.Default.Search
    PhoneSection.DRAFTS -> Icons.Default.Message
    PhoneSection.PHOTOS -> Icons.Default.Image
    PhoneSection.CALLS -> Icons.Default.Call
    PhoneSection.NOTES -> Icons.Default.Notes
    PhoneSection.MUSIC -> Icons.Default.MusicNote
    PhoneSection.USAGE -> Icons.Default.Apps
    PhoneSection.BROWSING -> Icons.Default.History
    PhoneSection.SAVED -> Icons.Default.Bookmark
}

private fun CheckPhonePlusSnapshot.preview(section: PhoneSection): String = when {
    !canSee(section) -> "暂时无法查看"
    section == PhoneSection.SEARCH -> searches.firstOrNull()?.query
    section == PhoneSection.DRAFTS -> drafts.firstOrNull()?.text
    section == PhoneSection.PHOTOS -> photos.firstOrNull()?.title
    section == PhoneSection.CALLS -> calls.firstOrNull()?.scheduledAtTime
    section == PhoneSection.NOTES -> notes.firstOrNull()?.text
    section == PhoneSection.MUSIC -> music.firstOrNull()?.title
    section == PhoneSection.USAGE -> usage.firstOrNull()?.let { "${it.time}  ${it.appName}" }
    section == PhoneSection.BROWSING -> browsing.firstOrNull()
    else -> saved.firstOrNull()
} ?: "暂无记录"

@Composable
private fun PhoneSectionRow(section: PhoneSection, snapshot: CheckPhonePlusSnapshot, onClick: () -> Unit) {
    val theme = LocalAiluaTheme.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 66.dp)
            .clickable(onClick = onClick).testTag("check_phone_section_${section.name.lowercase()}"),
        horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(section.icon, contentDescription = null, tint = theme.palette.accent,
            modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(section.title, style = theme.text.body, color = theme.palette.onSurface)
            if (snapshot.canSee(section)) {
                Text(snapshot.preview(section), style = theme.text.secondary,
                    color = theme.palette.onSurfaceMuted, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(if (snapshot.canSee(section)) Icons.Default.ChevronRight else Icons.Default.Lock,
            contentDescription = null, tint = theme.palette.onSurfaceMuted,
            modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = theme.surfaces.divider)
}

@Composable
private fun PhoneTextEntry(title: String, secondary: String?, onClick: (() -> Unit)? = null) {
    val theme = LocalAiluaTheme.current
    val modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(vertical = theme.layout.itemGap.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = theme.text.body, color = theme.palette.onSurface)
        if (!secondary.isNullOrBlank()) Text(secondary, style = theme.text.caption,
            color = theme.palette.onSurfaceMuted)
    }
    HorizontalDivider(color = theme.surfaces.divider)
}

@Composable
private fun PhoneNoteEntry(note: CharacterPhoneNote) {
    PhoneTextEntry(note.text, when {
        note.planned -> "待办${note.time?.let { " · $it" } ?: ""}"
        else -> note.time
    })
}

@Composable
private fun LockedSection() {
    val theme = LocalAiluaTheme.current
    Column(Modifier.fillMaxWidth().padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = theme.palette.onSurfaceMuted,
            modifier = Modifier.size(28.dp))
        Text("这部分内容暂时无法查看", style = theme.text.body, color = theme.palette.onSurface)
        Text("等你们更熟悉，也许会有新的线索。", style = theme.text.secondary,
            color = theme.palette.onSurfaceMuted)
    }
}

@Composable
private fun EmptySection(message: String) {
    val theme = LocalAiluaTheme.current
    Text(message, style = theme.text.body, color = theme.palette.onSurfaceMuted,
        modifier = Modifier.padding(top = theme.layout.sectionGap.dp))
}
