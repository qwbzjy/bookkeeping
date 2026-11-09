package com.wenbo.bookkeeping.data.llm

import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 大模型客户端：按配置的协议编解码请求/响应，HttpURLConnection 发送。
 * 每次按当前配置构造实例（配置可能随时在设置页修改），无状态。
 */
class LlmClient(private val config: LlmConfig) {

    /** 票据识别：图片 base64 → 结构化识别结果。 */
    suspend fun analyzeReceipt(
        imageBase64: String,
        mediaType: String,
        todayEpochDay: Long = LocalDate.now().toEpochDay(),
    ): ReceiptParse = withContext(Dispatchers.IO) {
        val request = when (config.provider) {
            LlmProvider.OPENAI -> OpenAiCodec.receiptRequest(config, imageBase64, mediaType)
            LlmProvider.ANTHROPIC -> AnthropicCodec.receiptRequest(config, imageBase64, mediaType)
        }
        val text = extractText(HttpTransport.post(request))
        val json = ReceiptCodec.extractJson(text)
            ?: throw IOException("模型返回中未找到 JSON")
        ReceiptCodec.parse(json, todayEpochDay)
            ?: throw IOException("识别结果缺少有效金额")
    }

    /** 连通性测试：返回模型回复文本。 */
    suspend fun ping(): String = withContext(Dispatchers.IO) {
        val request = when (config.provider) {
            LlmProvider.OPENAI -> OpenAiCodec.pingRequest(config)
            LlmProvider.ANTHROPIC -> AnthropicCodec.pingRequest(config)
        }
        extractText(HttpTransport.post(request))
    }

    private fun extractText(raw: String): String {
        try {
            return when (config.provider) {
                LlmProvider.OPENAI -> OpenAiCodec.extractText(raw)
                LlmProvider.ANTHROPIC -> AnthropicCodec.extractText(raw)
            }
        } catch (e: Exception) {
            // 响应不是预期结构：多为鉴权/参数错误，尽量透出服务端信息
            val message = when (config.provider) {
                LlmProvider.OPENAI -> OpenAiCodec.extractErrorMessage(raw)
                LlmProvider.ANTHROPIC -> AnthropicCodec.extractErrorMessage(raw)
            }
            throw IOException(message ?: "响应格式不符合协议", e)
        }
    }
}
