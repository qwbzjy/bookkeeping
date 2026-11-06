package com.wenbo.bookkeeping.core.amount

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountFormatTest {

    // formatMinorToYuan

    @Test
    fun `format 2580 to 25_80`() {
        assertEquals("25.80", AmountFormat.formatMinorToYuan(2580))
    }

    @Test
    fun `format zero`() {
        assertEquals("0.00", AmountFormat.formatMinorToYuan(0))
    }

    @Test
    fun `format single fen`() {
        assertEquals("0.05", AmountFormat.formatMinorToYuan(5))
    }

    @Test
    fun `format large amount`() {
        assertEquals("1234567.89", AmountFormat.formatMinorToYuan(123456789))
    }

    @Test
    fun `format negative keeps sign`() {
        assertEquals("-12.34", AmountFormat.formatMinorToYuan(-1234))
    }

    // parseYuanToMinor

    @Test
    fun `parse 12_5`() {
        assertEquals(1250L, AmountFormat.parseYuanToMinor("12.5"))
    }

    @Test
    fun `parse integer yuan`() {
        assertEquals(1200L, AmountFormat.parseYuanToMinor("12"))
    }

    @Test
    fun `parse two decimals`() {
        assertEquals(1250L, AmountFormat.parseYuanToMinor("12.50"))
    }

    @Test
    fun `parse empty returns null`() {
        assertNull(AmountFormat.parseYuanToMinor(""))
        assertNull(AmountFormat.parseYuanToMinor("   "))
    }

    @Test
    fun `parse invalid returns null`() {
        assertNull(AmountFormat.parseYuanToMinor("abc"))
        assertNull(AmountFormat.parseYuanToMinor("12.345"))
        assertNull(AmountFormat.parseYuanToMinor("1.2.3"))
        assertNull(AmountFormat.parseYuanToMinor("-5"))
    }

    // sanitizeInput

    @Test
    fun `sanitize keeps digits and first dot`() {
        assertEquals("12.50", AmountFormat.sanitizeInput("12.50"))
    }

    @Test
    fun `sanitize drops letters`() {
        assertEquals("125", AmountFormat.sanitizeInput("a1b2c5"))
    }

    @Test
    fun `sanitize keeps only one dot`() {
        // 多余的点丢弃，后续数字仍进入小数位（受两位限制）
        assertEquals("12.58", AmountFormat.sanitizeInput("12.5.8"))
    }

    @Test
    fun `sanitize limits two decimals`() {
        assertEquals("12.34", AmountFormat.sanitizeInput("12.345"))
    }

    @Test
    fun `sanitize empty stays empty`() {
        assertEquals("", AmountFormat.sanitizeInput(""))
    }

    // formatCompact

    @Test
    fun `compact below wan shows yuan`() {
        assertEquals("9999.99", AmountFormat.formatCompact(999_999))
    }

    @Test
    fun `compact at wan shows wan`() {
        assertEquals("1.0万", AmountFormat.formatCompact(1_000_000))
    }

    @Test
    fun `compact large wan drops decimals`() {
        assertEquals("100万", AmountFormat.formatCompact(100_000_000))
    }
}
