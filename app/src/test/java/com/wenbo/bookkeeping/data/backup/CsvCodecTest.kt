package com.wenbo.bookkeeping.data.backup

import com.wenbo.bookkeeping.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvCodecTest {

    // encode / decode 往返

    @Test
    fun `tx roundtrip`() {
        val row = BackupRow(
            kind = "tx",
            id = 12L,
            type = "EXPENSE",
            amountMinor = 2580L,
            category = "FOOD",
            date = "2026-11-01",
            note = "午饭加蛋",
            createdAt = 1_793_577_600_000L,
        )
        val decoded = CsvCodec.decode(CsvCodec.encode(listOf(row)))
        assertEquals(0, decoded.skipped)
        assertEquals(1, decoded.rows.size)
        assertEquals(row, decoded.rows[0])
    }

    @Test
    fun `budget roundtrip`() {
        val row = BackupRow(kind = "budget", amountMinor = 150_000L, category = "FOOD")
        val decoded = CsvCodec.decode(CsvCodec.encode(listOf(row)))
        assertEquals(0, decoded.skipped)
        assertEquals(row, decoded.rows[0])
    }

    // 转义

    @Test
    fun `note with comma quote newline roundtrips`() {
        val row = BackupRow(
            kind = "tx",
            id = 1L,
            type = "INCOME",
            amountMinor = 100L,
            category = "SALARY",
            date = "2026-11-01",
            note = "含,逗号 \"引号\"\n换行",
            createdAt = 1L,
        )
        val decoded = CsvCodec.decode(CsvCodec.encode(listOf(row)))
        assertEquals(0, decoded.skipped)
        assertEquals("含,逗号 \"引号\"\n换行", decoded.rows[0].note)
    }

    // BOM

    @Test
    fun `decode strips bom`() {
        val csv = CsvCodec.BOM + "kind,id,type,amountMinor,category,date,note,createdAt\r\n" +
            "tx,1,EXPENSE,100,FOOD,2026-11-01,午饭,1\r\n"
        val decoded = CsvCodec.decode(csv)
        assertEquals(1, decoded.rows.size)
        assertEquals("FOOD", decoded.rows[0].category)
    }

    @Test
    fun `decode without bom works`() {
        val csv = "tx,1,EXPENSE,100,FOOD,2026-11-01,午饭,1\r\n"
        val decoded = CsvCodec.decode(csv)
        assertEquals(1, decoded.rows.size)
    }

    // 容错

    @Test
    fun `bad rows skipped with count`() {
        val csv = "kind,id,type,amountMinor,category,date,note,createdAt\r\n" +
            "tx,1,EXPENSE,100,FOOD,2026-11-01,,1\r\n" + // 正常
            "tx,2,WEIRD,100,FOOD,2026-11-01,,1\r\n" + // type 非法
            "tx,3,EXPENSE\r\n" + // 列数不足
            "budget,,,150,FOOD,,,\r\n" // 正常
        val decoded = CsvCodec.decode(csv)
        assertEquals(2, decoded.rows.size)
        assertEquals(2, decoded.skipped)
    }

    @Test
    fun `unknown category falls back to other`() {
        val csv = "tx,1,EXPENSE,100,NO_SUCH,2026-11-01,,1\r\n"
        val decoded = CsvCodec.decode(csv)
        assertEquals(1, decoded.rows.size)
        assertEquals("OTHER_EXPENSE", decoded.rows[0].category)
    }

    @Test
    fun `income unknown category falls back to income other`() {
        val csv = "tx,1,INCOME,100,NO_SUCH,2026-11-01,,1\r\n"
        val decoded = CsvCodec.decode(csv)
        assertEquals("OTHER_INCOME", decoded.rows[0].category)
    }

    @Test
    fun `empty and header-only inputs`() {
        assertTrue(CsvCodec.decode("").rows.isEmpty())
        assertEquals(0, CsvCodec.decode("kind,id,type,amountMinor,category,date,note,createdAt\r\n").skipped)
    }

    @Test
    fun `missing date or amount skipped`() {
        val csv = "tx,1,EXPENSE,,FOOD,2026-11-01,,1\r\n" + // 无金额
            "tx,1,EXPENSE,100,FOOD,not-a-date,,1\r\n" // 日期非法
        val decoded = CsvCodec.decode(csv)
        assertEquals(0, decoded.rows.size)
        assertEquals(2, decoded.skipped)
    }

    @Test
    fun `missing id parses as null`() {
        val csv = "tx,,EXPENSE,100,FOOD,2026-11-01,,1\r\n"
        val decoded = CsvCodec.decode(csv)
        assertNull(decoded.rows[0].id)
    }

    // encode 后 type 值正确性（导入用）

    @Test
    fun `decode maps type name`() {
        val decoded = CsvCodec.decode("tx,1,INCOME,100,SALARY,2026-11-01,,1\r\n")
        assertEquals(TransactionType.INCOME.name, decoded.rows[0].type)
    }
}
