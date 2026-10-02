package com.example.ui.home.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.desktop.DesktopFolder
import com.example.data.desktop.DesktopItem
import com.example.ui.components.AppIconItem
import com.example.ui.components.HideDialogStatusBar
import com.example.ui.design.launcher.WorkspaceAppLabel
import com.example.ui.themeengine.LocalAiluaTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@Composable
fun FolderOverlay(
    folder: DesktopFolder,
    children: List<DesktopItem>,
    labels: Map<String, WorkspaceAppLabel>,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onOpenApp: (String) -> Unit,
    onMoveOut: (String) -> Unit,
    onReorder: (String, Int) -> Unit,
) {
    val theme = LocalAiluaTheme.current
    val focusManager = LocalFocusManager.current
    var title by remember(folder.id) { mutableStateOf(folder.title) }
    var order by remember(folder.id) { mutableStateOf(children) }
    var menuFor by remember(folder.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(children.map { it.id to it.rank }) { order = children }
    val gridState = rememberLazyGridState()
    val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
        if (from.index in order.indices && to.index in order.indices) {
            order = order.toMutableList().apply { add(to.index, removeAt(from.index)) }
        }
    }
    fun saveTitle() { onRename(title.ifBlank { "文件夹" }) }
    Dialog(onDismissRequest = { saveTitle(); onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HideDialogStatusBar()
        val shape = RoundedCornerShape(theme.widgets.cornerRadiusDp.dp)
        Column(
            Modifier.padding(24.dp).widthIn(max = 390.dp).fillMaxWidth()
                .clip(shape)
                .background(theme.widgets.backgroundColor.copy(alpha =
                    theme.widgets.surfaceAlpha.coerceAtLeast(0.88f)))
                .border(theme.widgets.border.widthDp.dp,
                    theme.widgets.border.color, shape)
                .padding(18.dp).testTag("folder_overlay"),
        ) {
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                textStyle = TextStyle(color = theme.palette.onSurface,
                    fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    saveTitle()
                    focusManager.clearFocus()
                }),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    .testTag("folder_title"),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3), state = gridState,
                modifier = Modifier.fillMaxWidth().heightIn(max = 390.dp)
                    .testTag("folder_grid"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(order, key = DesktopItem::id) { item ->
                    ReorderableItem(reorderState, key = item.id) {
                        val label = labels[item.sourceId]
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AppIconItem(
                                name = label?.name ?: item.sourceId,
                                iconKey = label?.iconKey ?: item.sourceId,
                                size = 52.dp,
                                showLabel = true,
                                onClick = { onOpenApp(item.sourceId) },
                                onLongClick = { menuFor = item.id },
                            )
                            Box(Modifier.longPressDraggableHandle(onDragStopped = {
                                val index = order.indexOfFirst { it.id == item.id }
                                if (index >= 0) onReorder(item.id, index)
                            }).padding(horizontal = 14.dp, vertical = 3.dp)
                                .testTag("folder_reorder_${item.id}")) {
                                Text("≡", color = theme.palette.onSurfaceMuted, fontSize = 16.sp)
                            }
                            DropdownMenu(
                                expanded = menuFor == item.id,
                                onDismissRequest = { menuFor = null },
                            ) {
                                DropdownMenuItem(text = { Text("打开") }, onClick = {
                                    menuFor = null
                                    onOpenApp(item.sourceId)
                                })
                                DropdownMenuItem(text = { Text("移到桌面") }, onClick = {
                                    menuFor = null
                                    onMoveOut(item.id)
                                })
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = { saveTitle(); onDismiss() }) { Text("完成") }
            }
        }
    }
}
