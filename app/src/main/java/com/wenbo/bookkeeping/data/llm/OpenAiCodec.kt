package com.wenbo.bookkeeping.data.llm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * OpenAI 兼容协议编解码（chat/completions，多模态 image_url）。
 * 纯 JSON 构造与解析，不碰网络，可完整单测。
 */
object OpenAiCodec {

    /** 票据识别请求：文本指令 + base64 图片（data URI）。 */
    fun receiptRequest(config: LlmConfig, imageBase64: String, mediaType: String): HttpRequest {
        val body = buildJsonObject {
            put("model", config.model)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", ReceiptCodec.prompt())
                        })
                        add(buildJsonObject {
                            put("type", "image_url")
                            put("image_url", buildJsonObject {
                                put("url", "data:$mediaType;base64,$imageBase64")
                            })
                        })
                    })
                })
            })
        }.toString()
        return HttpRequest(
            url = "${config.resolveBaseUrl()}/chat/completions",
            body = body,
            // 自定义 header 后置合并：同名覆盖默认（便于接自定义鉴权网关）
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer ${config.apiKey}",
            ) + config.customHeaders,
        )
    }

    /** 连通性测试请求：纯文本。 */
    fun pingRequest(config: LlmConfig): HttpRequest {
        val body = buildJsonObject {
            put("model", config.model)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", "请只回复两个字：正常")
                })
            })
        }.toString()
        return HttpRequest(
            url = "${config.resolveBaseUrl()}/chat/completions",
            body = body,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer ${config.apiKey}",
            ) + config.customHeaders,
        )
    }

    /** 从响应中取 choices[0].message.content；解析失败抛异常由调用方转错误提示。 */
    fun extractText(responseJson: String): String {
        val root = Json.parseToJsonElement(responseJson).jsonObject
        return root["choices"]!!
            .jsonArray[0].jsonObject["message"]!!
            .jsonObject["content"]!!.jsonPrimitive.content
    }

    /** 错误响应中的可读信息（error.message），取不到返回 null。 */
    fun extractErrorMessage(responseJson: String): String? =
        runCatching {
            Json.parseToJsonElement(responseJson).jsonObject["error"]
                ?.jsonObject?.get("message")?.jsonPrimitive?.content
        }.getOrNull()
}
