package com.wenbo.bookkeeping.data.llm

import com.wenbo.bookkeeping.core.amount.AmountFormat
import com.wenbo.bookkeeping.core.time.MonthUtils
import com.wenbo.bookkeeping.domain.model.TransactionType
import com.wenbo.bookkeeping.domain.model.TxCategory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import java.time.LocalDate

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

    /** 识别指令：要求逐条列出明细行，只返回 JSON。枚举列表由 TxCategory 生成。 */
    fun prompt(todayEpochDay: Long = LocalDate.now().toEpochDay()): String {
        val categories = TxCategory.entries.joinToString(", ") { "${it.name} ${it.label}" }
        val today = LocalDate.ofEpochDay(todayEpochDay).toString()
        return """
            你是记账助手。请识别图片中的票据、账单或支付截图，逐条列出每一笔明细
            （如购物小票的每个商品行、外卖订单的每一项、账单的每个分项）。
            只返回 JSON，不要任何其他文字，格式如下：
            {"items":[{"type":"EXPENSE","amount":"12.50","category":"FOOD","date":"2026-11-09","note":"宫保鸡丁"},{"type":"EXPENSE","amount":"3.00","category":"TRANSPORT","date":"2026-11-09","note":"地铁"}]}
            字段说明：
            - type："EXPENSE"（支出）或 "INCOME"（收入）
            - amount：金额（元），字符串，最多两位小数，不含货币符号
            - category：从以下枚举中选最匹配的一个：$categories
            - date：该明细的日期，格式 yyyy-MM-dd。今天的日期是 $today；
              票据上没有印年份、年份模糊或难以确认时，一律按今天的年份
            - note：该明细的品名或摘要，不超过 30 字
            - 无法拆分明细时（如单笔转账截图），items 里只放一条
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
     * 解析识别 JSON 为明细列表。各字段全部容错：
     * 金额接受字符串/数字、可带货币符号；分类按枚举名或中文标签匹配，未匹配降级「其他」；
     * 日期解析失败用今天；无有效金额的条目被丢弃。
     * 兼容旧单条格式（无 items 数组但顶层有字段）。
     */
    fun parseItems(json: String, todayEpochDay: Long): List<ReceiptParse> {
        val parsed = runCatching { receiptJson.decodeFromString(ReceiptJson.serializer(), json) }
            .getOrNull() ?: return emptyList()
        val raw = if (parsed.items.isNotEmpty()) parsed.items else listOf(parsed.toItem())
        return raw.mapNotNull { it.toParse(todayEpochDay) }
    }

    @Serializable
    private data class ReceiptJson(
        val items: List<ItemJson> = emptyList(),
        // 旧单条格式兼容
        val type: String? = null,
        // 模型可能返回字符串或数字，用 JsonPrimitive 兜住两种形态
        val amount: JsonPrimitive? = null,
        val category: String? = null,
        val date: String? = null,
        val note: String? = null,
    ) {
        fun toItem() = ItemJson(type, amount, category, date, note)
    }

    @Serializable
    private data class ItemJson(
        val type: String? = null,
        val amount: JsonPrimitive? = null,
        val category: String? = null,
        val date: String? = null,
        val note: String? = null,
    )

    private fun ItemJson.toParse(todayEpochDay: Long): ReceiptParse? {
        val amountMinor = amountMinor() ?: return null
        if (amountMinor <= 0L) return null // 0/负数金额视为无效明细
        val type = runCatching { TransactionType.valueOf(type.orEmpty()) }
            .getOrDefault(TransactionType.EXPENSE)
        return ReceiptParse(
            type = type,
            amountMinor = amountMinor,
            category = matchCategory(category, type),
            dateEpochDay = saneDate(date?.let { MonthUtils.isoToDate(it) }, todayEpochDay),
            note = note.orEmpty().take(50),
        )
    }

    /**
     * 日期钳制：未来日期（> 明天）或超过 90 天前的日期几乎必然是模型误读年份
     * （票据通常只印月日），回退到今天，保证记录落在当前月可见。
     */
    private fun saneDate(parsed: Long?, todayEpochDay: Long): Long {
        val date = parsed ?: todayEpochDay
        return when {
            date > todayEpochDay + 1 -> todayEpochDay
            date < todayEpochDay - 90 -> todayEpochDay
            else -> date
        }
    }

    private fun ItemJson.amountMinor(): Long? {
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
