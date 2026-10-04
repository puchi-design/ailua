package com.example.ui.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun AiluaTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    onTitleClick: (() -> Unit)? = null,
    backTestTag: String? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val theme = LocalAiluaTheme.current
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp)
            .padding(start = if (onBack == null) theme.layout.screenHorizontalPadding.dp else 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.then(backTestTag?.let { Modifier.testTag(it) } ?: Modifier)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", Modifier.size(22.dp), tint = theme.palette.onSurface)
            }
        }
        leading?.invoke()
        Column(
            Modifier.weight(1f)
                .then(if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier)
                .padding(vertical = 10.dp, horizontal = 4.dp),
        ) {
            Text(title, style = theme.text.title, color = theme.palette.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = theme.text.secondary, color = theme.palette.onSurfaceMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing()
    }
}
