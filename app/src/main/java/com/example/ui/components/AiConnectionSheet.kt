package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.model.ProviderProfile
import com.example.data.ai.onboarding.ProviderSetup
import com.example.data.ai.repository.ProviderGraph
import com.example.ui.themeengine.LocalAiluaTheme
import com.example.ui.designsystem.AiluaChip
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.rememberCoroutineScope

/**
 * AiConnectionSheet — minimal OpenAI-compatible provider configuration
 * (P3C-4 §4), enough for a real-device E2E: Name / Base URL / Model / API Key
 * with Save · Set Active · Delete.
 *
 * Security contract: the API key goes straight into [ProviderGraph]'s
 * [com.example.data.ai.security.ApiSecretStore] via `setApiKey`; the UI only
 * ever displays the masked `••••••••abcd` label on reopen and clears its
 * local input state right after saving. Plaintext never enters StateFlow.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiConnectionSheet(
    onDismiss: () -> Unit,
    onConnected: () -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    val visual = LocalAiluaTheme.current
    val repository = ProviderGraph.repository
    val profiles by repository.profiles.collectAsStateWithLifecycle()
    val activeId by repository.activeProfileId.collectAsStateWithLifecycle()

    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun resetForm() {
        editingId = null
        name = ""
        baseUrl = ""
        model = ""
        apiKeyInput = ""
    }

    val maskedKey = editingId?.let { id -> repository.maskedKeyLabel(id) }
    val keyPresent = apiKeyInput.isNotBlank() || maskedKey != null
    val validation = ProviderSetup.validate(baseUrl, model, keyPresent)
    val canSave = name.isNotBlank() && validation == null && !isTesting

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = visual.surfaces.raised,
    ) {
        HideDialogStatusBar()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = visual.layout.screenHorizontalPadding.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("AI 连接", style = visual.text.title, color = visual.palette.onSurface)
            Spacer(modifier = Modifier.height(6.dp))
            Text("密钥加密保存在本机", style = visual.text.secondary, color = visual.palette.onSurfaceMuted)

            Spacer(modifier = Modifier.height(14.dp))

            profiles.forEach { profile ->
                val isActive = profile.id == activeId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(visual.shapes.medium.dp))
                        .background(
                            if (isActive) visual.palette.accent.copy(alpha = 0.10f)
                            else visual.surfaces.inset.copy(alpha = 0.4f),
                        )
                        .clickable {
                            editingId = profile.id
                            name = profile.name
                            baseUrl = profile.baseUrl
                            model = profile.model
                            apiKeyInput = ""
                            statusMessage = null
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.name,
                                style = visual.text.body,
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(visual.shapes.medium.dp))
                                        .background(visual.palette.accent.copy(alpha = 0.18f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                ) {
                                    Text(
                                        text = "激活",
                                        style = visual.text.caption,
                                        fontWeight = FontWeight.SemiBold,
                                        color = visual.palette.onSurface,
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${profile.model} · ${profile.baseUrl}",
                            style = visual.text.caption,
                            color = visual.palette.onSurfaceMuted,
                            maxLines = 1,
                        )
                        Text(
                            text = repository.maskedKeyLabel(profile.id) ?: "未设置 API Key",
                            style = visual.text.caption,
                            color = visual.palette.onSurfaceMuted.copy(alpha = 0.7f),
                        )
                    }
                    if (!isActive) {
                        TextButton(onClick = {
                            repository.setActiveProfile(profile.id)
                            statusMessage = "已设为激活：${profile.name}"
                        }) {
                            Text("设为激活", style = visual.text.caption, color = visual.palette.onSurface)
                        }
                    }
                    TextButton(onClick = {
                        repository.deleteProfile(profile.id)
                        if (editingId == profile.id) resetForm()
                        statusMessage = "已删除：${profile.name}"
                    }) {
                        Text("删除", style = visual.text.caption, color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (profiles.isEmpty()) {
                Text(
                    text = "还没有连接。填写下面的表单创建第一个 AI 连接。",
                    style = visual.text.secondary,
                    color = visual.palette.onSurfaceMuted,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text("选择服务商", style = visual.text.caption)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProviderSetup.presets.forEach { preset ->
                    AiluaChip(
                        selected = name == preset.label,
                        onClick = {
                            name = preset.label
                            baseUrl = preset.url
                            model = preset.model
                            statusMessage = null
                        },
                        label = preset.label,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("连接名称") },
                singleLine = true,
                shape = RoundedCornerShape(visual.shapes.medium.dp),
                colors = fieldColors(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API 地址") },
                placeholder = { Text("https://api.openai.com/v1") },
                singleLine = true,
                shape = RoundedCornerShape(visual.shapes.medium.dp),
                colors = fieldColors(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("模型") },
                placeholder = { Text("gpt-4o-mini") },
                singleLine = true,
                shape = RoundedCornerShape(visual.shapes.medium.dp),
                colors = fieldColors(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                placeholder = {
                    Text(if (maskedKey != null) maskedKey else "sk-…")
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(visual.shapes.medium.dp),
                colors = fieldColors(),
            )

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        if (isTesting) return@Button
                        isTesting = true
                        statusMessage = "正在连接…"
                        scope.launch {
                            try {
                                val key = apiKeyInput.takeIf { it.isNotBlank() }
                                    ?: editingId?.let { repository.resolveApiKey(it) }.orEmpty()
                                val result = ProviderSetup.test(baseUrl.trim(), model.trim(), key)
                                statusMessage = result
                                if (result == "连接成功") {
                                    val id = editingId ?: UUID.randomUUID().toString()
                                    val saved = repository.saveProfile(ProviderProfile(
                                        id, name.trim().ifBlank { "自带 API Key" }, baseUrl.trim(), model.trim(), repository.profile(id)?.apiKeyRef
                                    ))
                                    if (apiKeyInput.isNotBlank()) repository.setApiKey(saved.id, apiKeyInput.trim())
                                    repository.setActiveProfile(saved.id)
                                    apiKeyInput = ""
                                    editingId = saved.id
                                    onConnected()
                                }
                            } catch (e: CancellationException) { throw e }
                            catch (_: Exception) { statusMessage = "连接测试失败，请检查配置后重试" }
                            finally { isTesting = false }
                        }
                    },
                    enabled = canSave,
                    shape = RoundedCornerShape(visual.shapes.medium.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = visual.palette.accent.copy(alpha = 0.18f), contentColor = visual.palette.onSurface),
                ) { Text(if (isTesting) "正在连接…" else "测试连接", style = visual.text.secondary) }
                Button(
                    onClick = {
                        val id = editingId ?: UUID.randomUUID().toString()
                        val existing = repository.profile(id)
                        val saved = repository.saveProfile(
                            ProviderProfile(
                                id = id,
                                name = name.trim(),
                                baseUrl = baseUrl.trim(),
                                model = model.trim(),
                                apiKeyRef = existing?.apiKeyRef,
                            ),
                        )
                        if (apiKeyInput.isNotBlank()) {
                            repository.setApiKey(saved.id, apiKeyInput.trim())
                        }
                        apiKeyInput = ""
                        editingId = saved.id
                        if (repository.activeProfileId.value == null) {
                            repository.setActiveProfile(saved.id)
                            statusMessage = "已保存并激活：${saved.name}"
                        } else {
                            statusMessage = "已保存：${saved.name}"
                        }
                    },
                    enabled = canSave,
                    shape = RoundedCornerShape(visual.shapes.medium.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = visual.palette.accent.copy(alpha = 0.18f), contentColor = visual.palette.onSurface),
                ) {
                    Text("保存", style = visual.text.secondary)
                }
                TextButton(onClick = { resetForm(); statusMessage = null }) {
                    Text("清空表单", style = visual.text.secondary, color = visual.palette.onSurfaceMuted)
                }
            }

            statusMessage?.let { msg ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = msg,
                    style = visual.text.caption,
                    color = visual.palette.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = LocalAiluaTheme.current.surfaces.inset.copy(alpha = 0.4f),
    unfocusedContainerColor = LocalAiluaTheme.current.surfaces.inset.copy(alpha = 0.3f),
    focusedBorderColor = LocalAiluaTheme.current.palette.accent.copy(alpha = 0.6f),
    unfocusedBorderColor = Color.Transparent,
)
