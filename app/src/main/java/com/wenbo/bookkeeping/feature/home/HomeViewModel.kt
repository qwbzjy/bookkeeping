package com.wenbo.bookkeeping.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenbo.bookkeeping.core.time.MonthUtils
import com.wenbo.bookkeeping.data.db.TransactionEntity
import com.wenbo.bookkeeping.di.AppContainer
import com.wenbo.bookkeeping.domain.StatsAggregator
import java.time.YearMonth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 首页明细状态：选中月 + 按天分组的时间线。 */
data class HomeUiState(
    val month: YearMonth = YearMonth.now(),
    val loading: Boolean = true,
    val days: Map<Long, List<TransactionEntity>> = emptyMap(), // epochDay -> 当日交易
    val summary: StatsAggregator.MonthSummary = StatsAggregator.MonthSummary(0, 0, 0),
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val month = MutableStateFlow(YearMonth.now())

    init {
        viewModelScope.launch {
            combine(
                container.database.transactionDao().observeAll(),
                month,
            ) { txs, m ->
                // 数据量小，全量拉取后在内存按选中月过滤，聚合逻辑可直测
                val inMonth = txs.filter {
                    it.date >= MonthUtils.firstDay(m) && it.date <= MonthUtils.lastDay(m)
                }
                m to inMonth
            }.collect { (m, inMonth) ->
                _state.update {
                    it.copy(
                        month = m,
                        loading = false,
                        days = StatsAggregator.groupByDay(inMonth),
                        summary = StatsAggregator.summary(inMonth),
                    )
                }
            }
        }
    }

    fun changeMonth(m: YearMonth) {
        month.value = m
    }

    fun delete(tx: TransactionEntity) {
        viewModelScope.launch {
            container.database.transactionDao().delete(tx)
            _events.send("已删除")
        }
    }
}
