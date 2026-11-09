package com.wenbo.bookkeeping.data.llm

import com.wenbo.bookkeeping.domain.model.TransactionType
import com.wenbo.bookkeeping.domain.model.TxCategory
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptCodecTest {

    private val today = LocalDate.of(2026, 11, 9).toEpochDay()

    // prompt

    @Test
    fun `prompt contains all category names and items schema`() {
        val prompt = ReceiptCodec.prompt()
        TxCategory.entries.forEach { assertTrue(prompt.contains(it.name)) }
        assertTrue(prompt.contains("items"))
        assertTrue(prompt.contains("yyyy-MM-dd"))
    }

    // extractJson

    @Test
    fun `extract json from fenced block`() {
        val text = "以下是识别结果：\n```json\n{\"items\":[]}\n```\n谢谢"
        assertEquals("{\"items\":[]}", ReceiptCodec.extractJson(text))
    }

    @Test
    fun `extract json with prose around`() {
        val text = "识别结果 {\"a\":1} 请确认"
        assertEquals("{\"a\":1}", ReceiptCodec.extractJson(text))
    }

    @Test
    fun `extract json returns null when absent`() {
        assertTrue(ReceiptCodec.extractJson("抱歉，无法识别") == null)
        assertTrue(ReceiptCodec.extractJson("") == null)
    }

    // parseItems

    @Test
    fun `parse multiple items`() {
        val json = """
            {"items":[
                {"type":"EXPENSE","amount":"12.50","category":"FOOD","date":"2026-11-09","note":"宫保鸡丁"},
                {"type":"EXPENSE","amount":"3.00","category":"TRANSPORT","date":"2026-11-09","note":"地铁"}
            ]}
        """.trimIndent()
        val items = ReceiptCodec.parseItems(json, today)
        assertEquals(2, items.size)
        assertEquals(TxCategory.FOOD, items[0].category)
        assertEquals(1250L, items[0].amountMinor)
        assertEquals("宫保鸡丁", items[0].note)
        assertEquals(TxCategory.TRANSPORT, items[1].category)
        assertEquals(300L, items[1].amountMinor)
    }

    @Test
    fun `parse legacy single record format`() {
        val json = """
            {"type":"INCOME","amount":"9500.00","category":"SALARY","date":"2026-11-01","note":"十月工资"}
        """.trimIndent()
        val items = ReceiptCodec.parseItems(json, today)
        assertEquals(1, items.size)
        assertEquals(TransactionType.INCOME, items[0].type)
        assertEquals(950_000L, items[0].amountMinor)
        assertEquals(TxCategory.SALARY, items[0].category)
        assertEquals(LocalDate.of(2026, 11, 1).toEpochDay(), items[0].dateEpochDay)
    }

    @Test
    fun `parse amount as number and with currency symbols`() {
        assertEquals(2550L, ReceiptCodec.parseItems("""{"amount":25.5}""", today)[0].amountMinor)
        assertEquals(2550L, ReceiptCodec.parseItems("""{"amount":"¥25.50"}""", today)[0].amountMinor)
        assertEquals(1234560L, ReceiptCodec.parseItems("""{"amount":"12,345.60"}""", today)[0].amountMinor)
    }

    @Test
    fun `items without valid amount are dropped`() {
        val json = """
            {"items":[
                {"amount":"12.50","category":"FOOD"},
                {"amount":"","category":"FUN"},
                {"note":"无金额"},
                {"amount":0,"category":"DAILY"}
            ]}
        """.trimIndent()
        val items = ReceiptCodec.parseItems(json, today)
        assertEquals(1, items.size)
        assertEquals(TxCategory.FOOD, items[0].category)
    }

    @Test
    fun `parse category by chinese label`() {
        assertEquals(
            TxCategory.FOOD,
            ReceiptCodec.parseItems("""{"amount":"10","category":"餐饮"}""", today)[0].category,
        )
        assertEquals(
            TxCategory.TRANSPORT,
            ReceiptCodec.parseItems("""{"amount":"10","category":"交通"}""", today)[0].category,
        )
    }

    @Test
    fun `parse unknown category falls back by type`() {
        assertEquals(
            TxCategory.OTHER_EXPENSE,
            ReceiptCodec.parseItems("""{"amount":"10","category":"MYSTERY"}""", today)[0].category,
        )
        assertEquals(
            TxCategory.OTHER_INCOME,
            ReceiptCodec.parseItems("""{"type":"INCOME","amount":"10","category":"MYSTERY"}""", today)[0].category,
        )
    }

    @Test
    fun `parse missing or bad date falls back to today`() {
        assertEquals(today, ReceiptCodec.parseItems("""{"amount":"10"}""", today)[0].dateEpochDay)
        assertEquals(today, ReceiptCodec.parseItems("""{"amount":"10","date":"not-a-date"}""", today)[0].dateEpochDay)
    }

    @Test
    fun `parse defaults type to expense`() {
        assertEquals(TransactionType.EXPENSE, ReceiptCodec.parseItems("""{"amount":"10"}""", today)[0].type)
    }

    @Test
    fun `parse invalid json returns empty`() {
        assertTrue(ReceiptCodec.parseItems("not json at all", today).isEmpty())
        assertTrue(ReceiptCodec.parseItems("""{"items":[]}""", today).isEmpty())
    }
}
