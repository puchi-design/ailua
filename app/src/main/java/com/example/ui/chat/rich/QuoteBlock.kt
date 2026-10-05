package com.example.ui.chat.rich

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun QuoteBlock(preview: String, author: String, modifier: Modifier = Modifier) {
    val theme = LocalAiluaTheme.current
    Row(
        modifier = modifier.fillMaxWidth().testTag("chat_quote_block"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.foundation.layout.Spacer(
            Modifier.width(3.dp).height(38.dp).background(theme.palette.accent.copy(alpha = 0.72f)),
        )
        Column {
            Text("回复 $author", style = theme.text.caption, color = theme.palette.onSurfaceMuted)
            Text(
                preview,
                style = theme.text.secondary,
                color = theme.palette.onSurfaceMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun QuoteComposerBanner(
    preview: String,
    author: String,
    onClear: () -> Unit,
) {
    val theme = LocalAiluaTheme.current
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = theme.layout.screenHorizontalPadding.dp)
            .testTag("chat_quote_composer"),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        QuoteBlock(
            preview = preview,
            author = author,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
        )
        TextButton(onClick = onClear, modifier = Modifier.testTag("chat_quote_clear")) {
            Text("取消")
        }
    }
}
