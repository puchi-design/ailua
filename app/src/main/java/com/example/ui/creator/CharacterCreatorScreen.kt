package com.example.ui.creator

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.codec.AiluaCharacterExtensionCodec
import com.example.data.codec.CharacterCardJsonCodec
import com.example.data.model.CharacterCard
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
    onGoHome: () -> Unit = onBack,
    initialAdvancedMode: Boolean = false
) {
    val theme = LocalAiluaTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var draft by rememberSaveable(stateSaver = CreatorDraft.Saver) { mutableStateOf(CreatorDraft.fresh()) }
    var advanced by rememberSaveable(initialAdvancedMode) { mutableStateOf(initialAdvancedMode) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var showTemplates by rememberSaveable { mutableStateOf(false) }
    var jsonPreview by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImport by rememberSaveable(stateSaver = CreatorCardSaver) { mutableStateOf<CharacterCard?>(null) }
    val pendingValidation = remember(pendingImport) { pendingImport?.let(CharacterCardJsonCodec::validate) }
    var exportCard by rememberSaveable(stateSaver = CreatorCardSaver) { mutableStateOf<CharacterCard?>(null) }
    val data = draft.card.data
    val behavior = AiluaCharacterExtensionCodec.read(data)
    val canEditBehavior = AiluaCharacterExtensionCodec.canEdit(data)

    fun notify(message: String) { scope.launch { snackbar.showSnackbar(message) } }
    fun persistCard(card: CharacterCard): CharacterCard {
        val savedProfile = CharacterRegistry.saveCharacterCard(card)
        val savedCard = CharacterRegistry.getCard(savedProfile.id) ?: card.copy(data = card.data.copy(id = savedProfile.id))
        draft = CreatorDraft(savedCard)
        return savedCard
    }
    fun withValidCard(action: (CharacterCard) -> Unit) {
        runCatching {
            val card = draft.build()
            val validation = CharacterCardJsonCodec.validate(card)
            require(validation.isValid) { validation.errors.joinToString("\n") }
            draft = CreatorDraft(card)
            error = null
            action(card)
        }.onFailure { error = it.message ?: "角色卡格式不正确" }
    }
    fun saveCard() = withValidCard { card ->
        persistCard(card)
        notify("已保存角色：${card.data.name}")
    }

    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader(Charsets.UTF_8).readText() }
                ?: throw IllegalStateException("无法读取角色卡")
            CharacterCardJsonCodec.decode(raw)
        }.onSuccess {
            pendingImport = it
        }.onFailure { error = "导入失败：${it.localizedMessage ?: "未知格式错误"}" }
    }
    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        val card = exportCard
        if (uri != null && card != null) runCatching {
            val stream = context.contentResolver.openOutputStream(uri) ?: throw IllegalStateException("无法写入文件")
            stream.use { it.write(CharacterCardJsonCodec.encode(card).toByteArray(Charsets.UTF_8)) }
        }.onSuccess { notify("已导出 ${card.data.name}") }
            .onFailure { error = "导出失败：${it.localizedMessage}" }
        exportCard = null
    }
    val choosePhoto = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            draft = draft.editData { it.copy(avatarReference = uri.toString()) }
        }.onFailure { error = "照片权限未能保留，请从系统文件选择器重新选择照片。" }
    }

    AiluaScreenScaffold(
        title = if (advanced) "角色工坊" else "创建角色",
        onBack = onBack, onGoHome = onGoHome,
        modifier = Modifier.testTag("character_creator_screen"), backTestTag = "creator_back_btn",
        bottomBar = { SnackbarHost(snackbar) },
        trailing = {
            if (advanced) IconButton(onClick = ::saveCard, modifier = Modifier.testTag("creator_save_btn")) {
                Icon(Icons.Default.Save, "保存", tint = theme.palette.onSurface)
            }
            IconButton(onClick = {
                withValidCard { card ->
                    onPreviewCharacter(persistCard(card).data.id)
                }
            }, modifier = Modifier.testTag("creator_preview_btn")) {
                Icon(Icons.Default.Visibility, "保存并预览", tint = theme.palette.onSurfaceMuted)
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp),
            verticalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                    AiluaChip("普通模式", selected = !advanced, modifier = Modifier.testTag("creator_simple_mode"), onClick = {
                        runCatching { draft.applyJsonEdits() }.onSuccess { draft = it; advanced = false; error = null }
                            .onFailure { error = "请先修正高级 JSON：${it.message}" }
                    })
                    AiluaChip("高级模式", selected = advanced, modifier = Modifier.testTag("creator_advanced_mode"), onClick = { advanced = true })
                }
            }
            error?.let { message -> item { Text(message, style = theme.text.secondary, color = theme.palette.onSurface) } }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                    AiluaChip("新建", onClick = { draft = CreatorDraft.fresh(); error = null })
                    AiluaChip("复制", onClick = {
                        runCatching {
                            val copied = draft.duplicate()
                            val validation = CharacterCardJsonCodec.validate(copied.card)
                            require(validation.isValid) { validation.errors.joinToString("\n") }
                            persistCard(copied.card)
                            error = null
                            notify("已保存完整角色副本")
                        }.onFailure { error = it.message }
                    })
                }
            }
            if (!advanced) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                        CharacterPortrait(data.id, PortraitVariant.PROFILE, avatarReferenceOverride = data.avatarReference, characterNameOverride = data.name)
                        AiluaChip("选择角色照片", onClick = { choosePhoto.launch(arrayOf("image/*")) }, modifier = Modifier.testTag("creator_photo_btn"))
                    }
                }
                item {
                    CreatorSection("认识这个人") {
                        CreatorField("名字", data.name, { text -> draft = draft.editData { it.copy(name = text) } }, singleLine = true)
                        if (canEditBehavior) {
                            CreatorField("性别（可自定义）", genderLabel(behavior.identity.gender), { text ->
                                draft = draft.editExtension { it.copy(identity = it.identity.copy(gender = genderValue(text))) }
                            }, singleLine = true)
                            CreatorField("年龄（可不填）", behavior.identity.age?.toString().orEmpty(), { text ->
                                if (text.isBlank() || text.toIntOrNull() != null) draft = draft.editExtension {
                                    it.copy(identity = it.identity.copy(age = text.toIntOrNull()))
                                }
                            }, singleLine = true)
                            CreatorField("职业", behavior.identity.occupation, { text ->
                                draft = draft.editExtension { it.copy(identity = it.identity.copy(occupation = text)) }
                            })
                            CreatorField("你们的关系", relationLabel(behavior.relationship.initialRelation), { text ->
                                draft = draft.editExtension { it.copy(relationship = it.relationship.copy(initialRelation = relationValue(text))) }
                            })
                            CreatorField("关系类型（可自定义）", routeLabel(behavior.relationship.routeType), { text ->
                                draft = draft.editExtension { it.copy(relationship = it.relationship.copy(routeType = routeValue(text))) }
                            })
                        } else Text(
                            "这张卡的 AILUA 行为格式暂不支持普通编辑。人物简介仍可修改；行为、性别与关系会原样保留，可在高级模式查看原始扩展。",
                            style = theme.text.secondary, color = theme.palette.onSurfaceMuted
                        )
                        CreatorField("人物简介", data.description, { text -> draft = draft.editData { it.copy(description = text) } }, minLines = 2)
                    }
                }
                item {
                    CreatorSection("相处的感觉") {
                        CreatorField("性格，包括缺点与不擅长的事", data.personality, { text -> draft = draft.editData { it.copy(personality = text) } }, minLines = 3)
                        if (canEditBehavior) {
                            CreatorField("说话的感觉（每行一种）", behavior.speech.tone.joinToString("\n"), { text ->
                                draft = draft.editExtension { it.copy(speech = it.speech.copy(tone = lines(text))) }
                            }, minLines = 2)
                            CreatorField("平时会做什么（每行一项）", behavior.life.hobbies.joinToString("\n"), { text ->
                                draft = draft.editExtension { it.copy(life = it.life.copy(hobbies = lines(text))) }
                            }, minLines = 2)
                            CreatorField("怎么表达喜欢（每行一种）", behavior.behavior.flirtPatterns.joinToString("\n"), { text ->
                                draft = draft.editExtension { it.copy(behavior = it.behavior.copy(flirtPatterns = lines(text))) }
                            }, minLines = 2)
                            CreatorField("吃醋时是什么样（每行一种）", behavior.behavior.jealousyPatterns.joinToString("\n"), { text ->
                                draft = draft.editExtension { it.copy(behavior = it.behavior.copy(jealousyPatterns = lines(text))) }
                            }, minLines = 2)
                            Text("主动联系你的频率", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                                listOf("low" to "少一些", "medium" to "适中", "high" to "主动些").forEach { (value, label) ->
                                    AiluaChip(label, selected = behavior.initiative.messageFrequency == value, onClick = {
                                        draft = draft.editExtension { it.copy(initiative = it.initiative.copy(messageFrequency = value)) }
                                    })
                                }
                            }
                        }
                    }
                }
                item {
                    CreatorSection("初次见面") {
                        CreatorField("相遇的场景", data.scenario, { text -> draft = draft.editData { it.copy(scenario = text) } }, minLines = 2)
                        CreatorField("第一句话", data.firstMessage, { text -> draft = draft.editData { it.copy(firstMessage = text) } }, minLines = 3)
                        AiluaChip("保存角色", selected = true, onClick = ::saveCard, modifier = Modifier.fillMaxWidth().testTag("creator_save_btn"))
                    }
                }
            } else {
                item {
                    LazyRow(
                        modifier = Modifier.testTag("creator_advanced_actions"),
                        horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)
                    ) {
                        item { AiluaChip("导入 JSON", modifier = Modifier.testTag("creator_saf_import_btn"), onClick = { openDocument.launch(arrayOf("application/json", "text/*", "*/*")) }) }
                        item { AiluaChip("内置模板", modifier = Modifier.testTag("creator_template_import_btn"), onClick = { showTemplates = true }) }
                        item { AiluaChip("导出 JSON", modifier = Modifier.testTag("creator_saf_export_btn"), onClick = {
                            withValidCard { card ->
                                exportCard = card
                                createDocument.launch("${card.data.name.substringBefore(" ").ifBlank { "character" }}.json")
                            }
                        }) }
                        item { AiluaChip("预览 JSON", modifier = Modifier.testTag("creator_preview_code_btn"), onClick = {
                            runCatching { CharacterCardJsonCodec.encode(draft.build()) }.onSuccess { jsonPreview = it }.onFailure { error = it.message }
                        }) }
                    }
                }
                item {
                    CreatorSection("Character Card V2") {
                        CreatorField("ID", data.id, { text -> draft = draft.editData { it.copy(id = text) } }, singleLine = true)
                        CreatorField("Name", data.name, { text -> draft = draft.editData { it.copy(name = text) } })
                        CreatorField("Description", data.description, { text -> draft = draft.editData { it.copy(description = text) } }, minLines = 3)
                        CreatorField("Personality", data.personality, { text -> draft = draft.editData { it.copy(personality = text) } }, minLines = 3)
                        CreatorField("Scenario", data.scenario, { text -> draft = draft.editData { it.copy(scenario = text) } }, minLines = 3)
                        CreatorField("First Mes", data.firstMessage, { text -> draft = draft.editData { it.copy(firstMessage = text) } }, minLines = 3)
                        CreatorField("Mes Example", data.exampleMessages, { text -> draft = draft.editData { it.copy(exampleMessages = text) } }, minLines = 3)
                        CreatorField("System Prompt", data.systemPrompt, { text -> draft = draft.editData { it.copy(systemPrompt = text) } }, minLines = 3)
                        CreatorField("Post History Instructions", data.postHistoryInstructions, { text -> draft = draft.editData { it.copy(postHistoryInstructions = text) } }, minLines = 3)
                        CreatorField("Alternate Greetings (JSON)", draft.jsonText("alternate_greetings"), { draft = draft.editJson("alternate_greetings", it) }, minLines = 3)
                        CreatorField("Lorebook / Character Book (JSON)", draft.jsonText("character_book"), { draft = draft.editJson("character_book", it) }, minLines = 4)
                        CreatorField("Extensions (JSON)", draft.jsonText("extensions"), { draft = draft.editJson("extensions", it) }, minLines = 5)
                        CreatorField("Tags（每行一个）", data.tags.joinToString("\n"), { text -> draft = draft.editData { it.copy(tags = lines(text)) } }, minLines = 2)
                        CreatorField("Creator Notes", data.creatorNotes, { text -> draft = draft.editData { it.copy(creatorNotes = text) } }, minLines = 2)
                        CreatorField("Creator", data.creator, { text -> draft = draft.editData { it.copy(creator = text) } })
                        CreatorField("Character Version", data.characterVersion, { text -> draft = draft.editData { it.copy(characterVersion = text) } })
                        CreatorField("Avatar Reference", data.avatarReference, { text -> draft = draft.editData { it.copy(avatarReference = text) } })
                    }
                }
            }
            item { Spacer(Modifier.height(theme.layout.sectionGap.dp)) }
        }
    }
    pendingImport?.let { card ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("导入 Character Card V2", style = theme.text.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                    Text(card.data.name, style = theme.text.section)
                    Text(card.data.firstMessage.take(120), style = theme.text.body)
                    pendingValidation?.let { result ->
                        (result.errors + result.warnings).forEach { Text(it, style = theme.text.secondary) }
                    }
                }
            },
            confirmButton = { TextButton(enabled = pendingValidation?.isValid == true, onClick = {
                runCatching {
                    val imported = card.copy(data = card.data.copy(id = card.data.id.ifBlank { "custom_${java.util.UUID.randomUUID()}" }))
                    val saved = persistCard(imported)
                    pendingImport = null
                    error = null
                    notify("已导入并保存 ${saved.data.name}")
                }.onFailure {
                    pendingImport = null
                    error = "保存导入角色失败：${it.message}"
                }
            }) { Text("确认导入并载入") } },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("取消") } }
        )
    }
    if (showTemplates) AlertDialog(
        onDismissRequest = { showTemplates = false }, title = { Text("内置与已保存角色", style = theme.text.title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                items(CharacterRegistry.getAllCards(), key = { it.data.id }) { card ->
                    Row(Modifier.fillMaxWidth().clickable {
                        draft = CreatorDraft(card)
                        showTemplates = false
                        error = null
                    }.padding(vertical = theme.layout.itemGap.dp), horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                        CharacterPortrait(card.data.id, PortraitVariant.AVATAR, avatarReferenceOverride = card.data.avatarReference, characterNameOverride = card.data.name)
                        Column(Modifier.weight(1f)) {
                            Text(card.data.name, style = theme.text.body)
                            Text(card.data.tags.joinToString(" · "), style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { showTemplates = false }) { Text("关闭") } }
    )
    jsonPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { jsonPreview = null }, title = { Text("Character Card V2 JSON", style = theme.text.title) },
            text = { AiluaSurface(Modifier.fillMaxWidth().height(300.dp)) {
                LazyColumn(Modifier.padding(theme.layout.itemGap.dp)) { item { Text(preview, style = theme.text.secondary, color = theme.palette.onSurface) } }
            } },
            confirmButton = { TextButton(onClick = { jsonPreview = null }) { Text("完成") } }
        )
    }
}

// Keep the trailing blank row while typing; trimming it would prevent adding a new line.
private fun lines(text: String) = if (text.isEmpty()) emptyList() else text.split('\n')
private fun genderLabel(value: String) = when (value) { "male" -> "男"; "female" -> "女"; else -> value }
private fun genderValue(value: String) = when (value) { "男" -> "male"; "女" -> "female"; else -> value }
private fun routeLabel(value: String) = when (value) { "romance" -> "恋爱"; "friendship" -> "友情"; else -> value }
private fun routeValue(value: String) = when (value) { "恋爱" -> "romance"; "友情" -> "friendship"; else -> value }
private fun relationLabel(value: String) = when (value) { "acquaintance" -> "相识"; "friend" -> "朋友"; "partner" -> "恋人"; else -> value }
private fun relationValue(value: String) = when (value) { "相识" -> "acquaintance"; "朋友" -> "friend"; "恋人" -> "partner"; else -> value }

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
    label: String, value: String, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier, singleLine: Boolean = false, minLines: Int = 1
) {
    val theme = LocalAiluaTheme.current
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, style = theme.text.secondary) }, textStyle = theme.text.body,
        modifier = modifier.fillMaxWidth(), singleLine = singleLine, minLines = minLines,
        shape = RoundedCornerShape(theme.shapes.medium.dp)
    )
}
