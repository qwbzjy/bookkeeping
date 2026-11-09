package com.wenbo.bookkeeping.data.llm

import com.wenbo.bookkeeping.domain.model.TransactionType
import com.wenbo.bookkeeping.domain.model.TxCategory
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptCodecTest {

    private val today = LocalDate.of(2026, 11, 9).toEpochDay()

    // prompt

    @Test
    fun `prompt contains all category names and json schema`() {
        val prompt = ReceiptCodec.prompt()
        TxCategory.entries.forEach { assert(prompt.contains(it.name)) }
        assert(prompt.contains("EXPENSE"))
        assert(prompt.contains("yyyy-MM-dd"))
    }

    // extractJson

    @Test
    fun `extract json from fenced block`() {
        val text = "以下是识别结果：\n```json\n{\"type\":\"EXPENSE\",\"amount\":\"25.50\"}\n```\n谢谢"
        assertEquals("{\"type\":\"EXPENSE\",\"amount\":\"25.50\"}", ReceiptCodec.extractJson(text))
    }

    @Test
    fun `extract json with prose around`() {
        val text = "识别结果 {\"a\":1} 请确认"
        assertEquals("{\"a\":1}", ReceiptCodec.extractJson(text))
    }

    @Test
    fun `extract json returns null when absent`() {
        assertNull(ReceiptCodec.extractJson("抱歉，无法识别"))
        assertNull(ReceiptCodec.extractJson(""))
    }

    // parse

    @Test
    fun `parse full valid receipt`() {
        val json = """
            {"type":"INCOME","amount":"9500.00","category":"SALARY","date":"2026-11-01","note":"十月工资"}
        """.trimIndent()
        val parse = ReceiptCodec.parse(json, today)!!
        assertEquals(TransactionType.INCOME, parse.type)
        assertEquals(950_000L, parse.amountMinor)
        assertEquals(TxCategory.SALARY, parse.category)
        assertEquals(LocalDate.of(2026, 11, 1).toEpochDay(), parse.dateEpochDay)
        assertEquals("十月工资", parse.note)
    }

    @Test
    fun `parse amount as number and with currency symbols`() {
        assertEquals(2550L, ReceiptCodec.parse("""{"amount":25.5}""", today)!!.amountMinor)
        assertEquals(2550L, ReceiptCodec.parse("""{"amount":"¥25.50"}""", today)!!.amountMinor)
        assertEquals(1234560L, ReceiptCodec.parse("""{"amount":"12,345.60"}""", today)!!.amountMinor)
    }

    @Test
    fun `parse category by chinese label`() {
        assertEquals(TxCategory.FOOD, ReceiptCodec.parse("""{"amount":"10","category":"餐饮"}""", today)!!.category)
        assertEquals(TxCategory.TRANSPORT, ReceiptCodec.parse("""{"amount":"10","category":"交通"}""", today)!!.category)
    }

    @Test
    fun `parse unknown category falls back by type`() {
        assertEquals(
            TxCategory.OTHER_EXPENSE,
            ReceiptCodec.parse("""{"amount":"10","category":"MYSTERY"}""", today)!!.category,
        )
        assertEquals(
            TxCategory.OTHER_INCOME,
            ReceiptCodec.parse("""{"type":"INCOME","amount":"10","category":"MYSTERY"}""", today)!!.category,
        )
    }

    @Test
    fun `parse missing or bad date falls back to today`() {
        assertEquals(today, ReceiptCodec.parse("""{"amount":"10"}""", today)!!.dateEpochDay)
        assertEquals(today, ReceiptCodec.parse("""{"amount":"10","date":"not-a-date"}""", today)!!.dateEpochDay)
    }

    @Test
    fun `parse defaults type to expense`() {
        assertEquals(TransactionType.EXPENSE, ReceiptCodec.parse("""{"amount":"10"}""", today)!!.type)
    }

    @Test
    fun `parse returns null when amount missing or invalid`() {
        assertNull(ReceiptCodec.parse("""{"type":"EXPENSE"}""", today))
        assertNull(ReceiptCodec.parse("""{"amount":""}""", today))
        assertNull(ReceiptCodec.parse("not json at all", today))
    }

    @Test
    fun `parse ignores unknown fields and extra prose json`() {
        val json = """{"type":"EXPENSE","amount":"12.00","category":"FOOD","date":"2026-11-09","note":"午饭","confidence":0.98}"""
        val parse = ReceiptCodec.parse(json, today)!!
        assertEquals("午饭", parse.note)
    }
}
