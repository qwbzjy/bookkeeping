package com.wenbo.bookkeeping.domain

import com.wenbo.bookkeeping.data.db.TransactionEntity
import com.wenbo.bookkeeping.domain.model.TransactionType
import com.wenbo.bookkeeping.domain.model.TxCategory
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsAggregatorTest {

    private fun tx(
        type: TransactionType,
        amountMinor: Long,
        category: TxCategory,
        date: LocalDate = LocalDate.of(2026, 11, 6),
        createdAt: Long = 0L,
    ) = TransactionEntity(
        type = type,
        amountMinor = amountMinor,
        category = category.name,
        date = date.toEpochDay(),
        note = "",
        createdAt = createdAt,
    )

    // summary

    @Test
    fun `summary nets income against expense`() {
        val s = StatsAggregator.summary(
            listOf(
                tx(TransactionType.EXPENSE, 2500, TxCategory.FOOD),
                tx(TransactionType.EXPENSE, 1000, TxCategory.TRANSPORT),
                tx(TransactionType.INCOME, 950_000, TxCategory.SALARY),
            ),
        )
        assertEquals(3500L, s.expenseMinor)
        assertEquals(950_000L, s.incomeMinor)
        assertEquals(946_500L, s.balanceMinor)
    }

    @Test
    fun `summary empty`() {
        val s = StatsAggregator.summary(emptyList())
        assertEquals(0L, s.expenseMinor)
        assertEquals(0L, s.incomeMinor)
        assertEquals(0L, s.balanceMinor)
    }

    // categoryShares

    @Test
    fun `shares sorted by amount desc with fractions`() {
        val shares = StatsAggregator.categoryShares(
            listOf(
                tx(TransactionType.EXPENSE, 6000, TxCategory.FOOD),
                tx(TransactionType.EXPENSE, 3000, TxCategory.TRANSPORT),
                tx(TransactionType.EXPENSE, 1000, TxCategory.FUN),
                tx(TransactionType.INCOME, 999, TxCategory.SALARY), // 不计入支出占比
            ),
            TransactionType.EXPENSE,
        )
        assertEquals(3, shares.size)
        assertEquals(TxCategory.FOOD, shares[0].category)
        assertEquals(6000L, shares[0].amountMinor)
        assertEquals(0.6f, shares[0].fraction, 0.0001f)
        assertEquals(TxCategory.TRANSPORT, shares[1].category)
        assertEquals(TxCategory.FUN, shares[2].category)
    }

    @Test
    fun `shares empty when total zero`() {
        assertEquals(
            emptyList<StatsAggregator.CategoryShare>(),
            StatsAggregator.categoryShares(emptyList(), TransactionType.EXPENSE),
        )
    }

    // groupByDay

    @Test
    fun `group by day keeps entries`() {
        val d1 = LocalDate.of(2026, 11, 6)
        val d2 = LocalDate.of(2026, 11, 7)
        val grouped = StatsAggregator.groupByDay(
            listOf(
                tx(TransactionType.EXPENSE, 100, TxCategory.FOOD, d1),
                tx(TransactionType.EXPENSE, 200, TxCategory.FUN, d2),
                tx(TransactionType.EXPENSE, 300, TxCategory.TRANSPORT, d1),
            ),
        )
        assertEquals(setOf(d1.toEpochDay(), d2.toEpochDay()), grouped.keys)
        assertEquals(2, grouped[d1.toEpochDay()]?.size)
        assertEquals(1, grouped[d2.toEpochDay()]?.size)
    }

    // monthlyTrend

    @Test
    fun `trend fills zero months`() {
        val nov = LocalDate.of(2026, 11, 6)
        val sep = LocalDate.of(2026, 9, 10)
        val trend = StatsAggregator.monthlyTrend(
            listOf(
                tx(TransactionType.EXPENSE, 1000, TxCategory.FOOD, nov),
                tx(TransactionType.INCOME, 5000, TxCategory.SALARY, sep),
            ),
            endMonth = YearMonth.of(2026, 11),
            count = 6,
        )
        assertEquals(6, trend.size)
        assertEquals(YearMonth.of(2026, 6), trend[0].month)
        assertEquals(0L, trend[0].expenseMinor)
        assertEquals(YearMonth.of(2026, 9), trend[3].month)
        assertEquals(5000L, trend[3].incomeMinor)
        assertEquals(YearMonth.of(2026, 11), trend[5].month)
        assertEquals(1000L, trend[5].expenseMinor)
    }

    @Test
    fun `trend crosses year boundary`() {
        val trend = StatsAggregator.monthlyTrend(
            emptyList(),
            endMonth = YearMonth.of(2026, 2),
            count = 6,
        )
        assertEquals(YearMonth.of(2025, 9), trend.first().month)
        assertEquals(YearMonth.of(2026, 2), trend.last().month)
    }
}
