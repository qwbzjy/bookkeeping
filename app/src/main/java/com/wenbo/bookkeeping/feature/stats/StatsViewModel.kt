package com.wenbo.bookkeeping.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenbo.bookkeeping.di.AppContainer
import com.wenbo.bookkeeping.domain.StatsAggregator
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 统计页模式：按月 / 按年。 */
enum class StatsMode { MONTH, YEAR }

data class StatsUiState(
    val mode: StatsMode = StatsMode.MONTH,
    val month: YearMonth = YearMonth.now(),
    val loading: Boolean = true,
    val summary: StatsAggregator.MonthSummary = StatsAggregator.MonthSummary(0, 0, 0),
    val expenseShares: List<StatsAggregator.CategoryShare> = emptyList(),
    val incomeShares: List<StatsAggregator.CategoryShare> = emptyList(),
    val trend: List<StatsAggregator.MonthPoint> = emptyList(),
)

/**
 * 统计页：聚合全部在内存做（[StatsAggregator] 纯函数），
 * 选中月/年与模式变化都会重新计算。
 */
class StatsViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state = _state.asStateFlow()

    private val month = MutableStateFlow(YearMonth.now())
    private val mode = MutableStateFlow(StatsMode.MONTH)

    init {
        viewModelScope.launch {
            combine(
                container.database.transactionDao().observeAll(),
                month,
                mode,
            ) { txs, m, md ->
                Triple(txs, m, md)
            }.collect { (txs, m, md) ->
                val filtered = if (md == StatsMode.MONTH) {
                    val from = m.atDay(1).toEpochDay()
                    val to = m.atEndOfMonth().toEpochDay()
                    txs.filter { it.date in from..to }
                } else {
                    // 年模式：取 m.year 整年
                    val from = java.time.LocalDate.of(m.year, 1, 1).toEpochDay()
                    val to = java.time.LocalDate.of(m.year, 12, 31).toEpochDay()
                    txs.filter { it.date in from..to }
                }
                _state.update {
                    it.copy(
                        mode = md,
                        month = m,
                        loading = false,
                        summary = StatsAggregator.summary(filtered),
                        expenseShares = StatsAggregator.categoryShares(
                            filtered,
                            com.wenbo.bookkeeping.domain.model.TransactionType.EXPENSE,
                        ),
                        incomeShares = StatsAggregator.categoryShares(
                            filtered,
                            com.wenbo.bookkeeping.domain.model.TransactionType.INCOME,
                        ),
                        trend = StatsAggregator.monthlyTrend(txs, YearMonth.now()),
                    )
                }
            }
        }
    }

    fun changeMonth(m: YearMonth) {
        month.value = m
    }

    fun changeMode(md: StatsMode) {
        mode.value = md
    }
}
