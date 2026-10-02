package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.DayPhase
import com.example.data.model.WeatherState
import com.example.ui.themeengine.LocalAiluaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerSheet(
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = LocalAiluaTheme.current.surfaces.raised, modifier = Modifier.testTag("theme_picker_sheet"),
    ) {
        HideDialogStatusBar()
        ThemePickerContent(selectedId = selectedId, onSelect = { onSelect(it); onDismiss() })
    }
}

@Composable
fun ThemePickerContent(selectedId: String, onSelect: (String) -> Unit) {
    val visual = LocalAiluaTheme.current
    val followPreview = wallpaperPalette(DayPhase.EVENING, WeatherState.RAIN, false).colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = visual.layout.screenHorizontalPadding.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("桌面配色", style = visual.text.title, color = visual.palette.onSurface)
        Text("长按桌面空白处可随时更换", style = visual.text.secondary, color = visual.palette.onSurfaceMuted)
        HomeThemeCatalog.themes.forEach { theme ->
            val swatches = if (theme.followsWorldClock) followPreview else theme.lightColors
            val selected = theme.id == selectedId
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(visual.shapes.medium.dp))
                    .background(if (selected) visual.palette.accent.copy(alpha = 0.16f) else visual.surfaces.inset)
                    .clickable { onSelect(theme.id) }.padding(horizontal = 14.dp, vertical = 14.dp)
                    .testTag("theme_option_${theme.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    swatches.forEach { color ->
                        Box(Modifier.size(18.dp).clip(CircleShape).background(color))
                    }
                }
                Text(theme.name, style = visual.text.body, color = visual.palette.onSurface, modifier = Modifier.weight(1f))
                if (selected) Icon(Icons.Default.Check, "当前主题", Modifier.size(18.dp), tint = visual.palette.onSurface)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
