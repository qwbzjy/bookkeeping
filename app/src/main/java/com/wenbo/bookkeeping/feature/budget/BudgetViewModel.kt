package com.wenbo.bookkeeping.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenbo.bookkeeping.core.amount.AmountFormat
import com.wenbo.bookkeeping.data.db.BudgetEntity
import com.wenbo.bookkeeping.data.db.TransactionEntity
import com.wenbo.bookkeeping.di.AppContainer
import com.wenbo.bookkeeping.domain.StatsAggregator
import com.wenbo.bookkeeping.domain.model.TransactionType
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 一行预算的展示数据：分类（或总预算）+ 上限 + 当月已用。 */
data class BudgetRowUi(
    val categoryKey: String, // TxCategory.name 或 "__all__"
    val label: String,
    val emoji: String,
    val limitMinor: Long,
    val usedMinor: Long, // 当月该分类（或全部）支出
) {
    val isOver: Boolean get() = limitMinor > 0 && usedMinor > limitMinor
    val fraction: Float
        get() = if (limitMinor <= 0L) 0f else (usedMinor.toFloat() / limitMinor).coerceAtMost(1f)
    val usedText: String get() = AmountFormat.formatMinorToYuan(usedMinor)
    val limitText: String get() = AmountFormat.formatMinorToYuan(limitMinor)
}

data class BudgetUiState(
    val loading: Boolean = true,
    val total: BudgetRowUi? = null, // 总预算行，未设时也展示（limit 0）
    val rows: List<BudgetRowUi> = emptyList(),
)

/** 预算页：常设月度预算 + 当月支出进度。 */
class BudgetViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(BudgetUiState())
    val state = _state.asStateFlow()

    private val currentMonth = YearMonth.now()

    init {
        viewModelScope.launch {
            combine(
                container.database.budgetDao().observeAll(),
                container.database.transactionDao().observeAll(),
            ) { budgets, txs -> budgets to txs }.collect { (budgets, txs) ->
                val monthTxs = txs.filter {
                    it.type == TransactionType.EXPENSE &&
                        it.date in currentMonth.atDay(1).toEpochDay()..currentMonth.atEndOfMonth().toEpochDay()
                }
                _state.update {
                    it.copy(
                        loading = false,
                        total = buildRow(
                            key = BudgetEntity.ALL_KEY,
                            label = "总预算",
                            emoji = "🧮",
                            budgets = budgets,
                            monthTxs = monthTxs,
                        ),
                        rows = com.wenbo.bookkeeping.domain.model.TxCategory.expenseAll().map { c ->
                            buildRow(c.name, c.label, c.emoji, budgets, monthTxs)
                        },
                    )
                }
            }
        }
    }

    private fun buildRow(
        key: String,
        label: String,
        emoji: String,
        budgets: List<BudgetEntity>,
        monthTxs: List<TransactionEntity>,
    ): BudgetRowUi {
        val limit = budgets.firstOrNull { it.category == key }?.amountMinor ?: 0L
        val used = if (key == BudgetEntity.ALL_KEY) {
            StatsAggregator.summary(monthTxs).expenseMinor
        } else {
            monthTxs.filter { it.category == key }.sumOf { it.amountMinor }
        }
        return BudgetRowUi(key, label, emoji, limit, used)
    }

    /** 设置上限（元字符串）；空串或解析为 0 表示清除。 */
    fun setLimit(categoryKey: String, yuanText: String) {
        viewModelScope.launch {
            val minor = AmountFormat.parseYuanToMinor(yuanText) ?: 0L
            val dao = container.database.budgetDao()
            if (minor <= 0L) {
                dao.delete(categoryKey)
            } else {
                dao.upsert(BudgetEntity(categoryKey, minor))
            }
        }
    }
}
