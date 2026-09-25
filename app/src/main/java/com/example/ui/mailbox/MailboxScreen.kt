package com.example.ui.mailbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.model.Letter
import com.example.data.model.LetterDeliveryState
import com.example.data.model.LetterType
import com.example.data.repository.MailboxRepository
import com.example.ui.components.AiluaAvatar
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.components.WorldTimeDevSheet
import com.example.ui.theme.AiluaDustyRose
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailboxScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {},
    onReplyInChat: (String) -> Unit = {}
) {
    val letters by MailboxRepository.letters.collectAsState()
    val clock by WorldHeartbeatEngine.worldClock.collectAsState()
    var selectedCategory by remember { mutableStateOf("未读来信") }
    var viewingLetter by remember { mutableStateOf<Letter?>(null) }
    var showDevTimeSheet by remember { mutableStateOf(false) }

    val categories = listOf("未读来信", "已读", "明信片", "小纸条", "全部")

    val filteredLetters = letters.filter { letter ->
        when (selectedCategory) {
            "未读来信" -> letter.deliveryState == LetterDeliveryState.DELIVERED
            "已读" -> letter.deliveryState == LetterDeliveryState.OPENED
            "明信片" -> letter.letterType == LetterType.POSTCARD
            "小纸条" -> letter.letterType == LetterType.NOTE
            else -> true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF131219) else Color(0xFFFBF8F2))
            .testTag("mailbox_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VirtualPhoneStatusBar(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )

            // Mailbox Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("mailbox_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "心网信箱 · Slow Letters",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AiluaMoonGold.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "慢邮时代",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    color = AiluaMoonGold,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    Text(
                        text = "虚拟时间 ${clock.timeFormatted} · 纸短情长的私密手笺",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Time Travel Dev Button
                IconButton(
                    onClick = { showDevTimeSheet = true },
                    modifier = Modifier.testTag("mailbox_time_dev_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "调快虚拟时间",
                        tint = AiluaMoonGold
                    )
                }
            }

            // Category Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    val count = when (cat) {
                        "未读来信" -> letters.count { it.deliveryState == LetterDeliveryState.DELIVERED }
                        "已读" -> letters.count { it.deliveryState == LetterDeliveryState.OPENED }
                        "明信片" -> letters.count { it.letterType == LetterType.POSTCARD }
                        "小纸条" -> letters.count { it.letterType == LetterType.NOTE }
                        else -> letters.size
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) AiluaMoonGold.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                1.dp,
                                if (isSelected) AiluaMoonGold.copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (count > 0 && cat == "未读来信") "$cat ($count)" else cat,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.5.sp,
                                color = if (isSelected) AiluaMoonGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Letter List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredLetters.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Drafts,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "此处暂无信件",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(filteredLetters, key = { it.id }) { letter ->
                    LetterCard(
                        letter = letter,
                        onClick = { viewingLetter = letter }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            VirtualPhoneHomeBar(
                canGoBack = true,
                onBack = onBack,
                onGoHome = onBack
            )
        }

        // Letter Reader Dialog
        viewingLetter?.let { letter ->
            LetterReaderDialog(
                letter = letter,
                onDismiss = { viewingLetter = null },
                onMarkOpened = {
                    MailboxRepository.markOpened(letter.id)
                },
                onReplyInChat = { charId ->
                    viewingLetter = null
                    onReplyInChat(charId)
                }
            )
        }

        // Virtual Time Dev Sheet
        if (showDevTimeSheet) {
            WorldTimeDevSheet(onDismiss = { showDevTimeSheet = false })
        }
    }
}

@Composable
private fun LetterCard(
    letter: Letter,
    onClick: () -> Unit
) {
    val isScheduled = letter.deliveryState == LetterDeliveryState.SCHEDULED
    val isUnread = letter.deliveryState == LetterDeliveryState.DELIVERED

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Color(letter.paperColorHex))
            .border(
                1.dp,
                if (isUnread) AiluaMoonGold.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("letter_card_${letter.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AiluaAvatar(
                        avatarId = letter.characterId,
                        size = 32.dp,
                        showHalo = false
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = letter.senderName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = Color(0xFF2C2416)
                        )
                        Text(
                            text = letter.letterType.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                color = Color(0xFF7A6E5D)
                            )
                        )
                    }
                }

                if (isScheduled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8E0D2))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${letter.deliverAtVirtualTimeString} 后送达",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                color = Color(0xFF8C7E6A),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                } else if (isUnread) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AiluaMoonGold)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "未读新信",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = Color(0xFF2A2110),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else {
                    Text(
                        text = "已拆阅",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            color = Color(0xFFA39582)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = letter.subject,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                ),
                color = Color(0xFF2C2416)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isScheduled) "信笺正由心网寄递中，静候送达时刻…" else letter.body.replace("\n", " "),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                color = if (isScheduled) Color(0xFF998A76) else Color(0xFF554A3A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LetterReaderDialog(
    letter: Letter,
    onDismiss: () -> Unit,
    onMarkOpened: () -> Unit,
    onReplyInChat: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(letter.paperColorHex))
                .border(1.dp, Color(0xFFDED4C5), RoundedCornerShape(22.dp))
                .padding(20.dp)
                .testTag("letter_reader_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header with Wax Seal feeling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AiluaDustyRose.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "✉️", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${letter.senderName} 的手笺",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = Color(0xFF2C2416)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color(0xFF7A6E5D)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = letter.subject,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF2C2416)
                )

                Text(
                    text = "投递时间：${letter.deliverAtVirtualTimeString} · ${letter.letterType.label}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        color = Color(0xFF8C7E6A)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Handwritten body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.5f))
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (letter.deliveryState == LetterDeliveryState.SCHEDULED) {
                            "这是一封预定在 ${letter.deliverAtVirtualTimeString} 送达的慢递信件。\n\n你可以通过右上角推进虚拟时间，等待送达！"
                        } else {
                            letter.body
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 22.sp,
                            fontStyle = FontStyle.Normal
                        ),
                        color = Color(0xFF382E20)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (letter.deliveryState == LetterDeliveryState.DELIVERED) {
                        Button(
                            onClick = {
                                onMarkOpened()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AiluaMoonGold,
                                contentColor = Color(0xFF2A2110)
                            )
                        ) {
                            Text("标为已读", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            onMarkOpened()
                            onReplyInChat(letter.characterId)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AiluaMistBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("去聊天回信", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
