package com.example.ui.home.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.desktop.DesktopFolder
import com.example.data.desktop.DesktopItem
import com.example.ui.components.AppIconItem
import com.example.ui.components.AppIconDefaults
import com.example.ui.design.launcher.WorkspaceAppLabel
import com.example.ui.themeengine.LocalAiluaTheme

/** Folder previews use AppIconItem, and therefore the same IconResolver as Home and Drawer. */
@Composable
fun WorkspaceFolderItem(
    folder: DesktopFolder,
    children: List<DesktopItem>,
    labels: Map<String, WorkspaceAppLabel>,
    isEditing: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = AppIconDefaults.ContainerSize,
) {
    val theme = LocalAiluaTheme.current
    val shape = RoundedCornerShape(theme.shapes.medium.dp)
    val preview = children.take(4)
    Column(
        modifier = modifier.clickable { if (!isEditing) onClick() }
            .testTag("workspace_folder_${folder.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(size).clip(shape)
                .background(theme.surfaces.raised.copy(alpha = theme.widgets.surfaceAlpha.coerceAtLeast(0.55f)))
                .border(1.dp, theme.palette.border, shape),
            contentAlignment = Alignment.Center,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(2) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(2) { column ->
                            val child = preview.getOrNull(row * 2 + column)
                            if (child == null) Box(Modifier.size(size * 0.34f))
                            else {
                                val label = labels[child.sourceId]
                                AppIconItem(
                                    name = label?.name ?: child.sourceId,
                                    iconKey = label?.iconKey ?: child.sourceId,
                                    size = size * 0.34f,
                                    showLabel = false,
                                    editMode = false,
                                    onClick = { if (!isEditing) onClick() },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showLabel) {
            Text(
                folder.title, color = theme.icons.labelColor,
                style = theme.text.caption.copy(fontSize = theme.text.caption.fontSize * theme.typography.labelSizeScale),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
