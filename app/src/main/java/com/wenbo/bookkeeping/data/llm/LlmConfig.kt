package com.wenbo.bookkeeping.data.llm

import com.wenbo.bookkeeping.data.db.SettingsDao
import com.wenbo.bookkeeping.data.db.SettingsEntity

/** 大模型服务协议类型：OpenAI 兼容（chat/completions）或 Anthropic（messages）。 */
enum class LlmProvider { OPENAI, ANTHROPIC }

/**
 * 大模型服务配置：用户在设置页填写，存于 Room settings 表。
 * baseUrl 留空时按协议用官方默认地址；兼容端点（如方舟、DeepSeek）填自定义地址。
 */
data class LlmConfig(
    val provider: LlmProvider = LlmProvider.OPENAI,
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && model.isNotBlank()

    /** 实际请求的基础地址（不含路径）。 */
    fun resolveBaseUrl(): String = baseUrl.trim().trimEnd('/').ifBlank {
        if (provider == LlmProvider.OPENAI) "https://api.openai.com/v1" else "https://api.anthropic.com"
    }

    companion object {
        const val KEY_PROVIDER = "llm_provider"
        const val KEY_BASE_URL = "llm_base_url"
        const val KEY_API_KEY = "llm_api_key"
        const val KEY_MODEL = "llm_model"

        fun allKeys() = listOf(KEY_PROVIDER, KEY_BASE_URL, KEY_API_KEY, KEY_MODEL)
    }
}

/** LlmConfig 与 settings 表之间的读写收口。 */
class LlmSettingsStore(private val dao: SettingsDao) {

    suspend fun load(): LlmConfig {
        val map = dao.getAll().associate { it.key to it.value }
        return LlmConfig(
            provider = map[LlmConfig.KEY_PROVIDER]?.let { p ->
                runCatching { LlmProvider.valueOf(p) }.getOrNull()
            } ?: LlmProvider.OPENAI,
            baseUrl = map[LlmConfig.KEY_BASE_URL].orEmpty(),
            apiKey = map[LlmConfig.KEY_API_KEY].orEmpty(),
            model = map[LlmConfig.KEY_MODEL].orEmpty(),
        )
    }

    suspend fun save(config: LlmConfig) {
        val rows = mapOf(
            LlmConfig.KEY_PROVIDER to config.provider.name,
            LlmConfig.KEY_BASE_URL to config.baseUrl,
            LlmConfig.KEY_API_KEY to config.apiKey,
            LlmConfig.KEY_MODEL to config.model,
        )
        LlmConfig.allKeys().forEach { key ->
            dao.upsert(SettingsEntity(key, rows[key].orEmpty()))
        }
    }
}
