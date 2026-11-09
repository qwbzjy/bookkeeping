package com.wenbo.bookkeeping.data.llm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Anthropic Messages 协议编解码（图片走 base64 source）。
 * 纯 JSON 构造与解析，不碰网络，可完整单测。
 */
object AnthropicCodec {

    private const val ANTHROPIC_VERSION = "2023-06-01"
    private const val MAX_TOKENS = 1024

    /** 票据识别请求：文本指令 + base64 图片块。 */
    fun receiptRequest(config: LlmConfig, imageBase64: String, mediaType: String): HttpRequest {
        val body = buildJsonObject {
            put("model", config.model)
            put("max_tokens", MAX_TOKENS)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "image")
                            put("source", buildJsonObject {
                                put("type", "base64")
                                put("media_type", mediaType)
                                put("data", imageBase64)
                            })
                        })
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", ReceiptCodec.prompt())
                        })
                    })
                })
            })
        }.toString()
        return HttpRequest(
            url = "${config.resolveBaseUrl()}/v1/messages",
            body = body,
            headers = mapOf(
                "Content-Type" to "application/json",
                "x-api-key" to config.apiKey,
                "anthropic-version" to ANTHROPIC_VERSION,
            ),
        )
    }

    /** 连通性测试请求：纯文本。 */
    fun pingRequest(config: LlmConfig): HttpRequest {
        val body = buildJsonObject {
            put("model", config.model)
            put("max_tokens", MAX_TOKENS)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", "请只回复两个字：正常")
                })
            })
        }.toString()
        return HttpRequest(
            url = "${config.resolveBaseUrl()}/v1/messages",
            body = body,
            headers = mapOf(
                "Content-Type" to "application/json",
                "x-api-key" to config.apiKey,
                "anthropic-version" to ANTHROPIC_VERSION,
            ),
        )
    }

    /** 从响应中取首个 text 块内容。 */
    fun extractText(responseJson: String): String {
        val root = Json.parseToJsonElement(responseJson).jsonObject
        return root["content"]!!.jsonArray
            .first { it.jsonObject["type"]!!.jsonPrimitive.content == "text" }
            .jsonObject["text"]!!.jsonPrimitive.content
    }

    /** 错误响应中的可读信息（error.message），取不到返回 null。 */
    fun extractErrorMessage(responseJson: String): String? =
        runCatching {
            Json.parseToJsonElement(responseJson).jsonObject["error"]
                ?.jsonObject?.get("message")?.jsonPrimitive?.content
        }.getOrNull()
}
