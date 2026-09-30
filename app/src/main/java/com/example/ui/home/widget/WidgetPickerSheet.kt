package com.example.ui.home.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.home.WidgetRegistry
import com.example.ui.home.WidgetSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(onDismiss: () -> Unit, onAdd: (String, WidgetSize) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("添加桌面组件", style = MaterialTheme.typography.titleLarge)
            WidgetRegistry.defaultOrder.forEach { id ->
                val spec = WidgetRegistry.resolve(id.stableId)
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(16.dp)).padding(14.dp)) {
                    Text(spec.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        spec.supportedSizes.forEach { size ->
                            Column(Modifier.width(82.dp).height(66.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                                .clickable { onAdd(id.stableId, size) }
                                .padding(7.dp).testTag("add_${id.stableId}_${size.spanX}x${size.spanY}")) {
                                Text("${size.spanX}×${size.spanY}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary)
                                Text(spec.title, style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
