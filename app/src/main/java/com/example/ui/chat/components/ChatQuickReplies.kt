package com.example.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.designsystem.AiluaChip
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun ChatQuickReplies(prompts: List<String>, enabled: Boolean, onSelect: (String) -> Unit) {
    val theme = LocalAiluaTheme.current
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = theme.layout.screenHorizontalPadding.dp),
        horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
    ) {
        items(prompts) { prompt ->
            AiluaChip(label = prompt, onClick = if (enabled) ({ onSelect(prompt) }) else null)
        }
    }
}

@Composable
fun ChatJumpToLatest(visible: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        AiluaChip("回到最新消息", onClick = onClick)
    }
}
