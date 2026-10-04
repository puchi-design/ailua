package com.example.ui.theater

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AiluaLocalStore
import com.example.data.mock.WorldData
import com.example.data.model.TheaterBookmark
import com.example.data.model.TheaterChoice
import com.example.data.model.TheaterDialogueNode
import com.example.data.model.TheaterHistoryStep
import com.example.data.relationship.romance.RomanceRepository
import com.example.ui.designsystem.AiluaChip
import com.example.ui.designsystem.AiluaMediaFrame
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.designsystem.AiluaSectionHeader
import com.example.ui.designsystem.AiluaSurface
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.designsystem.publicCharacterName
import com.example.ui.themeengine.LocalAiluaTheme
import kotlinx.coroutines.launch

@Composable
fun TheaterScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {},
    onGoHome: () -> Unit = onBack
) {
    val theme = LocalAiluaTheme.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedBookmark by AiluaLocalStore.theaterBookmark.collectAsStateWithLifecycle()
    val legacyBookmark = savedBookmark?.takeIf {
        it.storyId == WorldData.rainyNightStory.id && it.history.isNotEmpty()
    }
    var readingLegacy by remember { mutableStateOf(false) }
    val story = if (readingLegacy && legacyBookmark != null) WorldData.rainyNightStory else WorldData.defaultTheaterStory
    var currentNodeId by remember(story.id) { mutableStateOf(story.initialNodeId) }
    var bondScore by remember(story.id) { mutableIntStateOf(50) }
    var storyVariables by remember(story.id) { mutableStateOf(story.initialVariables) }
    val history = remember(story.id) { mutableStateListOf<TheaterHistoryStep>() }
    var showHistory by remember(story.id) { mutableStateOf(false) }
    val currentNode: TheaterDialogueNode? = story.nodes[currentNodeId]
    LaunchedEffect(story.id, currentNodeId) {
        if (currentNode?.isEnding == true && history.isNotEmpty()) {
            history.mapNotNull { story.nodes[it.nodeId]?.avatarId }.distinct().forEach { characterId ->
                RomanceRepository.recordSharedStory(characterId, story.id)
            }
        }
    }

    fun isChoiceAvailable(choice: TheaterChoice): Boolean {
        val reqKey = choice.requiredVariableKey
        val reqVal = choice.requiredVariableValue
        if (reqKey != null && reqVal != null) return storyVariables[reqKey] == reqVal
        return true
    }

    AiluaScreenScaffold(
        title = "剧场",
        onBack = onBack,
        onGoHome = onGoHome,
        modifier = Modifier.testTag("theater_screen"),
        backTestTag = "theater_back_btn",
        bottomBar = { SnackbarHost(snackbarHostState) },
        trailing = {
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
                    coroutineScope.launch { snackbarHostState.showSnackbar("已保存书签") }
                },
                modifier = Modifier.testTag("theater_bookmark_btn")
            ) { Icon(Icons.Default.BookmarkBorder, "保存书签", tint = theme.palette.onSurfaceMuted) }
            IconButton(
                onClick = {
                    currentNodeId = story.initialNodeId
                    bondScore = 50
                    storyVariables = story.initialVariables
                    history.clear()
                    coroutineScope.launch { snackbarHostState.showSnackbar("已重新开始剧目") }
                },
                modifier = Modifier.testTag("theater_restart_btn")
            ) { Icon(Icons.Default.Refresh, "重新开始剧目", tint = theme.palette.onSurfaceMuted) }
        }
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp),
            verticalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp)
        ) {
            if (legacyBookmark != null) {
                item {
                    Row(
                        Modifier.fillMaxWidth().testTag("theater_legacy_story_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
                    ) {
                        Text(if (readingLegacy) "已保存的旧故事" else "你还有一段已保存的故事",
                            style = theme.text.secondary, color = theme.palette.onSurfaceMuted, modifier = Modifier.weight(1f))
                        AiluaChip(
                            label = if (readingLegacy) "回到新故事" else "打开旧故事",
                            modifier = Modifier.testTag("theater_legacy_story_btn"),
                            onClick = { readingLegacy = !readingLegacy },
                        )
                    }
                }
            }
            savedBookmark?.let { bookmark ->
                if (bookmark.storyId == story.id && bookmark.currentNodeId in story.nodes && currentNodeId == story.initialNodeId && history.isEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().testTag("theater_resume_banner"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)
                        ) {
                            Text("上次的故事还在这里", style = theme.text.secondary, color = theme.palette.onSurfaceMuted, modifier = Modifier.weight(1f))
                            AiluaChip(
                                label = "继续阅读",
                                modifier = Modifier.testTag("theater_resume_btn"),
                                onClick = {
                                    currentNodeId = bookmark.currentNodeId
                                    bondScore = bookmark.bondScore
                                    storyVariables = bookmark.variables
                                    history.clear()
                                    history.addAll(bookmark.history)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("已恢复剧目进度") }
                                }
                            )
                        }
                    }
                }
            }
            if (currentNode != null) {
                item {
                    AiluaMediaFrame(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(theme.layout.screenHorizontalPadding.dp),
                            verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)
                        ) {
                            Text(story.title, style = theme.text.section, color = theme.palette.onSurface)
                            Text(story.subtitle, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                            CharacterPortrait(
                                characterId = currentNode.avatarId,
                                variant = PortraitVariant.HERO,
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                        Text(publicCharacterName(currentNode.speakerId, currentNode.speakerName), style = theme.text.section, color = theme.palette.onSurface)
                        Text(currentNode.emotion, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                        Text(currentNode.text, style = theme.text.body, color = theme.palette.onSurface)
                    }
                }
                if (currentNode.isEnding) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                            AiluaSectionHeader(title = currentNode.endingTitle ?: "故事落幕")
                            Text(
                                "这段共同经历，已经留在故事里。",
                                style = theme.text.secondary, color = theme.palette.onSurfaceMuted
                            )
                            AiluaChip(
                                label = "再演一次其他分支",
                                onClick = {
                                    currentNodeId = story.initialNodeId
                                    bondScore = 50
                                    history.clear()
                                }
                            )
                        }
                    }
                } else {
                    val availableChoices = currentNode.choices.filter { isChoiceAvailable(it) }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                            availableChoices.forEach { choice ->
                                ChoiceButton(
                                    choice = choice,
                                    onSelect = {
                                        history.add(
                                            TheaterHistoryStep(
                                                nodeId = currentNode.id,
                                                speakerName = currentNode.speakerName,
                                                text = currentNode.text,
                                                choiceMadeText = choice.text
                                            )
                                        )
                                        if (choice.setVariableKey != null && choice.setVariableValue != null) {
                                            storyVariables = storyVariables + (choice.setVariableKey to choice.setVariableValue)
                                        }
                                        bondScore += choice.bondIncrease
                                        currentNodeId = choice.targetNodeId
                                    }
                                )
                            }
                        }
                    }
                }
            }
            if (history.isNotEmpty()) {
                item {
                    AiluaSectionHeader(
                        title = "此前剧情",
                        actionLabel = if (showHistory) "收起" else "展开",
                        onAction = { showHistory = !showHistory }
                    )
                }
                if (showHistory) {
                    items(history) { step ->
                        Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                            Text(publicCharacterName(story.nodes[step.nodeId]?.speakerId.orEmpty(), step.speakerName),
                                style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                            Text(step.text, style = theme.text.body, color = theme.palette.onSurface)
                            step.choiceMadeText?.let {
                                Text("你的选择：$it", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(theme.layout.sectionGap.dp)) }
        }
    }
}

@Composable
private fun ChoiceButton(choice: TheaterChoice, onSelect: () -> Unit) {
    val theme = LocalAiluaTheme.current
    AiluaSurface(modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect).testTag("theater_choice_${choice.id}")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(theme.layout.screenHorizontalPadding.dp),
            horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(choice.text, style = theme.text.body, color = theme.palette.onSurface, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = theme.palette.onSurfaceMuted)
        }
    }
}
