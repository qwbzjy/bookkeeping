package com.wenbo.bookkeeping.core.time

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 日期工具：交易日存 epochDay（Long），月范围查询就是两个数字的闭区间，
 * 无时区歧义。所有换算收口在这里，保证 [from, to] 边界一致。
 */
object MonthUtils {

    /** 月份首日的 epochDay。 */
    fun firstDay(month: YearMonth): Long = month.atDay(1).toEpochDay()

    /** 月份末日的 epochDay（自动处理闰年）。 */
    fun lastDay(month: YearMonth): Long = month.atEndOfMonth().toEpochDay()

    /** 月范围查询的闭区间 [from, to]。 */
    fun monthRange(month: YearMonth): LongRange = firstDay(month)..lastDay(month)

    /** 首页分组头：如 "11月6日 周五"。 */
    fun dayLabel(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        val week = when (date.dayOfWeek.value) {
            1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"
            5 -> "周五"; 6 -> "周六"; else -> "周日"
        }
        return "${date.monthValue}月${date.dayOfMonth}日 $week"
    }

    /** 月切换条文案：如 "2026年11月"。 */
    fun monthLabel(month: YearMonth): String = "${month.year}年${month.monthValue}月"

    /** 趋势图横轴文案：如 "11月"。 */
    fun shortMonthLabel(month: YearMonth): String = "${month.monthValue}月"

    /** CSV 导出用的 ISO 日期。 */
    private val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun dateToIso(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(isoDate)

    /** ISO 日期 → epochDay，解析失败返回 null（CSV 容错用）。 */
    fun isoToDate(text: String): Long? =
        runCatching { LocalDate.parse(text.trim(), isoDate).toEpochDay() }.getOrNull()
}
