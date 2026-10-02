package com.example.ui.home.page

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.desktop.DesktopItem
import com.example.data.desktop.DesktopItemType
import com.example.data.desktop.DesktopPage
import com.example.ui.themeengine.LocalAiluaTheme

/** Draws only cell footprints; live widgets and icon bitmaps stay out of the manager. */
@Composable
fun PageThumbnail(
    page: DesktopPage,
    pageNumber: Int,
    items: List<DesktopItem>,
    isCurrent: Boolean,
    onSetHomePage: () -> Unit,
    onDeletePage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAiluaTheme.current
    val shape = RoundedCornerShape(theme.shapes.medium.dp)
    Column(modifier.width(130.dp).clip(shape)
        .background(theme.surfaces.inset)
        .border(if (isCurrent || page.isHome) 2.dp else 1.dp,
            if (isCurrent || page.isHome) theme.palette.accent else theme.palette.border, shape)
        .padding(8.dp).testTag("page_thumbnail_${page.id}")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("第 ${pageNumber} 页", color = theme.palette.onSurface,
                style = theme.text.secondary, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            if (isCurrent) Text("当前", color = theme.palette.accent, style = theme.text.caption)
        }
        Canvas(Modifier.fillMaxWidth().height(142.dp).padding(top = 7.dp)
            .clip(RoundedCornerShape(theme.shapes.small.dp)).testTag("page_preview_${page.id}")) {
            drawRoundRect(theme.surfaces.screen, cornerRadius = CornerRadius(theme.shapes.small.dp.toPx()))
            val cellW = size.width / 4f
            val cellH = size.height / 6f
            for (column in 1..3) {
                val x = column * cellW
                drawLine(theme.palette.border.copy(alpha = 0.20f), Offset(x, 0f), Offset(x, size.height), 0.6.dp.toPx())
            }
            for (row in 1..5) {
                val y = row * cellH
                drawLine(theme.palette.border.copy(alpha = 0.20f), Offset(0f, y), Offset(size.width, y), 0.6.dp.toPx())
            }
            items.forEach { item ->
                if (item.cellX !in 0..3 || item.cellY !in 0..5) return@forEach
                val left = item.cellX * cellW + 3.dp.toPx()
                val top = item.cellY * cellH + 3.dp.toPx()
                val width = (item.spanX * cellW - 6.dp.toPx()).coerceAtLeast(2f)
                val height = (item.spanY * cellH - 6.dp.toPx()).coerceAtLeast(2f)
                when (item.type) {
                    DesktopItemType.APP -> {
                        drawCircle(theme.palette.accent, radius = minOf(width, height) * 0.35f,
                            center = Offset(left + width / 2f, top + height / 2f))
                    }
                    DesktopItemType.AILUA_WIDGET -> {
                        drawRoundRect(theme.widgets.backgroundColor, Offset(left, top), Size(width, height),
                            cornerRadius = CornerRadius(4.dp.toPx()))
                        drawRoundRect(theme.palette.accent.copy(alpha = 0.18f), Offset(left, top), Size(width, height),
                            cornerRadius = CornerRadius(4.dp.toPx()))
                    }
                    DesktopItemType.FOLDER -> {
                        drawRoundRect(theme.palette.accent.copy(alpha = 0.4f), Offset(left, top), Size(width, height),
                            cornerRadius = CornerRadius(4.dp.toPx()))
                        val radius = minOf(width, height) * 0.11f
                        listOf(0.32f to 0.32f, 0.68f to 0.32f, 0.32f to 0.68f, 0.68f to 0.68f)
                            .forEach { (x, y) ->
                                drawCircle(theme.palette.onSurface, radius,
                                    Offset(left + width * x, top + height * y))
                            }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSetHomePage, modifier = Modifier.size(34.dp)
                .testTag("page_set_home_${page.id}")) {
                Icon(if (page.isHome) Icons.Default.Star else Icons.Default.StarBorder,
                    if (page.isHome) "当前主屏" else "设为主屏",
                    tint = if (page.isHome) theme.palette.accent else theme.palette.onSurfaceMuted,
                    modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDeletePage, modifier = Modifier.size(34.dp)
                .testTag("page_delete_${page.id}")) {
                Icon(Icons.Default.DeleteOutline, "删除页面", tint = theme.palette.onSurfaceMuted,
                    modifier = Modifier.size(19.dp))
            }
        }
    }
}
