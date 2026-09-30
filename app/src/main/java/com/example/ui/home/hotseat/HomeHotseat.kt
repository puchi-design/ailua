package com.example.ui.home.hotseat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.desktop.DesktopItem
import com.example.ui.components.AppIconItem
import com.example.ui.design.launcher.WorkspaceAppLabel

/** Five real slots projected from DesktopItem(HOTSEAT); gaps remain usable cells. */
@Composable
fun HomeHotseat(
    items: List<DesktopItem>,
    labels: Map<String, WorkspaceAppLabel>,
    accent: Color,
    isEditing: Boolean,
    draggedItemId: String?,
    hoverSlot: Int?,
    canDrop: Boolean,
    onBounds: (Rect) -> Unit,
    onAppClick: (String) -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .shadow(3.dp, shape,
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.16f))
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f))
                .background(accent.copy(alpha = 0.10f))
                .background(Brush.verticalGradient(listOf(
                    Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.02f))))
                .border(1.dp, accent.copy(alpha = 0.30f), shape)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .onGloballyPositioned { onBounds(it.boundsInRoot()) }
                .testTag("virtual_phone_dock"),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(5) { slot ->
                val item = items.firstOrNull { it.cellX == slot }
                val label = item?.let { labels[it.sourceId] }
                val outline = when {
                    hoverSlot == slot -> if (canDrop) accent else MaterialTheme.colorScheme.error
                    item == null && isEditing -> accent.copy(alpha = 0.24f)
                    else -> Color.Transparent
                }
                Box(
                    Modifier.weight(1f).border(1.dp, outline, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item != null && label != null) {
                        Box(
                            modifier = Modifier.then(if (item.id == draggedItemId) Modifier.drawGhost() else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            AppIconItem(
                                name = label.name, iconKey = label.iconKey, badge = label.badge,
                                showLabel = false, editMode = isEditing,
                                onClick = if (isEditing) ({}) else ({ onAppClick(item.sourceId) }),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.drawGhost(): Modifier = this.then(Modifier.alpha(0.25f))
