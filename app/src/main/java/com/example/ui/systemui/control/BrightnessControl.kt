package com.example.ui.systemui.control

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.themeengine.LocalAiluaTheme
import kotlin.math.roundToInt

@Composable
fun BrightnessControl(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalAiluaTheme.current.palette
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Brightness6, null, tint = palette.onSurface)
            Spacer(Modifier.width(10.dp))
            Text("屏幕亮度", color = palette.onSurface, style = LocalAiluaTheme.current.text.body)
            Spacer(Modifier.weight(1f))
            Text("${(value * 100).roundToInt()}%", color = palette.onSurfaceMuted, style = LocalAiluaTheme.current.text.secondary)
        }
        Slider(
            value = value.coerceIn(0.5f, 1f), onValueChange = onValueChange, valueRange = 0.5f..1f,
            colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent,
                inactiveTrackColor = palette.border),
            modifier = Modifier.fillMaxWidth().testTag("control_brightness"),
        )
    }
}
