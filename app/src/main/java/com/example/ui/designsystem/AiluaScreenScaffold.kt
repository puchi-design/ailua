package com.example.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.themeengine.LocalAiluaTheme

/** Native insets belong to VirtualSystemUiHost. This shell owns only virtual chrome. */
@Composable
fun AiluaScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    onGoHome: () -> Unit = {},
    backTestTag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().background(LocalAiluaTheme.current.surfaces.screen)) {
        VirtualPhoneStatusBar()
        if (topBar != null) topBar() else AiluaTopBar(
            title, onBack = onBack, subtitle = subtitle, trailing = trailing, backTestTag = backTestTag,
        )
        Column(Modifier.weight(1f).fillMaxWidth(), content = content)
        bottomBar?.invoke()
        VirtualPhoneHomeBar(onGoHome = onGoHome)
    }
}
