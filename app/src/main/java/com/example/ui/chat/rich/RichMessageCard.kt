package com.example.ui.chat.rich

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.chat.rich.RichMessagePayload
import com.example.data.chat.rich.RichMessageStatus
import com.example.data.chat.rich.RichMessageType
import com.example.ui.themeengine.LocalAiluaTheme
import java.util.Locale

/** A deliberately small vocabulary of story cards. No balance or payment service is involved. */
@Composable
fun RichMessageCard(
    payload: RichMessagePayload,
    payloadIndex: Int,
    senderName: String,
    onStatusChange: (Int, RichMessageStatus) -> Unit,
    onOpenLocation: (String) -> Unit,
) {
    val theme = LocalAiluaTheme.current
    val pending = payload.status == RichMessageStatus.PENDING
    val validAmount = payload.amount?.let { it.isFinite() && it > 0.0 } == true
    val cardModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(theme.shapes.medium.dp))
        .background(theme.surfaces.raised)
        .padding(horizontal = 16.dp, vertical = 12.dp)

    when (payload.type) {
        RichMessageType.RED_PACKET -> Column(
            modifier = cardModifier.testTag("rich_red_packet_$payloadIndex"),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("🧧  $senderName 发来一个红包", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
            payload.label?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = theme.text.body, color = theme.palette.onSurface)
            }
            Text(formatVirtualAmount(payload), style = theme.text.title, color = theme.palette.onSurface)
            if (pending && validAmount) {
                TextButton(
                    onClick = { onStatusChange(payloadIndex, RichMessageStatus.OPENED) },
                    modifier = Modifier.testTag("rich_red_packet_open_$payloadIndex"),
                ) { Text("打开") }
            } else {
                Text(
                    if (!validAmount) "金额不可用" else when (payload.status) {
                        RichMessageStatus.OPENED -> "已打开"
                        RichMessageStatus.CANCELED -> "已取消"
                        else -> "暂不可打开"
                    },
                    style = theme.text.caption,
                    color = theme.palette.onSurfaceMuted,
                )
            }
            Text("剧情互动", style = theme.text.caption, color = theme.palette.onSurfaceMuted)
        }

        RichMessageType.TRANSFER -> Column(
            modifier = cardModifier.testTag("rich_transfer_$payloadIndex"),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("$senderName 向你转账", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
            Text(formatVirtualAmount(payload), style = theme.text.title, color = theme.palette.onSurface)
            payload.label?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = theme.text.body, color = theme.palette.onSurface)
            }
            if (pending && validAmount) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onStatusChange(payloadIndex, RichMessageStatus.ACCEPTED) },
                    modifier = Modifier.testTag("rich_transfer_accept_$payloadIndex"),
                ) { Text("收下") }
                TextButton(
                    onClick = { onStatusChange(payloadIndex, RichMessageStatus.DECLINED) },
                    modifier = Modifier.testTag("rich_transfer_decline_$payloadIndex"),
                ) { Text("退回") }
            } else Text(
                if (!validAmount) "金额不可用" else when (payload.status) {
                    RichMessageStatus.ACCEPTED -> "已收下"
                    RichMessageStatus.DECLINED -> "已退回"
                    RichMessageStatus.CANCELED -> "已取消"
                    else -> "暂不可操作"
                },
                style = theme.text.caption,
                color = theme.palette.onSurfaceMuted,
            )
            Text("剧情互动", style = theme.text.caption, color = theme.palette.onSurfaceMuted)
        }

        RichMessageType.GIFT -> Column(
            modifier = cardModifier.testTag("rich_gift_$payloadIndex"),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("🎁  $senderName 送给你", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
            Text(payload.label.orEmpty(), style = theme.text.body, color = theme.palette.onSurface)
            if (pending) TextButton(
                onClick = { onStatusChange(payloadIndex, RichMessageStatus.RECEIVED) },
                modifier = Modifier.testTag("rich_gift_receive_$payloadIndex"),
            ) { Text("收下") }
            else Text(
                if (payload.status == RichMessageStatus.RECEIVED) "已收下" else "暂不可操作",
                style = theme.text.caption,
                color = theme.palette.onSurfaceMuted,
            )
        }

        RichMessageType.LOCATION -> {
            val place = payload.locationName?.takeIf { it.isNotBlank() }
                ?: payload.label.orEmpty()
            Column(
                modifier = cardModifier
                    .clickable(enabled = place.isNotBlank()) { onOpenLocation(place) }
                    .testTag("rich_location_$payloadIndex"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("📍  $place", style = theme.text.body, color = theme.palette.onSurface)
                Text("$senderName 分享了位置 · 查看地点", style = theme.text.caption, color = theme.palette.onSurfaceMuted)
            }
        }

        RichMessageType.STICKER -> StickerMessage(payload.iconKey ?: payload.label.orEmpty(), payloadIndex)
        RichMessageType.TEXT, RichMessageType.QUOTE -> Unit
    }
}

@Composable
private fun StickerMessage(key: String, payloadIndex: Int) {
    val theme = LocalAiluaTheme.current
    val context = LocalContext.current
    val resource = when (key) {
        "偷看" -> R.string.rich_sticker_peek
        "无语" -> R.string.rich_sticker_speechless
        "开心" -> R.string.rich_sticker_happy
        "害羞" -> R.string.rich_sticker_shy
        "生气" -> R.string.rich_sticker_angry
        "晚安" -> R.string.rich_sticker_goodnight
        "抱抱" -> R.string.rich_sticker_hug
        "委屈" -> R.string.rich_sticker_sad
        else -> null
    }
    Column(modifier = Modifier.testTag("rich_sticker_$payloadIndex")) {
        Text(
            text = resource?.let { context.getString(it) } ?: "🙂",
            style = theme.text.display,
        )
        Text(key, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
    }
}

private fun formatVirtualAmount(payload: RichMessagePayload): String {
    val currency = payload.currency?.ifBlank { null } ?: "¥"
    val value = payload.amount?.takeIf { it.isFinite() && it > 0.0 } ?: return "金额不可用"
    return "$currency${String.format(Locale.ROOT, "%.2f", value)}"
}
