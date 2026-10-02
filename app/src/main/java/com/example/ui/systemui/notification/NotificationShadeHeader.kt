package com.example.ui.systemui.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LocalOsChromeState
import com.example.ui.themeengine.LocalAiluaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationShadeHeader(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val chrome = LocalOsChromeState.current
    val palette = LocalAiluaTheme.current.palette
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(chrome.timeLabel, color = palette.onSurface, fontSize = 34.sp, fontWeight = FontWeight.Light)
            Text(
                SimpleDateFormat("M月d日 · EEEE", Locale.CHINA).format(Date()),
                color = palette.onSurfaceMuted, fontSize = 12.sp,
            )
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "收起通知中心", tint = palette.onSurface)
        }
    }
}
