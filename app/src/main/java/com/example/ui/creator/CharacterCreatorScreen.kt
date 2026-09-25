package com.example.ui.creator

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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.mock.MockData
import com.example.data.mock.WorldData
import com.example.data.model.CharacterCard
import com.example.data.model.CharacterCardData
import com.example.ui.components.AiluaAvatar
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.theme.AiluaDustyRose
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold
import com.example.ui.theme.AiluaMutedLavender
import kotlinx.coroutines.launch

@Composable
fun CharacterCreatorScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {},
    onPreviewCharacter: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form states initialized with template
    var characterId by remember { mutableStateOf("custom_luna") }
    var characterName by remember { mutableStateOf("露娜 (Luna)") }
    var description by remember { mutableStateOf("星空天文学者，栖息于旧天文台的安静女孩，习惯在静谧深夜记录流星轨迹。") }
    var personality by remember { mutableStateOf("理性从容，内敛而浪漫，对未知星图充满好奇，说话轻柔温和。") }
    var scenario by remember { mutableStateOf("旧天文台穹顶下，望远镜正对准猎户座星云，桌上放着一杯刚泡好的柑橘茶。") }
    var firstMessage by remember { mutableStateOf("嘘……今晚的夜空很澄澈，猎户座的腰带清晰可见。请到观测台边来，我把望远镜调给你看。") }
    var exampleMessages by remember { mutableStateOf("<START>\n{{user}}: 今晚能看到流星吗？\n{{char}}: 按照星轨测算，半小时后会有一场双子座微流星雨。不用着急，我们一起慢慢等。") }
    var creatorNotes by remember { mutableStateOf("遵循 Character Card V2 规范塑造的天文星轨主题伴生者。") }
    var systemPrompt by remember { mutableStateOf("你将扮演 Luna（露娜）。说话语气沉静恬适，富有天体物理学与星辰诗意。") }
    var tags = remember { mutableStateListOf("天文观察", "星轨学者", "静谧夜读", "柑橘红茶") }
    var newTagInput by remember { mutableStateOf("") }
    var creatorName by remember { mutableStateOf("AILUA 星工坊") }
    var characterVersion by remember { mutableStateOf("1.0.0") }
    var avatarReference by remember { mutableStateOf("noa") }
    var associatedWorldBook by remember { mutableStateOf("青石街与心网物语") }

    // Dialog & Preview states
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showPreviewModal by remember { mutableStateOf(false) }

    val avatarOptions = listOf("mira", "yuna", "noa")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("character_creator_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Status Bar
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

                // Quick Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "JSON 导入",
                            tint = AiluaMistBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "JSON 导出",
                            tint = AiluaMoonGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Action Toolbar (新建, 复制, 预览, 保存)
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
                            // Reset to empty new card
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
                            characterName = "$characterName (副本)"
                            characterId = "${characterId}_copy"
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("已复制角色副本")
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("复制", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = { showPreviewModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AiluaMistBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("预览角色", fontSize = 12.sp, color = Color.White)
                }
            }

            // Form Sections
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Section 1: 基本档案
                item {
                    CreatorSectionCard(title = "1. 基本档案 · Identity") {
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
                            value = characterId,
                            onValueChange = { characterId = it },
                            label = { Text("唯一标识 (ID)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "伴生形象锚点 (Avatar Reference):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                                        .padding(3.dp)
                                ) {
                                    AiluaAvatar(avatarId = av, size = 46.dp, showHalo = isSelected)
                                }
                            }
                        }
                    }
                }

                // Section 2: 性格与设定
                item {
                    CreatorSectionCard(title = "2. 性格特质 · Personality") {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("外貌与简要介绍 (Description)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = personality,
                            onValueChange = { personality = it },
                            label = { Text("性格灵魂特征 (Personality)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Section 3: 世界背景与场景
                item {
                    CreatorSectionCard(title = "3. 世界背景 · Scenario") {
                        OutlinedTextField(
                            value = scenario,
                            onValueChange = { scenario = it },
                            label = { Text("开场情境与生活场景 (Scenario)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = systemPrompt,
                            onValueChange = { systemPrompt = it },
                            label = { Text("系统前置提示词 (System Prompt)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Section 4: 初次见面问候
                item {
                    CreatorSectionCard(title = "4. 初次见面 · First Message") {
                        OutlinedTextField(
                            value = firstMessage,
                            onValueChange = { firstMessage = it },
                            label = { Text("开场首条心网私信 (First Mes)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Section 5: 示例对话
                item {
                    CreatorSectionCard(title = "5. 示例对话 · Examples") {
                        OutlinedTextField(
                            value = exampleMessages,
                            onValueChange = { exampleMessages = it },
                            label = { Text("对话范例 (<START> {{user}} {{char}})") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }

                // Section 6: 标签系统
                item {
                    CreatorSectionCard(title = "6. 角色标签 · Tags") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newTagInput,
                                onValueChange = { newTagInput = it },
                                label = { Text("新增标签") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newTagInput.isNotBlank() && !tags.contains(newTagInput.trim())) {
                                        tags.add(newTagInput.trim())
                                        newTagInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("添加")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tags) { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AiluaMistBlue.copy(alpha = 0.15f))
                                        .border(0.5.dp, AiluaMistBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
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
                    }
                }

                // Section 7: 创作者信息与世界书关联
                item {
                    CreatorSectionCard(title = "7. 创作者与世界书 · Lorebook Link") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = creatorName,
                                onValueChange = { creatorName = it },
                                label = { Text("创作者 (Creator)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = characterVersion,
                                onValueChange = { characterVersion = it },
                                label = { Text("版本 (Version)") },
                                modifier = Modifier.weight(0.6f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = creatorNotes,
                            onValueChange = { creatorNotes = it },
                            label = { Text("创作者备注 (Creator Notes)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = associatedWorldBook,
                            onValueChange = { associatedWorldBook = it },
                            label = { Text("关联世界书 (World Book)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Snackbar
            SnackbarHost(hostState = snackbarHostState)

            // Home Bar
            VirtualPhoneHomeBar(canGoBack = true, onBack = onBack, onGoHome = onBack)
        }

        // Preview Modal
        if (showPreviewModal) {
            AlertDialog(
                onDismissRequest = { showPreviewModal = false },
                title = { Text("角色预览 · $characterName") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AiluaAvatar(avatarId = avatarReference, size = 52.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    characterName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    tags.joinToString(" · "),
                                    fontSize = 11.sp,
                                    color = AiluaMistBlue
                                )
                            }
                        }
                        Text("【开场问候】", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(firstMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("【背景场景】", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(scenario, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    Button(onClick = { showPreviewModal = false }) {
                        Text("确认")
                    }
                }
            )
        }

        // Export JSON Dialog
        if (showExportDialog) {
            val jsonPreview = """
{
  "spec": "chara_card_v2",
  "spec_version": "2.0",
  "data": {
    "name": "$characterName",
    "description": "${description.replace("\"", "\\\"")}",
    "personality": "${personality.replace("\"", "\\\"")}",
    "scenario": "${scenario.replace("\"", "\\\"")}",
    "first_mes": "${firstMessage.replace("\"", "\\\"")}",
    "mes_example": "${exampleMessages.replace("\n", "\\n").replace("\"", "\\\"")}",
    "creator_notes": "$creatorNotes",
    "system_prompt": "$systemPrompt",
    "tags": [${tags.joinToString { "\"$it\"" }}],
    "creator": "$creatorName",
    "character_version": "$characterVersion",
    "extensions": {
      "ailua_avatar": "$avatarReference",
      "ailua_world_book": "$associatedWorldBook"
    }
  }
}
            """.trimIndent()

            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("导出 Character Card V2 JSON") },
                text = {
                    Column {
                        Text("符合社区标准的 Character Card V2 JSON 规范：", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
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
                                            fontSize = 10.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExportDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("已生成 Character Card V2 数据结构")
                            }
                        }
                    ) {
                        Text("复制并完成")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("关闭")
                    }
                }
            )
        }

        // Import Preset Template Dialog
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("导入角色卡模版 · JSON Import") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("请选择要加载的 Character Card V2 角色范例：", fontSize = 12.sp)
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
                                        showImportDialog = false
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("已成功载入 ${card.data.name}")
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
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("取消")
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
