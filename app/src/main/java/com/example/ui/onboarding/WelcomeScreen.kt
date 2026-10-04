package com.example.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ai.repository.ProviderGraph
import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.context.CharacterContext
import com.example.data.model.CharacterProfile
import com.example.data.registry.CharacterRegistry
import com.example.ui.components.AiConnectionSheet
import com.example.ui.designsystem.AiluaChip
import com.example.ui.designsystem.AiluaSectionHeader
import com.example.ui.designsystem.AiluaSurface
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.themeengine.LocalAiluaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(onFinish: (String, Boolean) -> Unit) {
    val theme = LocalAiluaTheme.current
    var step by remember { mutableIntStateOf(0) }
    var showConnection by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf(CharacterContext.currentId()) }
    var showMore by remember { mutableStateOf(selectedId !in (CharacterRegistry.OFFICIAL_ROMANCE_IDS + CharacterRegistry.OFFICIAL_FRIENDSHIP_IDS)) }
    val official = CharacterRegistry.getOfficialRomanceCharacters()
    val friends = CharacterRegistry.getOfficialFriendshipCharacters()
    val others = CharacterRegistry.getAllCharacters().filterNot {
        it.id in CharacterRegistry.OFFICIAL_ROMANCE_IDS || it.id in CharacterRegistry.OFFICIAL_FRIENDSHIP_IDS
    }
    val connected = ProviderGraph.repository.activeProfile() != null

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(theme.layout.screenHorizontalPadding.dp).testTag("welcome_screen"),
        verticalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp)
    ) {
        Spacer(Modifier.height(theme.layout.sectionGap.dp))
        Text("AILUA", style = theme.text.display, color = theme.palette.onSurface)
        Text("你的另一部手机", style = theme.text.section, color = theme.palette.onSurfaceMuted)
        when (step) {
            0 -> {
                Text("在这里，认识一个有自己生活的人。你们可以聊天，也可以慢慢走近。", style = theme.text.body, color = theme.palette.onSurface)
                AiluaChip(label = "开始", selected = true, onClick = { step = 1 })
            }
            1 -> {
                AiluaSectionHeader("连接 AI")
                Text("聊天需要你选择一个 AI 服务。未连接时，也可以先看看这里的生活。", style = theme.text.body, color = theme.palette.onSurface)
                Text("AILUA 官方服务 · 即将开放", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                AiluaChip(label = "自带 API Key", onClick = { showConnection = true })
                if (connected) Text("已保存 AI 连接，可以继续。", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                AiluaChip(label = if (connected) "继续" else "暂时跳过", selected = true, onClick = { step = 2 })
            }
            2 -> {
                Text("今晚，你想先认识谁？", style = theme.text.title, color = theme.palette.onSurface)
                AiluaSectionHeader("心动对象")
                official.forEach { character ->
                    WelcomeCharacterRow(character, selectedId == character.id) { selectedId = character.id }
                }
                AiluaSectionHeader("我的朋友")
                Text("一起聊天、分享生活，也可以在群聊里见面。", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                friends.forEach { character ->
                    WelcomeCharacterRow(character, selectedId == character.id) { selectedId = character.id }
                }
                if (others.isNotEmpty()) {
                    AiluaSectionHeader("其他角色", actionLabel = if (showMore) "收起" else "展开", onAction = { showMore = !showMore })
                }
                if (showMore && others.isNotEmpty()) {
                    others.forEach { character ->
                        WelcomeCharacterRow(character, selectedId == character.id) { selectedId = character.id }
                    }
                    if (selectedId !in (official + friends + others).map { it.id }) {
                        WelcomeCharacterRow(CharacterRegistry.getCharacter(selectedId), selected = true) {}
                    }
                }
                Text("也可以导入或创建自己的角色，性别与关系由你决定。", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                    AiluaChip(label = "导入角色卡", onClick = { onFinish(selectedId, true) }, modifier = Modifier.testTag("welcome_import"))
                    AiluaChip(label = "继续", selected = true, onClick = { step = 3 })
                }
            }
            else -> {
                val character = CharacterRegistry.getCharacter(selectedId)
                CharacterPortrait(character.id, PortraitVariant.PROFILE, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("你和${character.name}的故事，从这里开始。", style = theme.text.title, color = theme.palette.onSurface)
                Text(if (connected) "去打个招呼吧。" else "你可以先看看这里，之后再连接 AI。", style = theme.text.body, color = theme.palette.onSurfaceMuted)
                AiluaChip(
                    label = if (connected) "开始聊天" else "进入 AILUA", selected = true,
                    onClick = { onFinish(selectedId, false) }, modifier = Modifier.testTag("welcome_finish")
                )
            }
        }
        Spacer(Modifier.height(theme.layout.sectionGap.dp))
    }
    if (showConnection) AiConnectionSheet(
        onDismiss = { showConnection = false },
        onConnected = { showConnection = false; step = 2 }
    )
}

@Composable
private fun WelcomeCharacterRow(character: CharacterProfile, selected: Boolean, onSelect: () -> Unit) {
    val theme = LocalAiluaTheme.current
    val runtime = CharacterRuntimeResolver.resolve(character.id)
    val signature = if (CharacterRegistry.isUnmodifiedBuiltIn(character.id)) {
        welcomeSignatures[character.id] ?: character.contextualQuote
    } else character.contextualQuote
    AiluaSurface(modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect).testTag("welcome_character_${character.id}")) {
        Row(
            Modifier.fillMaxWidth().padding(theme.layout.itemGap.dp),
            horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            CharacterPortrait(character.id, PortraitVariant.AVATAR, Modifier.size(64.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(listOfNotNull(character.name, runtime.identity.age?.let { "$it 岁" }).joinToString(" · "),
                    style = theme.text.section, color = theme.palette.onSurface)
                Text(character.title, style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                Text("“$signature”", style = theme.text.secondary, color = theme.palette.onSurface)
            }
            if (selected) Text("已选", style = theme.text.caption, color = theme.palette.accent)
        }
    }
}

private val welcomeSignatures = mapOf(
    "hewenchuan" to "楼下。下来拿东西。",
    "zhoujianye" to "你五分钟没回我了，不会真睡了吧？",
    "peixubai" to "有段声音想给你听。",
    "mira" to "我刚烤多了一份，过来拿。",
    "yuna" to "今天出门吗？我找到一家你会喜欢的店。",
    "noa" to "先喝咖啡，再把事情说清楚。",
)
