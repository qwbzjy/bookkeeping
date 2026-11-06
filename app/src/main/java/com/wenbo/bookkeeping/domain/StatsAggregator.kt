package com.wenbo.bookkeeping.domain

import com.wenbo.bookkeeping.core.time.MonthUtils
import com.wenbo.bookkeeping.data.db.TransactionEntity
import com.wenbo.bookkeeping.domain.model.TransactionType
import com.wenbo.bookkeeping.domain.model.TxCategory
import java.time.LocalDate
import java.time.YearMonth

/**
 * 统计聚合纯函数：不写 SQL，ViewModel 拉全量后在这里内存聚合，
 * 聚合逻辑因此可被 JUnit 直测（个人记账数据量极小，无性能问题）。
 */
object StatsAggregator {

    /** 分类占比项（环形图 + 图例）。 */
    data class CategoryShare(
        val category: TxCategory,
        val amountMinor: Long,
        val fraction: Float, // 0..1
    )

    /** 月度收支汇总三卡。 */
    data class MonthSummary(
        val expenseMinor: Long,
        val incomeMinor: Long,
        val balanceMinor: Long,
    )

    /** 趋势柱状图数据点。 */
    data class MonthPoint(
        val month: YearMonth,
        val expenseMinor: Long,
        val incomeMinor: Long,
    )

    /** 求一组交易的收支汇总。 */
    fun summary(txs: List<TransactionEntity>): MonthSummary {
        val expense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
        val income = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
        return MonthSummary(expense, income, income - expense)
    }

    /**
     * 分类占比：按金额降序；fraction = amount / total。
     * 仅统计指定类型的交易；总额为 0 时返回空列表。
     */
    fun categoryShares(txs: List<TransactionEntity>, type: TransactionType): List<CategoryShare> {
        val filtered = txs.filter { it.type == type }
        val total = filtered.sumOf { it.amountMinor }
        if (total <= 0L) return emptyList()
        return filtered
            .groupBy { it.category }
            .map { (name, list) ->
                val amount = list.sumOf { it.amountMinor }
                CategoryShare(
                    category = TxCategory.byNameOrDefault(name, type),
                    amountMinor = amount,
                    fraction = amount.toFloat() / total,
                )
            }
            .sortedByDescending { it.amountMinor }
    }

    /** 按天分组（首页时间线）：输入已按 date/createdAt 排序，分组后按日期升序。 */
    fun groupByDay(txs: List<TransactionEntity>): Map<Long, List<TransactionEntity>> =
        txs.groupBy { it.date }.toSortedMap()

    /**
     * 近 [count] 个月的收支趋势：无记录月份补零而非缺失。
     * [endMonth] 为最后一月（通常当前月）。
     */
    fun monthlyTrend(
        txs: List<TransactionEntity>,
        endMonth: YearMonth,
        count: Int = 6,
    ): List<MonthPoint> {
        val startMonth = endMonth.minusMonths((count - 1).toLong())
        val byMonth = txs.groupBy {
            val date = LocalDate.ofEpochDay(it.date)
            YearMonth.of(date.year, date.monthValue)
        }
        return (0 until count).map { offset ->
            val m = startMonth.plusMonths(offset.toLong())
            val s = summary(byMonth[m].orEmpty())
            MonthPoint(m, s.expenseMinor, s.incomeMinor)
        }
    }
}
