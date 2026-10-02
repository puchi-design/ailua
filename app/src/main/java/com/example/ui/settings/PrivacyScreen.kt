package com.example.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onGoHome: () -> Unit = onBack
) {
    val theme = LocalAiluaTheme.current
    AiluaScreenScaffold(title = "隐私", onBack = onBack, onGoHome = onGoHome) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(theme.layout.screenHorizontalPadding.dp),
            verticalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp)
        ) {
            Text("聊天、角色、记忆和虚拟世界记录保存在本机。API Key 使用 Android Keystore 加密保存。", style = theme.text.body, color = theme.palette.onSurface)
            Text("使用统计和 Health Connect 仅在你主动开启并授权后读取；未授权时保持未知。可随时在现实连接中关闭。", style = theme.text.body, color = theme.palette.onSurface)
            Text("与远程 AI 聊天时，必要的对话、角色及有限上下文会发送给你选择的 AI 服务商，受其服务条款约束。现实环境只会以低敏摘要按需加入上下文。", style = theme.text.body, color = theme.palette.onSurface)
            Text("AILUA 当前没有账号或云同步。", style = theme.text.body, color = theme.palette.onSurface)
        }
    }
}
