package com.example.data.ai.onboarding

import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiProviderError
import com.example.data.ai.model.AiRole
import com.example.data.ai.model.AiStreamEvent
import com.example.data.ai.provider.AiProvider
import com.example.data.ai.provider.OpenAiCompatibleProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URI

/** These presets all use the app's existing OpenAI-compatible protocol. */
data class ProviderPreset(val label: String, val url: String, val model: String)

object ProviderSetup {
    private const val CONNECTION_TEST_TIMEOUT_MS = 45_000L
    val presets = listOf(
        ProviderPreset("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
        ProviderPreset("DeepSeek", "https://api.deepseek.com", "deepseek-flash"),
        ProviderPreset("OpenRouter", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini"),
        ProviderPreset("兼容接口 / 自定义", "", ""),
    )

    fun validate(url: String, model: String, keyPresent: Boolean): String? {
        val uri = runCatching { URI(url.trim()) }.getOrNull()
        if (uri?.scheme != "https" || uri.host.isNullOrBlank() || uri.userInfo != null || uri.query != null || uri.fragment != null) {
            return "请输入有效的 HTTPS API 地址"
        }
        if (model.isBlank()) return "请输入模型名称"
        if (!keyPresent) return "请输入 API Key"
        return null
    }

    fun friendly(error: AiProviderError): String = when (error) {
        AiProviderError.Unauthorized -> "认证失败，请检查 API Key"
        AiProviderError.Timeout -> "连接超时，请稍后重试"
        is AiProviderError.Network -> "网络连接失败，请检查网络和 API 地址"
        is AiProviderError.Protocol -> "服务返回格式不兼容，请检查接口地址"
        is AiProviderError.Http -> when (error.code) {
            403 -> "服务拒绝访问，请检查 Key 权限"
            404 -> "模型或接口地址不存在"
            429 -> "请求过于频繁或额度不足"
            in 500..599 -> "AI 服务暂时不可用"
            else -> "连接失败，请检查模型和接口配置"
        }
        AiProviderError.Cancelled -> "连接测试已取消"
    }

    suspend fun test(url: String, model: String, key: String, provider: AiProvider = OpenAiCompatibleProvider(url, key)): String {
        validate(url, model, key.isNotBlank())?.let { return it }
        val terminal = withTimeoutOrNull(CONNECTION_TEST_TIMEOUT_MS) {
            provider.streamChat(AiChatRequest(model, listOf(AiMessage(AiRole.USER, "请只回复：好")), stream = false, maxTokens = 8))
                .first { it is AiStreamEvent.Completed || it is AiStreamEvent.Failed || it is AiStreamEvent.Cancelled }
        }
        return when (terminal) {
            is AiStreamEvent.Completed -> "连接成功"
            is AiStreamEvent.Failed -> friendly(terminal.error)
            AiStreamEvent.Cancelled -> "连接测试已取消"
            else -> "连接超时，请稍后重试"
        }
    }
}
