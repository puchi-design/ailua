package com.example.ui.design.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import com.example.data.desktop.DesktopItem
import com.example.data.desktop.GridSpec
import com.example.ui.design.launcher.layout.LayoutSolution

/** A bounded spatial layout with real empty cells and span support. */
@Composable
fun CellLayout(
    grid: GridSpec,
    items: List<DesktopItem>,
    transientLayout: LayoutSolution?,
    modifier: Modifier = Modifier,
    content: @Composable (DesktopItem) -> Unit,
) {
    Layout(
        content = {
            items.forEach { item ->
                key(item.id) { Box { content(item) } }
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val cellWidth = width / grid.columns
        val cellHeight = height / grid.rows
        val placeables = measurables.mapIndexed { index, measurable ->
            val item = items[index]
            measurable.measure(Constraints.fixed(cellWidth * item.spanX, cellHeight * item.spanY))
        }
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val item = items[index]
                val rect = transientLayout?.placements?.get(item.id)
                placeable.place((rect?.x ?: item.cellX) * cellWidth, (rect?.y ?: item.cellY) * cellHeight)
            }
        }
    }
}
