package com.wenbo.bookkeeping.data.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiCodecTest {

    private val config = LlmConfig(
        provider = LlmProvider.OPENAI,
        baseUrl = "",
        apiKey = "sk-test",
        model = "gpt-4o-mini",
    )

    @Test
    fun `default base url resolves to official`() {
        assertEquals("https://api.openai.com/v1", config.resolveBaseUrl())
    }

    @Test
    fun `custom base url trimmed`() {
        assertEquals(
            "https://ark.cn-beijing.volces.com/api/v3",
            config.copy(baseUrl = "https://ark.cn-beijing.volces.com/api/v3/").resolveBaseUrl(),
        )
    }

    @Test
    fun `receipt request has data uri image and bearer auth`() {
        val request = OpenAiCodec.receiptRequest(config, "QUJD", "image/jpeg")
        assertEquals("https://api.openai.com/v1/chat/completions", request.url)
        assertTrue(request.headers["Authorization"] == "Bearer sk-test")
        assertTrue(request.body.contains("\"model\":\"gpt-4o-mini\""))
        assertTrue(request.body.contains("data:image/jpeg;base64,QUJD"))
        assertTrue(request.body.contains(ReceiptCodec.prompt().take(20)))
    }

    @Test
    fun `extract text from choices`() {
        val response = """
            {"choices":[{"message":{"role":"assistant","content":"{\"amount\":\"25.50\"}"}}]}
        """.trimIndent()
        assertEquals("{\"amount\":\"25.50\"}", OpenAiCodec.extractText(response))
    }

    @Test
    fun `extract error message`() {
        val response = """{"error":{"message":"Incorrect API key","type":"auth"}}"""
        assertEquals("Incorrect API key", OpenAiCodec.extractErrorMessage(response))
        assertNull(OpenAiCodec.extractErrorMessage("""{"choices":[]}"""))
    }
}
