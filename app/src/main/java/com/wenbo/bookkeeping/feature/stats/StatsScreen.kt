package com.wenbo.bookkeeping.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wenbo.bookkeeping.core.amount.AmountFormat
import com.wenbo.bookkeeping.di.LocalAppContainer
import com.wenbo.bookkeeping.domain.model.TransactionType
import com.wenbo.bookkeeping.ui.components.DonutChart
import com.wenbo.bookkeeping.ui.components.EmptyState
import com.wenbo.bookkeeping.ui.components.MonthSwitcher
import com.wenbo.bookkeeping.ui.components.MonthlyBarChart
import com.wenbo.bookkeeping.ui.components.categoryColor
import com.wenbo.bookkeeping.ui.theme.ExpenseRed
import com.wenbo.bookkeeping.ui.theme.IncomeGreen
import com.wenbo.bookkeeping.ui.theme.TextSecondary

/**
 * 统计页：月/年切换 + 收支结余三卡 + 分类占比环形图（图例列表）+ 近 6 月趋势。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen() {
    val container = LocalAppContainer.current
    val viewModel: StatsViewModel = viewModel { StatsViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 占比图当前展示的收入/支出维度
    var shareType by remember { mutableStateOf(TransactionType.EXPENSE) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("统计") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        MonthSwitcher(
                            month = state.month,
                            onChange = viewModel::changeMonth,
                        )
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = state.mode == StatsMode.MONTH,
                                onClick = { viewModel.changeMode(StatsMode.MONTH) },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                            ) { Text("月") }
                            SegmentedButton(
                                selected = state.mode == StatsMode.YEAR,
                                onClick = { viewModel.changeMode(StatsMode.YEAR) },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                            ) { Text("年") }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // 三卡
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            StatItem(
                                "支出",
                                AmountFormat.formatMinorToYuan(state.summary.expenseMinor),
                                ExpenseRed,
                            )
                            StatItem(
                                "收入",
                                AmountFormat.formatMinorToYuan(state.summary.incomeMinor),
                                IncomeGreen,
                            )
                            StatItem(
                                "结余",
                                AmountFormat.formatMinorToYuan(state.summary.balanceMinor),
                                MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    // 占比维度切换
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = shareType == TransactionType.EXPENSE,
                            onClick = { shareType = TransactionType.EXPENSE },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                        ) { Text("支出占比") }
                        SegmentedButton(
                            selected = shareType == TransactionType.INCOME,
                            onClick = { shareType = TransactionType.INCOME },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                        ) { Text("收入占比") }
                    }
                }
            }
            val shares = if (shareType == TransactionType.EXPENSE) state.expenseShares else state.incomeShares
            val total = if (shareType == TransactionType.EXPENSE) {
                state.summary.expenseMinor
            } else {
                state.summary.incomeMinor
            }
            if (shares.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "📊",
                        title = "暂无${if (shareType == TransactionType.EXPENSE) "支出" else "收入"}数据",
                        subtitle = "记几笔后再来看统计",
                    )
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        DonutChart(shares = shares, totalMinor = total)
                        Spacer(Modifier.height(16.dp))
                        shares.forEach { share ->
                            LegendRow(share, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
                        }
                    }
                }
            }
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "近 6 个月趋势",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    MonthlyBarChart(points = state.trend, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun LegendRow(
    share: com.wenbo.bookkeeping.domain.StatsAggregator.CategoryShare,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            Modifier
                .size(8.dp)
                .background(categoryColor(share.category), CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${share.category.emoji} ${share.category.label}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = AmountFormat.formatMinorToYuan(share.amountMinor),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "${(share.fraction * 100).let { if (it >= 99.5f) 100 else it.toInt() }}%",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
    }
}
