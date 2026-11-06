package com.wenbo.bookkeeping.core.time

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonthUtilsTest {

    @Test
    fun `feb 2026 is not leap`() {
        assertEquals(LocalDate.of(2026, 2, 1).toEpochDay(), MonthUtils.firstDay(YearMonth.of(2026, 2)))
        assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), MonthUtils.lastDay(YearMonth.of(2026, 2)))
    }

    @Test
    fun `feb 2028 is leap`() {
        assertEquals(LocalDate.of(2028, 2, 29).toEpochDay(), MonthUtils.lastDay(YearMonth.of(2028, 2)))
    }

    @Test
    fun `month range is closed interval`() {
        val range = MonthUtils.monthRange(YearMonth.of(2026, 11))
        assertEquals(LocalDate.of(2026, 11, 1).toEpochDay(), range.first)
        assertEquals(LocalDate.of(2026, 11, 30).toEpochDay(), range.last)
        // 月内任意一天都在区间内
        assertEquals(true, LocalDate.of(2026, 11, 15).toEpochDay() in range)
    }

    @Test
    fun `prev month across year`() {
        assertEquals(
            YearMonth.of(2025, 12),
            YearMonth.of(2026, 1).minusMonths(1),
        )
    }

    @Test
    fun `next month across year`() {
        assertEquals(
            YearMonth.of(2027, 1),
            YearMonth.of(2026, 12).plusMonths(1),
        )
    }

    @Test
    fun `month label`() {
        assertEquals("2026年11月", MonthUtils.monthLabel(YearMonth.of(2026, 11)))
    }

    @Test
    fun `short month label`() {
        assertEquals("11月", MonthUtils.shortMonthLabel(YearMonth.of(2026, 11)))
    }

    @Test
    fun `day label`() {
        assertEquals("11月6日 周五", MonthUtils.dayLabel(LocalDate.of(2026, 11, 6).toEpochDay()))
    }

    @Test
    fun `iso roundtrip`() {
        val day = LocalDate.of(2026, 11, 1).toEpochDay()
        assertEquals("2026-11-01", MonthUtils.dateToIso(day))
        assertEquals(day, MonthUtils.isoToDate("2026-11-01"))
    }

    @Test
    fun `iso parse invalid returns null`() {
        assertNull(MonthUtils.isoToDate("abc"))
        assertNull(MonthUtils.isoToDate(""))
        assertNull(MonthUtils.isoToDate("2026-13-01"))
    }
}
