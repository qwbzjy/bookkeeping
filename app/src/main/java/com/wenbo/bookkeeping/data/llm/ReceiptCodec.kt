package com.wenbo.bookkeeping.data.llm

import com.wenbo.bookkeeping.core.amount.AmountFormat
import com.wenbo.bookkeeping.core.time.MonthUtils
import com.wenbo.bookkeeping.domain.model.TransactionType
import com.wenbo.bookkeeping.domain.model.TxCategory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

/**
 * 票据识别结果：已换算为可入表单的形态。
 */
data class ReceiptParse(
    val type: TransactionType,
    val amountMinor: Long,
    val category: TxCategory,
    val dateEpochDay: Long,
    val note: String,
)

/**
 * 模型回复 → 结构化识别结果的纯函数解析：
 * 提示词构造、JSON 块提取、字段容错映射。可完整单测。
 */
object ReceiptCodec {

    private val receiptJson = Json { ignoreUnknownKeys = true }

    /** 识别指令：要求模型只返回 JSON，枚举列表由 TxCategory 生成。 */
    fun prompt(): String {
        val categories = TxCategory.entries.joinToString(", ") { "${it.name} ${it.label}" }
        return """
            你是记账助手。请识别图片中的票据、账单或支付截图，归纳为一笔记录。
            只返回 JSON，不要任何其他文字，格式如下：
            {"type":"EXPENSE","amount":"25.50","category":"FOOD","date":"2026-11-09","note":"摘要"}
            字段说明：
            - type："EXPENSE"（支出）或 "INCOME"（收入）
            - amount：金额（元），字符串，最多两位小数，不含货币符号
            - category：从以下枚举中选最匹配的一个：$categories
            - date：票据日期，格式 yyyy-MM-dd；看不出日期时用今天
            - note：不超过 30 字的消费摘要
        """.trimIndent()
    }

    /**
     * 从模型回复文本中提取 JSON 块：容忍 ```json 围栏与前后说明文字。
     * 取第一个 { 到最后一个 } 之间内容；找不到返回 null。
     */
    fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return text.substring(start, end + 1)
    }

    /**
     * 解析识别 JSON。各字段全部容错：
     * 金额接受字符串/数字、可带货币符号；分类按枚举名或中文标签匹配，未匹配降级「其他」；
     * 日期解析失败用今天；解析不出金额返回 null（调用方报错）。
     */
    fun parse(json: String, todayEpochDay: Long): ReceiptParse? {
        val parsed = runCatching { receiptJson.decodeFromString(ReceiptJson.serializer(), json) }
            .getOrNull() ?: return null
        val amountMinor = parsed.amountMinor() ?: return null
        val type = runCatching { TransactionType.valueOf(parsed.type.orEmpty()) }
            .getOrDefault(TransactionType.EXPENSE)
        val category = matchCategory(parsed.category, type)
        return ReceiptParse(
            type = type,
            amountMinor = amountMinor,
            category = category,
            dateEpochDay = parsed.date?.let { MonthUtils.isoToDate(it) } ?: todayEpochDay,
            note = parsed.note.orEmpty().take(50),
        )
    }

    @Serializable
    private data class ReceiptJson(
        val type: String? = null,
        // 模型可能返回字符串或数字，用 JsonPrimitive 兜住两种形态
        val amount: JsonPrimitive? = null,
        val category: String? = null,
        val date: String? = null,
        val note: String? = null,
    )

    private fun ReceiptJson.amountMinor(): Long? {
        val raw = amount?.content ?: return null
        // 去掉货币符号、千分位等非数字字符后按元解析
        val cleaned = raw.filter { it.isDigit() || it == '.' }
        return AmountFormat.parseYuanToMinor(cleaned)
            // 超过两位小数等非法形态再退一层：按数字直接换算
            ?: cleaned.toDoubleOrNull()?.let { (it * 100).toLong() }
    }

    private fun matchCategory(name: String?, type: TransactionType): TxCategory {
        if (!name.isNullOrBlank()) {
            TxCategory.entries.firstOrNull { it.name == name }?.let { return it }
            TxCategory.entries.firstOrNull { it.label == name.trim() }?.let { return it }
        }
        return TxCategory.byNameOrDefault(name, type)
    }
}
