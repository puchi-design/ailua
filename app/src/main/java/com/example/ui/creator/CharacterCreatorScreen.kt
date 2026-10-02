package com.example.ui.creator

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.codec.CharacterCardJsonCodec
import com.example.data.codec.CharacterCardValidationResult
import com.example.data.mock.WorldData
import com.example.data.model.CharacterCard
import com.example.data.model.CharacterCardData
import com.example.data.registry.CharacterRegistry
import com.example.ui.designsystem.AiluaChip
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.designsystem.AiluaSectionHeader
import com.example.ui.designsystem.AiluaSurface
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.themeengine.LocalAiluaTheme
import kotlinx.coroutines.launch

@Composable
fun CharacterCreatorScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {},
    onPreviewCharacter: (String) -> Unit = {},
    onGoHome: () -> Unit = onBack
) {
    val theme = LocalAiluaTheme.current
    var showAdvanced by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form states
    var characterId by remember { mutableStateOf("custom_luna") }
    var characterName by remember { mutableStateOf("露娜 (Luna)") }
    var description by remember { mutableStateOf("星空天文学者，栖息于旧天文台的安静女孩，习惯在静谧深夜记录流星轨迹。") }
    var personality by remember { mutableStateOf("理性从容，内敛而浪漫，对未知星图充满好奇，说话轻柔温和。") }
    var scenario by remember { mutableStateOf("旧天文台穹顶下，望远镜正对准猎户座星云，桌上放着一杯刚泡好的柑橘茶。") }
    var firstMessage by remember { mutableStateOf("嘘……今晚的夜空很澄澈，猎户座的腰带清晰可见。请到观测台边来，我把望远镜调给你看。") }
    var exampleMessages by remember { mutableStateOf("<START>\n{{user}}: 今晚能看到流星吗？\n{{char}}: 按照星轨测算，半小时后会有一场双子座微流星雨。不用着急，我们一起慢慢等。") }
    var creatorNotes by remember { mutableStateOf("遵循 Character Card V2 规范塑造的天文星轨主题伴生者。") }
    var systemPrompt by remember { mutableStateOf("你将扮演 Luna（露娜）。说话语气沉静恬适，富有天体物理学与星辰诗意。") }
    val tags = remember { mutableStateListOf("天文观察", "星轨学者", "静谧夜读", "柑橘红茶") }
    var newTagInput by remember { mutableStateOf("") }
    var creatorName by remember { mutableStateOf("AILUA 星工坊") }
    var characterVersion by remember { mutableStateOf("1.0.0") }
    var avatarReference by remember { mutableStateOf("noa") }

    // Dialog states
    var showImportTemplateDialog by remember { mutableStateOf(false) }
    var showJsonPreviewDialog by remember { mutableStateOf(false) }
    var pendingImportCard by remember { mutableStateOf<CharacterCard?>(null) }
    var pendingImportValidation by remember { mutableStateOf<CharacterCardValidationResult?>(null) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }

    val avatarOptions = listOf("mira", "yuna", "noa")

    // Helper to build CharacterCard from current form
    fun buildCurrentCard(): CharacterCard {
        return CharacterCard(
            spec = "chara_card_v2",
            specVersion = "2.0",
            data = CharacterCardData(
                id = characterId,
                name = characterName,
                description = description,
                personality = personality,
                scenario = scenario,
                firstMessage = firstMessage,
                exampleMessages = exampleMessages,
                creatorNotes = creatorNotes,
                systemPrompt = systemPrompt,
                alternateGreetings = listOf(
                    "今晚的猎户座格外清亮，快来看看吧。",
                    "我刚泡好柑橘红茶，为你留了一杯温的。"
                ),
                tags = tags.toList(),
                creator = creatorName,
                characterVersion = characterVersion,
                avatarReference = avatarReference
            )
        )
    }

    // Android Document Open Launcher (Real UTF-8 JSON Import)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""
                val decodedCard = CharacterCardJsonCodec.decode(jsonString)
                val validation = CharacterCardJsonCodec.validate(decodedCard)
                pendingImportCard = decodedCard
                pendingImportValidation = validation
                showImportConfirmDialog = true
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("JSON 解析失败: ${e.localizedMessage ?: "未知格式错误"}")
                }
            }
        }
    }

    // Android Document Create Launcher (Real UTF-8 JSON Export)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val card = buildCurrentCard()
                val encodedJson = CharacterCardJsonCodec.encode(card)
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(encodedJson.toByteArray(Charsets.UTF_8))
                }
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("已成功导出 ${card.data.name} 为 JSON 文件")
                }
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("导出文件失败: ${e.localizedMessage}")
                }
            }
        }
    }

    AiluaScreenScaffold(
        title = "角色工坊",
        onBack = onBack,
        onGoHome = onGoHome,
        modifier = Modifier.testTag("character_creator_screen"),
        backTestTag = "creator_back_btn",
        bottomBar = { SnackbarHost(snackbarHostState) },
        trailing = {
            IconButton(
                onClick = {
                    val card = buildCurrentCard()
                    CharacterRegistry.saveCharacterCard(card)
                    coroutineScope.launch { snackbarHostState.showSnackbar("已成功保存并注册角色：${card.data.name}") }
                },
                modifier = Modifier.testTag("creator_save_btn")
            ) { Icon(Icons.Default.Save, "保存", tint = theme.palette.onSurface) }
            IconButton(
                onClick = {
                    val card = buildCurrentCard()
                    CharacterRegistry.saveCharacterCard(card)
                    onPreviewCharacter(card.data.id)
                },
                modifier = Modifier.testTag("creator_preview_btn")
            ) { Icon(Icons.Default.Visibility, "预览", tint = theme.palette.onSurfaceMuted) }
        }
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp),
            verticalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                    AiluaChip(label = "新建", onClick = {
                        characterId = "custom_${System.currentTimeMillis() % 1000}"
                        characterName = "新角色"
                        description = ""
                        personality = ""
                        scenario = ""
                        firstMessage = ""
                        exampleMessages = ""
                        tags.clear()
                        coroutineScope.launch { snackbarHostState.showSnackbar("已创建全新角色草稿") }
                    })
                    AiluaChip(label = "复制", onClick = {
                        val copy = CharacterRegistry.duplicateCharacter(characterId)
                        characterId = copy.data.id
                        characterName = copy.data.name
                        coroutineScope.launch { snackbarHostState.showSnackbar("已复制角色副本并保存") }
                    })
                }
            }
            item {
                CreatorSection(title = "基本信息") {
                    CreatorField("角色名称", characterName, { characterName = it }, singleLine = true)
                    CreatorField("简介", description, { description = it }, minLines = 2)
                    Text("头像", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp)) {
                        avatarOptions.forEach { avatar ->
                            Column(
                                modifier = Modifier.clickable { avatarReference = avatar },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CharacterPortrait(avatar, PortraitVariant.AVATAR, modifier = Modifier.size(48.dp))
                                if (avatarReference == avatar) {
                                    Icon(Icons.Default.Check, "已选头像", Modifier.size(18.dp), tint = theme.palette.accent)
                                } else Spacer(Modifier.height(18.dp))
                            }
                        }
                    }
                }
            }
            item {
                CreatorSection(title = "人设") {
                    CreatorField("性格", personality, { personality = it }, minLines = 2)
                    CreatorField("初识场景", scenario, { scenario = it }, minLines = 2)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                        items(tags) { tag -> AiluaChip(label = "$tag ×", onClick = { tags.remove(tag) }) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                        CreatorField("新标签", newTagInput, { newTagInput = it }, singleLine = true, modifier = Modifier.weight(1f))
                        AiluaChip(label = "添加", onClick = {
                            if (newTagInput.isNotBlank()) {
                                tags.add(newTagInput.trim())
                                newTagInput = ""
                            }
                        })
                    }
                }
            }
            item {
                CreatorSection(title = "对话") {
                    CreatorField("初次见面台词", firstMessage, { firstMessage = it }, minLines = 3)
                }
            }
            item {
                AiluaSectionHeader("高级", actionLabel = if (showAdvanced) "收起" else "展开", onAction = { showAdvanced = !showAdvanced })
            }
            if (showAdvanced) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                        item {
                            AiluaChip(label = "导入 JSON", modifier = Modifier.testTag("creator_saf_import_btn"),
                                onClick = { openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*")) })
                        }
                        item {
                            AiluaChip(label = "内置模板", modifier = Modifier.testTag("creator_template_import_btn"),
                                onClick = { showImportTemplateDialog = true })
                        }
                        item {
                            AiluaChip(label = "导出 JSON", modifier = Modifier.testTag("creator_saf_export_btn"), onClick = {
                                val defaultFileName = "${characterName.substringBefore(" ").ifBlank { "character" }}.json"
                                createDocumentLauncher.launch(defaultFileName)
                            })
                        }
                        item {
                            AiluaChip(label = "预览 JSON", modifier = Modifier.testTag("creator_preview_code_btn"),
                                onClick = { showJsonPreviewDialog = true })
                        }
                    }
                }
                item {
                    CreatorSection(title = "角色卡设置") {
                        CreatorField("唯一标识符 (ID)", characterId, { characterId = it }, singleLine = true)
                        CreatorField("System Prompt", systemPrompt, { systemPrompt = it }, minLines = 3)
                        CreatorField("Example Messages", exampleMessages, { exampleMessages = it }, minLines = 3)
                        CreatorField("创作者", creatorName, { creatorName = it }, singleLine = true)
                        CreatorField("角色版本", characterVersion, { characterVersion = it }, singleLine = true)
                        CreatorField("Creator Notes", creatorNotes, { creatorNotes = it }, minLines = 2)
                    }
                }
            }
            item { Spacer(Modifier.height(theme.layout.sectionGap.dp)) }
        }
    }

    if (showImportConfirmDialog) {
        val card = pendingImportCard
        val validation = pendingImportValidation
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            title = { Text("导入 Character Card V2", style = theme.text.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                    if (card != null) {
                        Text("解析到角色：${card.data.name}", style = theme.text.section, color = theme.palette.onSurface)
                        Text("初见台词：${card.data.firstMessage.take(60)}…", style = theme.text.body, color = theme.palette.onSurfaceMuted)
                        Text("标签：${card.data.tags.joinToString(" · ")}", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                        if (validation != null && validation.warnings.isNotEmpty()) {
                            Text("提示：${validation.warnings.joinToString("; ")}", style = theme.text.secondary, color = theme.palette.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    card?.let { c ->
                        characterId = c.data.id.ifBlank { "custom_" + c.data.name.lowercase().replace(" ", "_") }
                        characterName = c.data.name
                        description = c.data.description
                        personality = c.data.personality
                        scenario = c.data.scenario
                        firstMessage = c.data.firstMessage
                        exampleMessages = c.data.exampleMessages
                        creatorNotes = c.data.creatorNotes
                        systemPrompt = c.data.systemPrompt
                        avatarReference = c.data.avatarReference.ifBlank { "mira" }
                        tags.clear()
                        tags.addAll(c.data.tags)
                        creatorName = c.data.creator
                        characterVersion = c.data.characterVersion
                        CharacterRegistry.saveCharacterCard(c)
                        coroutineScope.launch { snackbarHostState.showSnackbar("已成功导入并注册角色：${c.data.name}") }
                    }
                    showImportConfirmDialog = false
                }) { Text("确认导入并载入", style = theme.text.secondary) }
            },
            dismissButton = { TextButton(onClick = { showImportConfirmDialog = false }) { Text("取消", style = theme.text.secondary) } }
        )
    }
    if (showImportTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showImportTemplateDialog = false },
            title = { Text("内置模板", style = theme.text.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                    WorldData.allCards.forEach { card ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                characterId = card.data.id
                                characterName = card.data.name
                                description = card.data.description
                                personality = card.data.personality
                                scenario = card.data.scenario
                                firstMessage = card.data.firstMessage
                                exampleMessages = card.data.exampleMessages
                                creatorNotes = card.data.creatorNotes
                                systemPrompt = card.data.systemPrompt
                                tags.clear()
                                tags.addAll(card.data.tags)
                                creatorName = card.data.creator
                                characterVersion = card.data.characterVersion
                                avatarReference = card.data.avatarReference
                                showImportTemplateDialog = false
                                coroutineScope.launch { snackbarHostState.showSnackbar("已载入 ${card.data.name} 模板") }
                            }.padding(vertical = theme.layout.itemGap.dp),
                            horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CharacterPortrait(card.data.avatarReference, PortraitVariant.AVATAR)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(card.data.name, style = theme.text.body, color = theme.palette.onSurface)
                                Text(card.data.tags.joinToString(" · "), style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showImportTemplateDialog = false }) { Text("关闭", style = theme.text.secondary) } }
        )
    }
    if (showJsonPreviewDialog) {
        val jsonPreview = CharacterCardJsonCodec.encode(buildCurrentCard())
        AlertDialog(
            onDismissRequest = { showJsonPreviewDialog = false },
            title = { Text("Character Card V2 JSON", style = theme.text.title) },
            text = {
                AiluaSurface(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    LazyColumn(modifier = Modifier.padding(theme.layout.itemGap.dp)) {
                        item { Text(jsonPreview, style = theme.text.secondary, color = theme.palette.onSurface) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showJsonPreviewDialog = false }) { Text("完成", style = theme.text.secondary) } }
        )
    }
}

@Composable
private fun CreatorSection(title: String, content: @Composable () -> Unit) {
    val theme = LocalAiluaTheme.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
        AiluaSectionHeader(title)
        content()
    }
}

@Composable
private fun CreatorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    val theme = LocalAiluaTheme.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = theme.text.secondary) },
        textStyle = theme.text.body,
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(theme.shapes.medium.dp)
    )
}
