package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AiluaMoonGold
import com.example.ui.theme.AiluaMutedLavender
import com.example.ui.themeengine.LocalAiluaTheme

/** Virtual phone chrome follows the same runtime as the desktop. */
@Composable
fun VirtualPhoneStatusBar(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    state: OsChromeState = LocalOsChromeState.current
) {
    val runtime = LocalAiluaTheme.current
    val spec = runtime.statusBar
    val textColor = spec.foregroundColor
    val subtleColor = textColor.copy(alpha = 0.66f)

    Row(
        modifier = modifier.fillMaxWidth()
            .background(runtime.palette.surface.copy(alpha = spec.backgroundAlpha))
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = state.timeLabel,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                ),
                color = textColor,
                modifier = Modifier.testTag("virtual_status_time")
            )
            if (!spec.minimal) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(runtime.palette.surface.copy(alpha = 0.65f))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier.size(5.dp).clip(CircleShape)
                            .background(AiluaMoonGold)
                    )
                    Text(
                        text = "AILUA OS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp, fontWeight = FontWeight.Medium
                        ),
                        color = subtleColor
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!spec.minimal) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "心网连接正常",
                        modifier = Modifier.size(13.dp),
                        tint = textColor
                    )
                    Text(
                        text = state.networkLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = subtleColor
                    )
                }

                Row(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(runtime.palette.surface.copy(alpha = 0.5f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = state.batteryLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp, fontWeight = FontWeight.Medium
                        ),
                        color = textColor
                    )
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape)
                            .background(Brush.sweepGradient(listOf(AiluaMutedLavender, AiluaMoonGold)))
                    )
                }
            }
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleTheme
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "切换虚拟世界心境主题",
                    modifier = Modifier.size(14.dp),
                    tint = textColor
                )
            }
        }
    }
}

@Composable
fun VirtualPhoneHomeBar(
    modifier: Modifier = Modifier,
    canGoBack: Boolean = false,
    onBack: () -> Unit = {},
    onGoHome: () -> Unit = {}
) {
    val color = LocalAiluaTheme.current.statusBar.foregroundColor
    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp, top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (canGoBack) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp).testTag("virtual_home_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回上一页",
                        modifier = Modifier.size(18.dp),
                        tint = color.copy(alpha = 0.7f)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }
            Box(
                modifier = Modifier.width(72.dp).height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color.copy(alpha = 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onGoHome
                    )
                    .testTag("virtual_phone_home_indicator")
            )
            Spacer(modifier = Modifier.size(36.dp))
        }
    }
}