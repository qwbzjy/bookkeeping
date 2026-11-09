package com.wenbo.bookkeeping.data.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnthropicCodecTest {

    private val config = LlmConfig(
        provider = LlmProvider.ANTHROPIC,
        baseUrl = "",
        apiKey = "ak-test",
        model = "claude-sonnet-4-5",
    )

    @Test
    fun `default base url resolves to official`() {
        assertEquals("https://api.anthropic.com", config.resolveBaseUrl())
    }

    @Test
    fun `receipt request uses base64 source and version header`() {
        val request = AnthropicCodec.receiptRequest(config, "QUJD", "image/jpeg")
        assertEquals("https://api.anthropic.com/v1/messages", request.url)
        assertEquals("ak-test", request.headers["x-api-key"])
        assertTrue(request.headers.containsKey("anthropic-version"))
        assertTrue(request.body.contains("\"media_type\":\"image/jpeg\""))
        assertTrue(request.body.contains("\"data\":\"QUJD\""))
        assertTrue(request.body.contains("\"type\":\"base64\""))
        assertTrue(request.body.contains("\"max_tokens\":1024"))
    }

    @Test
    fun `extract text from first text block`() {
        val response = """
            {"content":[{"type":"text","text":"{\"amount\":\"25.50\"}"}]}
        """.trimIndent()
        assertEquals("{\"amount\":\"25.50\"}", AnthropicCodec.extractText(response))
    }

    @Test
    fun `extract text skips non-text blocks`() {
        val response = """
            {"content":[{"type":"tool_use","id":"x"},{"type":"text","text":"结果"}]}
        """.trimIndent()
        assertEquals("结果", AnthropicCodec.extractText(response))
    }

    @Test
    fun `extract error message`() {
        val response = """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}"""
        assertEquals("invalid x-api-key", AnthropicCodec.extractErrorMessage(response))
        assertNull(AnthropicCodec.extractErrorMessage("""{"content":[]}"""))
    }

    @Test
    fun `custom headers appended`() {
        val withHeaders = config.copy(customHeaders = mapOf("X-Session-Id" to "abc123"))
        assertEquals("abc123", AnthropicCodec.receiptRequest(withHeaders, "QUJD", "image/jpeg").headers["X-Session-Id"])
        assertEquals("abc123", AnthropicCodec.pingRequest(withHeaders).headers["X-Session-Id"])
    }
}
