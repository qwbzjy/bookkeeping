package com.wenbo.bookkeeping.data.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmConfigTest {

    // headers JSON 往返

    @Test
    fun `headers json roundtrip`() {
        val headers = mapOf("X-Session-Id" to "abc123", "X-Tenant" to "t1")
        assertEquals(headers, LlmConfig.headersFromJson(LlmConfig.headersToJson(headers)))
    }

    @Test
    fun `headers from bad json returns empty`() {
        assertTrue(LlmConfig.headersFromJson(null).isEmpty())
        assertTrue(LlmConfig.headersFromJson("").isEmpty())
        assertTrue(LlmConfig.headersFromJson("not json").isEmpty())
    }

    @Test
    fun `empty headers encode and decode`() {
        assertTrue(LlmConfig.headersFromJson(LlmConfig.headersToJson(emptyMap())).isEmpty())
    }

    // isConfigured

    @Test
    fun `configured requires key and model`() {
        assertTrue(!LlmConfig().isConfigured)
        assertTrue(LlmConfig(apiKey = "k", model = "m").isConfigured)
        assertTrue(!LlmConfig(apiKey = "k").isConfigured)
    }
}
