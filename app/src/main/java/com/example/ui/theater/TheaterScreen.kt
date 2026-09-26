package com.example.ui.theater

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AiluaLocalStore
import com.example.data.mock.WorldData
import com.example.data.model.TheaterBookmark
import com.example.data.model.TheaterChoice
import com.example.data.model.TheaterDialogueNode
import com.example.data.model.TheaterHistoryStep
import com.example.data.model.TheaterStory
import com.example.ui.components.AiluaAvatar
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.theme.AiluaDustyRose
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold
import com.example.ui.theme.AiluaMutedLavender
import kotlinx.coroutines.launch

@Composable
fun TheaterScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val story = WorldData.rainyNightStory
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedBookmark by AiluaLocalStore.theaterBookmark.collectAsStateWithLifecycle()

    var currentNodeId by remember { mutableStateOf(story.initialNodeId) }
    var bondScore by remember { mutableIntStateOf(50) }
    var storyVariables by remember { mutableStateOf(story.initialVariables) }
    val history = remember { mutableStateListOf<TheaterHistoryStep>() }

    val currentNode: TheaterDialogueNode? = story.nodes[currentNodeId]

    // Evaluates conditional choice visibility (Phase 1B)
    fun isChoiceAvailable(choice: TheaterChoice): Boolean {
        val reqKey = choice.requiredVariableKey
        val reqVal = choice.requiredVariableValue
        if (reqKey != null && reqVal != null) {
            return storyVariables[reqKey] == reqVal
        }
        return true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("theater_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VirtualPhoneStatusBar(isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme)

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("theater_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = story.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = story.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                // Header Actions: Bookmark & Restart
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Bookmark Save
                    IconButton(
                        onClick = {
                            val bookmark = TheaterBookmark(
                                storyId = story.id,
                                storyTitle = story.title,
                                currentNodeId = currentNodeId,
                                variables = storyVariables,
                                bondScore = bondScore,
                                history = history.toList()
                            )
                            AiluaLocalStore.saveTheaterBookmark(bookmark)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("已保存当前剧目书签进度")
                            }
                        },
                        modifier = Modifier.size(36.dp).testTag("theater_bookmark_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "保存书签",
                            tint = AiluaMoonGold
                        )
                    }

                    // Reset narrative button
                    IconButton(
                        onClick = {
                            currentNodeId = story.initialNodeId
                            bondScore = 50
                            storyVariables = story.initialVariables
                            history.clear()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("已重置并重新开始剧目")
                            }
                        },
                        modifier = Modifier.size(36.dp).testTag("theater_restart_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "重新开始剧目",
                            tint = AiluaMistBlue
                        )
                    }
                }
            }

            // Resume Bookmark Banner (Phase 1C)
            savedBookmark?.let { bookmark ->
                if (bookmark.storyId == story.id && currentNodeId == story.initialNodeId && history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AiluaMoonGold.copy(alpha = 0.18f))
                            .border(0.5.dp, AiluaMoonGold.copy(alpha = 0.4f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("theater_resume_banner")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = AiluaMoonGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "发现已存书签进度 (羁绊: ${bookmark.bondScore})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            Button(
                                onClick = {
                                    currentNodeId = bookmark.currentNodeId
                                    bondScore = bookmark.bondScore
                                    storyVariables = bookmark.variables
                                    history.clear()
                                    history.addAll(bookmark.history)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("已成功恢复上次剧目进度")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AiluaMoonGold,
                                    contentColor = Color(0xFF2C2411)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(28.dp).testTag("theater_resume_btn")
                            ) {
                                Text("恢复进度 (Resume)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Closeness & Mood Gauge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = AiluaDustyRose, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "本幕羁绊亲密度: $bondScore",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (storyVariables["teaShared"] == "true") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AiluaMoonGold.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "温茶共饮 ☕", fontSize = 9.5.sp, color = AiluaMoonGold, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AiluaMistBlue.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Ink 状态分支",
                            fontSize = 10.sp,
                            color = AiluaMistBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Narrative Story Feed
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // History past nodes
                items(history) { hist ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = hist.speakerName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AiluaMistBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = hist.text,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        if (hist.choiceMadeText != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "➜ 你的选择: ${hist.choiceMadeText}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = AiluaMoonGold
                            )
                        }
                    }
                }

                // Current Active Node
                if (currentNode != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AiluaMistBlue.copy(alpha = 0.5f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AiluaAvatar(avatarId = currentNode.avatarId, size = 44.dp, showHalo = true)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = currentNode.speakerName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "心绪: ${currentNode.emotion}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                            color = AiluaDustyRose
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = currentNode.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        lineHeight = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Choices or Ending
                                if (currentNode.isEnding) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AiluaMoonGold.copy(alpha = 0.15f))
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "🎉 剧目达成: ${currentNode.endingTitle ?: "圆满心契"}",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                color = AiluaMoonGold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    currentNodeId = story.initialNodeId
                                                    bondScore = 50
                                                    history.clear()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = AiluaMoonGold)
                                            ) {
                                                Text("再演一次其他分支", color = Color.Black, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "你的回应与分支抉择：",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Filter choices based on condition (Phase 1B)
                                    val availableChoices = currentNode.choices.filter { isChoiceAvailable(it) }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        availableChoices.forEach { choice ->
                                            ChoiceButton(
                                                choice = choice,
                                                onSelect = {
                                                    // Record to history
                                                    history.add(
                                                        TheaterHistoryStep(
                                                            nodeId = currentNode.id,
                                                            speakerName = currentNode.speakerName,
                                                            text = currentNode.text,
                                                            choiceMadeText = choice.text
                                                        )
                                                    )

                                                    // Apply variables
                                                    if (choice.setVariableKey != null && choice.setVariableValue != null) {
                                                        storyVariables = storyVariables + (choice.setVariableKey to choice.setVariableValue)
                                                    }
                                                    bondScore += choice.bondIncrease

                                                    // Transition node
                                                    currentNodeId = choice.targetNodeId
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Snackbar
            SnackbarHost(hostState = snackbarHostState)

            // Bottom bar
            VirtualPhoneHomeBar(canGoBack = true, onBack = onBack, onGoHome = onBack)
        }
    }
}

@Composable
private fun ChoiceButton(
    choice: TheaterChoice,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(
                1.dp,
                if (choice.requiredVariableKey != null) AiluaMoonGold.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .padding(12.dp)
            .testTag("theater_choice_${choice.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (choice.requiredVariableKey != null) {
                    Text(
                        text = "✨ 达成前置条件解锁的特殊选择",
                        fontSize = 10.sp,
                        color = AiluaMoonGold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = choice.text,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+${choice.bondIncrease} 羁绊",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = AiluaDustyRose
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
