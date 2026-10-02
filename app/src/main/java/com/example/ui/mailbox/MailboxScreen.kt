package com.example.ui.mailbox

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Letter
import com.example.data.model.LetterDeliveryState
import com.example.data.repository.MailboxRepository
import com.example.ui.components.WorldTimeDevSheet
import com.example.ui.designsystem.AiluaChip
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.designsystem.AiluaSurface
import com.example.ui.themeengine.LocalAiluaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailboxScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {},
    onReplyInChat: (String) -> Unit = {},
    onGoHome: () -> Unit = onBack
) {
    val theme = LocalAiluaTheme.current
    val context = LocalContext.current
    val devEnabled = remember(context) {
        context.getSharedPreferences("ailua_settings", Context.MODE_PRIVATE).getBoolean("developer", false)
    }
    val letters by MailboxRepository.letters.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf("未读") }
    var viewingLetter by remember { mutableStateOf<Letter?>(null) }
    var showDevTimeSheet by remember { mutableStateOf(false) }
    val categories = listOf("未读", "全部", "待送达")
    val filteredLetters = letters.filter { letter ->
        when (selectedCategory) {
            "未读" -> letter.deliveryState == LetterDeliveryState.DELIVERED
            "待送达" -> letter.deliveryState == LetterDeliveryState.SCHEDULED
            else -> true
        }
    }

    AiluaScreenScaffold(
        title = "信箱",
        onBack = onBack,
        onGoHome = onGoHome,
        modifier = Modifier.testTag("mailbox_screen"),
        backTestTag = "mailbox_back_btn",
        trailing = {
            if (devEnabled) {
                IconButton(onClick = { showDevTimeSheet = true }, modifier = Modifier.testTag("mailbox_time_dev_btn")) {
                    Icon(Icons.Default.Schedule, "调整世界时间", tint = theme.palette.onSurfaceMuted)
                }
            }
        }
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp, vertical = theme.layout.itemGap.dp),
            horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)
        ) {
            items(categories) { category ->
                val count = letters.count { it.deliveryState == LetterDeliveryState.DELIVERED }
                AiluaChip(
                    label = if (category == "未读" && count > 0) "未读 $count" else category,
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category }
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp)
        ) {
            if (filteredLetters.isEmpty()) {
                item {
                    Text("暂无信件", modifier = Modifier.padding(vertical = theme.layout.sectionGap.dp),
                        style = theme.text.body, color = theme.palette.onSurfaceMuted)
                }
            }
            items(filteredLetters, key = { it.id }) { letter ->
                LetterCard(letter = letter, onClick = { viewingLetter = letter })
                HorizontalDivider(color = theme.surfaces.divider)
            }
            item { Spacer(Modifier.height(theme.layout.sectionGap.dp)) }
        }
    }
    viewingLetter?.let { letter ->
        LetterReaderDialog(
            letter = letter,
            onDismiss = { viewingLetter = null },
            onMarkOpened = { MailboxRepository.markOpened(letter.id) },
            onReplyInChat = { charId ->
                viewingLetter = null
                onReplyInChat(charId)
            }
        )
    }
    if (devEnabled && showDevTimeSheet) {
        WorldTimeDevSheet(onDismiss = { showDevTimeSheet = false })
    }
}

@Composable
private fun LetterCard(letter: Letter, onClick: () -> Unit) {
    val theme = LocalAiluaTheme.current
    val isScheduled = letter.deliveryState == LetterDeliveryState.SCHEDULED
    val isUnread = letter.deliveryState == LetterDeliveryState.DELIVERED
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(vertical = theme.layout.sectionGap.dp).testTag("letter_card_${letter.id}"),
        verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(letter.senderName, modifier = Modifier.weight(1f), style = theme.text.section, color = theme.palette.onSurface)
            Text(
                if (isScheduled) "待送达" else if (isUnread) "未读" else "已读",
                style = theme.text.caption,
                color = if (isUnread) theme.palette.accent else theme.palette.onSurfaceMuted
            )
        }
        Text(letter.subject, style = theme.text.body, color = theme.palette.onSurface)
        Text(
            if (isScheduled) "等待送达" else letter.body.replace("\n", " "),
            style = theme.text.secondary, color = theme.palette.onSurfaceMuted,
            maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        Text(letter.deliverAtVirtualTimeString, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
    }
}

@Composable
private fun LetterReaderDialog(
    letter: Letter,
    onDismiss: () -> Unit,
    onMarkOpened: () -> Unit,
    onReplyInChat: (String) -> Unit
) {
    val theme = LocalAiluaTheme.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            AiluaSurface(modifier = Modifier.fillMaxWidth().fillMaxSize(0.94f).testTag("letter_reader_dialog")) {
                Column(modifier = Modifier.fillMaxSize().padding(theme.layout.screenHorizontalPadding.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(letter.senderName, style = theme.text.secondary, color = theme.palette.onSurfaceMuted, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭", tint = theme.palette.onSurfaceMuted) }
                    }
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = theme.layout.sectionGap.dp),
                        verticalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp)
                    ) {
                        Text(letter.subject, style = theme.text.title, color = theme.palette.onSurface)
                        Text("${letter.deliverAtVirtualTimeString} · ${letter.letterType.label}", style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                        Text(
                            if (letter.deliveryState == LetterDeliveryState.SCHEDULED) {
                                "这封信将在 ${letter.deliverAtVirtualTimeString} 送达。"
                            } else letter.body,
                            style = theme.text.body,
                            color = theme.palette.onSurface
                        )
                        if (letter.deliveryState != LetterDeliveryState.SCHEDULED) {
                            Text("——${letter.senderName}", modifier = Modifier.align(Alignment.End), style = theme.text.body, color = theme.palette.onSurfaceMuted)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                        if (letter.deliveryState == LetterDeliveryState.DELIVERED) {
                            AiluaChip(
                                label = "标为已读", modifier = Modifier.weight(1f),
                                onClick = { onMarkOpened(); onDismiss() }
                            )
                        }
                        AiluaChip(
                            label = "去聊天回信", selected = true, modifier = Modifier.weight(1f),
                            onClick = { onMarkOpened(); onReplyInChat(letter.characterId) }
                        )
                    }
                }
            }
        }
    }
}
