package com.example.ui.home.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.desktop.DesktopPage
import com.example.data.desktop.WorkspaceSnapshot
import com.example.ui.themeengine.LocalAiluaTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageManagerSheet(
    snapshot: WorkspaceSnapshot,
    onDismiss: () -> Unit,
    onCreatePage: () -> Unit,
    onDeletePage: (String) -> Unit,
    onReorderPages: (List<String>) -> Unit,
    onSetHomePage: (String) -> Unit,
    currentPageId: String? = null,
) {
    val theme = LocalAiluaTheme.current
    var order by remember { mutableStateOf(snapshot.pages.sortedBy(DesktopPage::rank)) }
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(snapshot.pages.map { Triple(it.id, it.rank, it.isHome) }) {
        order = snapshot.pages.sortedBy(DesktopPage::rank)
    }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        if (from.index in order.indices && to.index in order.indices) {
            order = order.toMutableList().apply { add(to.index, removeAt(from.index)) }
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = theme.palette.surface,
        modifier = Modifier.testTag("page_manager_sheet"),
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Text("管理桌面", color = theme.palette.onSurface,
                fontSize = 21.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp))
            Text("长按页面预览拖动排序 · 点击星标设为主屏",
                color = theme.palette.onSurfaceMuted, fontSize = 12.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 3.dp, bottom = 16.dp))
            LazyRow(state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("page_manager_list")) {
                items(order, key = DesktopPage::id) { page ->
                    ReorderableItem(reorderState, key = page.id) {
                        val pageItems = snapshot.itemsFor(page.id)
                        PageThumbnail(
                            page = page,
                            pageNumber = order.indexOfFirst { it.id == page.id } + 1,
                            items = pageItems,
                            isCurrent = currentPageId == page.id,
                            onSetHomePage = { onSetHomePage(page.id) },
                            onDeletePage = {
                                notice = when {
                                    order.size <= 1 -> "至少保留一个桌面"
                                    page.isHome -> "请先将其他页面设为主屏"
                                    pageItems.isNotEmpty() -> "此页还有内容"
                                    else -> { onDeletePage(page.id); null }
                                }
                            },
                            modifier = Modifier.longPressDraggableHandle(onDragStopped = {
                                val ids = order.map(DesktopPage::id)
                                if (ids != snapshot.pages.sortedBy(DesktopPage::rank).map(DesktopPage::id)) {
                                    onReorderPages(ids)
                                }
                            }),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(theme.palette.accent.copy(alpha = 0.12f))
                .border(1.dp, theme.palette.accent.copy(alpha = 0.5f), RoundedCornerShape(13.dp))
                .testTag("page_create"), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.TextButton(onClick = { notice = null; onCreatePage() },
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null, tint = theme.palette.accent)
                    Text("新建页面", color = theme.palette.accent,
                        modifier = Modifier.padding(start = 6.dp))
                }
            }
            if (notice != null) {
                Text(notice.orEmpty(), color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp)
                        .testTag("page_manager_notice"))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Life Bento · 特殊页", color = theme.palette.onSurface,
                    fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("固定", color = theme.palette.onSurfaceMuted, fontSize = 12.sp)
            }
        }
    }
}
