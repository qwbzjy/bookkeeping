package com.wenbo.bookkeeping.feature.home

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wenbo.bookkeeping.core.amount.AmountFormat
import com.wenbo.bookkeeping.core.time.MonthUtils
import com.wenbo.bookkeeping.data.db.TransactionEntity
import com.wenbo.bookkeeping.di.LocalAppContainer
import com.wenbo.bookkeeping.domain.model.TransactionType
import com.wenbo.bookkeeping.domain.model.TxCategory
import com.wenbo.bookkeeping.ui.components.CategoryBadge
import com.wenbo.bookkeeping.ui.components.ConfirmDialog
import com.wenbo.bookkeeping.ui.components.EmptyState
import com.wenbo.bookkeeping.ui.components.MonthSwitcher
import com.wenbo.bookkeeping.ui.theme.ExpenseRed
import com.wenbo.bookkeeping.ui.theme.IncomeGreen
import com.wenbo.bookkeeping.ui.theme.TextSecondary

/**
 * 首页明细：月切换 + 收支结余汇总卡 + 按天分组时间线。
 * 点行进编辑页；长按行删除。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onEdit: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel { HomeViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("记账本") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Column {
                    MonthSwitcher(
                        month = state.month,
                        onChange = viewModel::changeMonth,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    SummaryCard(
                        summary = state.summary,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.padding(vertical = 4.dp))
                }
            }
            if (state.days.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        emoji = "🧾",
                        title = "本月还没有记录",
                        subtitle = "点击右下角 + 记一笔",
                    )
                }
            }
            state.days.forEach { (day, txs) ->
                stickyHeader(key = "header_$day") {
                    DayHeader(
                        day = day,
                        dayExpense = txs.filter { it.type == TransactionType.EXPENSE }
                            .sumOf { it.amountMinor },
                        dayIncome = txs.filter { it.type == TransactionType.INCOME }
                            .sumOf { it.amountMinor },
                    )
                }
                items(txs, key = { "tx_${it.id}" }) { tx ->
                    TransactionRow(
                        tx = tx,
                        onClick = { onEdit(tx.id) },
                        onLongClick = { pendingDelete = tx },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }

    pendingDelete?.let { tx ->
        ConfirmDialog(
            title = "删除这条记录？",
            text = "${TxCategory.byNameOrDefault(tx.category, tx.type).label} " +
                AmountFormat.formatMinorToYuan(tx.amountMinor) + " 元",
            onConfirm = {
                viewModel.delete(tx)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun SummaryCard(
    summary: com.wenbo.bookkeeping.domain.StatsAggregator.MonthSummary,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SummaryItem("支出", AmountFormat.formatMinorToYuan(summary.expenseMinor), ExpenseRed)
            SummaryItem("收入", AmountFormat.formatMinorToYuan(summary.incomeMinor), IncomeGreen)
            SummaryItem(
                "结余",
                AmountFormat.formatMinorToYuan(summary.balanceMinor),
                MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.padding(vertical = 2.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun DayHeader(
    day: Long,
    dayExpense: Long,
    dayIncome: Long,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = MonthUtils.dayLabel(day),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
        if (dayExpense > 0) {
            Text(
                text = "支 ${AmountFormat.formatMinorToYuan(dayExpense)}",
                style = MaterialTheme.typography.labelSmall,
                color = ExpenseRed,
            )
            Spacer(Modifier.width(8.dp))
        }
        if (dayIncome > 0) {
            Text(
                text = "收 ${AmountFormat.formatMinorToYuan(dayIncome)}",
                style = MaterialTheme.typography.labelSmall,
                color = IncomeGreen,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionRow(
    tx: TransactionEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val category = TxCategory.byNameOrDefault(tx.category, tx.type)
    val isExpense = tx.type == TransactionType.EXPENSE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(category)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.label,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (tx.note.isNotBlank()) {
                Text(
                    text = tx.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = (if (isExpense) "-" else "+") + AmountFormat.formatMinorToYuan(tx.amountMinor),
            style = MaterialTheme.typography.titleMedium,
            color = if (isExpense) ExpenseRed else IncomeGreen,
        )
    }
}
