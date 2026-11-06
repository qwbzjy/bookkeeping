package com.wenbo.bookkeeping.feature.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.wenbo.bookkeeping.di.LocalAppContainer
import com.wenbo.bookkeeping.ui.components.EmptyState
import com.wenbo.bookkeeping.ui.theme.ExpenseRed
import com.wenbo.bookkeeping.ui.theme.TextSecondary

/**
 * 预算页：总预算行 + 各支出分类行（emoji + 已用/上限 + 进度条，超支满格变红）。
 * 点行弹输入框设置上限，清空即清除。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen() {
    val container = LocalAppContainer.current
    val viewModel: BudgetViewModel = viewModel { BudgetViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 正在编辑上限的行 key（含"未设预算"状态也允许点入设置）
    var editingKey by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("预算") }) },
    ) { padding ->
        val allRows: List<BudgetRowUi> = buildList {
            state.total?.let { add(it) }
            addAll(state.rows)
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (allRows.none { it.limitMinor > 0 }) {
                item {
                    EmptyState(
                        emoji = "🎯",
                        title = "还没有设置预算",
                        subtitle = "点击任意分类设置每月上限",
                    )
                }
            }
            items(allRows, key = { it.categoryKey }) { row ->
                BudgetRow(row, onClick = { editingKey = row.categoryKey })
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "预算为每月常设上限，不区分月份；进度基于本月支出。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }

    editingKey?.let { key ->
        val row = listOfNotNull(state.total).plus(state.rows).firstOrNull { it.categoryKey == key }
        BudgetLimitDialog(
            title = if (key == "__all__") "设置总预算" else "设置「${row?.label ?: ""}」预算",
            currentYuan = row?.takeIf { it.limitMinor > 0 }?.limitText ?: "",
            onConfirm = { text ->
                viewModel.setLimit(key, text)
                editingKey = null
            },
            onDismiss = { editingKey = null },
        )
    }
}

@Composable
private fun BudgetRow(
    row: BudgetRowUi,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = row.emoji, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = row.label, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { row.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (row.isOver) ExpenseRed else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (row.limitMinor > 0) "${row.usedText} / ${row.limitText}" else "${row.usedText} / —",
            style = MaterialTheme.typography.labelMedium,
            color = if (row.isOver) ExpenseRed else TextSecondary,
        )
    }
}

@Composable
private fun BudgetLimitDialog(
    title: String,
    currentYuan: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(currentYuan) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = com.wenbo.bookkeeping.core.amount.AmountFormat.sanitizeInput(it) },
                    label = { Text("每月上限（元）") },
                    singleLine = true,
                )
                Text(
                    text = "清空并保存表示不设预算",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
