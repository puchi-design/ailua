package com.example.ui.creator

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.codec.CharacterCardJsonCodec
import com.example.data.codec.CharacterCardValidationResult
import com.example.data.mock.WorldData
import com.example.data.model.CharacterCard
import com.example.data.model.CharacterCardData
import com.example.data.registry.CharacterRegistry
import com.example.ui.components.AiluaAvatar
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.theme.AiluaDustyRose
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold
import kotlinx.coroutines.launch

@Composable
fun CharacterCreatorScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {},
    onPreviewCharacter: (String) -> Unit = {}
) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("character_creator_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VirtualPhoneStatusBar(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )

            // Top Header
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
                    IconButton(onClick = onBack, modifier = Modifier.testTag("creator_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "伴生工坊 · Character Creator",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Character Card V2 规范生命塑造平台",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                // Quick Action Buttons (Document SAF Import & Export)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Real Android SAF Import
                    IconButton(
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        modifier = Modifier.size(36.dp).testTag("creator_saf_import_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "打开本地 JSON 文件",
                            tint = AiluaMistBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Template Import
                    IconButton(
                        onClick = { showImportTemplateDialog = true },
                        modifier = Modifier.size(36.dp).testTag("creator_template_import_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "内置模版导入",
                            tint = AiluaMoonGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Real Android SAF Export
                    IconButton(
                        onClick = {
                            val defaultFileName = "${characterName.substringBefore(" ").ifBlank { "character" }}.json"
                            createDocumentLauncher.launch(defaultFileName)
                        },
                        modifier = Modifier.size(36.dp).testTag("creator_saf_export_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "导出 JSON 文件",
                            tint = AiluaMoonGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // JSON Code Preview
                    IconButton(
                        onClick = { showJsonPreviewDialog = true },
                        modifier = Modifier.size(36.dp).testTag("creator_preview_code_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "预览 JSON",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Action Toolbar (新建, 复制, 保存, 预览)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            characterId = "custom_${System.currentTimeMillis() % 1000}"
                            characterName = "新角色"
                            description = ""
                            personality = ""
                            scenario = ""
                            firstMessage = ""
                            exampleMessages = ""
                            tags.clear()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("已创建全新角色草稿")
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新建", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val copy = CharacterRegistry.duplicateCharacter(characterId)
                            characterId = copy.data.id
                            characterName = copy.data.name
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("已复制角色副本并保存")
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("复制", fontSize = 12.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Save into CharacterRegistry
                    Button(
                        onClick = {
                            val card = buildCurrentCard()
                            CharacterRegistry.saveCharacterCard(card)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("已成功保存并注册角色：${card.data.name}")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AiluaMoonGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("creator_save_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF2C2411), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("保存", fontSize = 12.sp, color = Color(0xFF2C2411), fontWeight = FontWeight.Bold)
                    }

                    // Preview Profile
                    Button(
                        onClick = {
                            val card = buildCurrentCard()
                            CharacterRegistry.saveCharacterCard(card)
                            onPreviewCharacter(card.data.id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AiluaMistBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("creator_preview_btn")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("预览", fontSize = 12.sp)
                    }
                }
            }

            // Form Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: 基本档案
                item {
                    CreatorSectionCard(title = "1. 基本档案 · Identity") {
                        OutlinedTextField(
                            value = characterId,
                            onValueChange = { characterId = it },
                            label = { Text("唯一标识符 (ID)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = characterName,
                            onValueChange = { characterName = it },
                            label = { Text("角色名称 (Name)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("角色简述 (Description)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Avatar Selection
                        Text("伴生头像标识：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            avatarOptions.forEach { av ->
                                val isSelected = avatarReference == av
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .border(
                                            2.dp,
                                            if (isSelected) AiluaMistBlue else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { avatarReference = av }
                                        .padding(2.dp)
                                ) {
                                    AiluaAvatar(avatarId = av, size = 48.dp, showHalo = isSelected)
                                }
                            }
                        }
                    }
                }

                // Section 2: 性格与设定
                item {
                    CreatorSectionCard(title = "2. 性格设定 · Personality") {
                        OutlinedTextField(
                            value = personality,
                            onValueChange = { personality = it },
                            label = { Text("性格特征 (Personality)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = scenario,
                            onValueChange = { scenario = it },
                            label = { Text("初识场景 (Scenario)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Section 3: 初次见面与系统提示词
                item {
                    CreatorSectionCard(title = "3. 初见台词与提示词 · System Prompt") {
                        OutlinedTextField(
                            value = firstMessage,
                            onValueChange = { firstMessage = it },
                            label = { Text("初次见面台词 (First Message / first_mes)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = systemPrompt,
                            onValueChange = { systemPrompt = it },
                            label = { Text("系统提示词 (System Prompt)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Section 4: 示例对话
                item {
                    CreatorSectionCard(title = "4. 示例对话 · Examples") {
                        OutlinedTextField(
                            value = exampleMessages,
                            onValueChange = { exampleMessages = it },
                            label = { Text("对话范例 (mes_example)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Section 5: 标签 Tags
                item {
                    CreatorSectionCard(title = "5. 标签 · Tags") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tags) { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(AiluaMistBlue.copy(alpha = 0.2f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = tag, fontSize = 11.5.sp, color = AiluaMistBlue)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "删除",
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { tags.remove(tag) },
                                            tint = AiluaMistBlue
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newTagInput,
                                onValueChange = { newTagInput = it },
                                placeholder = { Text("添加新标签…", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newTagInput.isNotBlank()) {
                                        tags.add(newTagInput.trim())
                                        newTagInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("添加")
                            }
                        }
                    }
                }

                // Section 6: 创作者信息
                item {
                    CreatorSectionCard(title = "6. 创作者署名 · Creator Info") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = creatorName,
                                onValueChange = { creatorName = it },
                                label = { Text("创作者") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = characterVersion,
                                onValueChange = { characterVersion = it },
                                label = { Text("版本") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = creatorNotes,
                            onValueChange = { creatorNotes = it },
                            label = { Text("创作者寄语 (Creator Notes)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(18.dp)) }
            }

            // Snackbar
            SnackbarHost(hostState = snackbarHostState)

            VirtualPhoneHomeBar(
                canGoBack = true,
                onBack = onBack,
                onGoHome = onBack
            )
        }

        // Dialog 1: Import SAF Validation & Confirmation Dialog
        if (showImportConfirmDialog) {
            val card = pendingImportCard
            val validation = pendingImportValidation

            AlertDialog(
                onDismissRequest = { showImportConfirmDialog = false },
                title = { Text("导入 Character Card V2 预览") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (card != null) {
                            Text("解析到角色：${card.data.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("初见台词：${card.data.firstMessage.take(60)}…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("标签：${card.data.tags.joinToString(" · ")}", fontSize = 11.sp, color = AiluaMistBlue)

                            if (validation != null && validation.warnings.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("提示：${validation.warnings.joinToString("; ")}", fontSize = 11.sp, color = AiluaMoonGold)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
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

                                // Register directly into CharacterRegistry
                                CharacterRegistry.saveCharacterCard(c)

                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("已成功导入并注册伴生者：${c.data.name}")
                                }
                            }
                            showImportConfirmDialog = false
                        }
                    ) {
                        Text("确认导入并载入")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportConfirmDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        // Dialog 2: Preset Template Selection
        if (showImportTemplateDialog) {
            AlertDialog(
                onDismissRequest = { showImportTemplateDialog = false },
                title = { Text("载入内置模版 · Preset Card") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WorldData.allCards.forEach { card ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
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
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("已载入 ${card.data.name} 模版")
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AiluaAvatar(avatarId = card.data.avatarReference, size = 36.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(card.data.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(card.data.tags.joinToString(" · "), fontSize = 10.5.sp, color = AiluaMistBlue)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showImportTemplateDialog = false }) {
                        Text("关闭")
                    }
                }
            )
        }

        // Dialog 3: JSON Code Preview
        if (showJsonPreviewDialog) {
            val jsonPreview = CharacterCardJsonCodec.encode(buildCurrentCard())
            AlertDialog(
                onDismissRequest = { showJsonPreviewDialog = false },
                title = { Text("Character Card V2 JSON 结构") },
                text = {
                    Column {
                        Text("实时序列化输出 (kotlinx.serialization)：", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(8.dp)
                        ) {
                            LazyColumn {
                                item {
                                    Text(
                                        text = jsonPreview,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showJsonPreviewDialog = false }) {
                        Text("完成")
                    }
                }
            )
        }
    }
}

@Composable
private fun CreatorSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                ),
                color = AiluaMistBlue
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
