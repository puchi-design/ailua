package com.example.ui.reality

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AiluaTheme

/** Health Connect permission rationale opened by the system's privacy-policy link. */
class RealityPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AiluaTheme {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("AILUA 现实感知", style = MaterialTheme.typography.headlineSmall)
                    Text("在你主动开启并授权后，AILUA 仅在本机读取今日步数和最近一次睡眠时长，用于生成简短、低频的陪伴情境。原始健康记录不写入聊天、世界事件或云端。你可以随时在现实感知页面关闭，或在 Health Connect 中撤销授权。")
                    Button(onClick = { finish() }) { Text("返回") }
                }
            }
        }
    }
}
