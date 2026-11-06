package com.wenbo.bookkeeping.feature.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wenbo.bookkeeping.core.time.MonthUtils
import com.wenbo.bookkeeping.di.LocalAppContainer
import com.wenbo.bookkeeping.domain.model.TransactionType
import com.wenbo.bookkeeping.domain.model.TxCategory
import java.time.LocalDate

/**
 * 记一笔表单页：收支切换 → 分类单选 → 金额/日期/备注 → 保存。
 * 金额用系统 Decimal 键盘 + 纯函数输入过滤，不自绘键盘。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    txId: Long,
    onClosed: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: EditViewModel = viewModel(key = "edit_$txId") {
        EditViewModel(container, txId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onClosed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "编辑记录" else "记一笔") },
                navigationIcon = {
                    IconButton(onClick = onClosed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 收/支切换
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { viewModel.selectType(TransactionType.EXPENSE) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("支出") }
                SegmentedButton(
                    selected = state.type == TransactionType.INCOME,
                    onClick = { viewModel.selectType(TransactionType.INCOME) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("收入") }
            }

            // 分类网格（随收支类型切换）
            val categories = if (state.type == TransactionType.EXPENSE) {
                TxCategory.expenseAll()
            } else {
                TxCategory.incomeAll()
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 4,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    CategoryCell(
                        category = category,
                        selected = state.category == category,
                        onClick = { viewModel.selectCategory(category) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // 金额
            OutlinedTextField(
                value = state.amountText,
                onValueChange = viewModel::updateAmount,
                label = { Text("金额（元）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 日期
            var showDatePicker by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "日期：${formatDate(state.dateEpochDay)}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showDatePicker = true }) { Text("修改") }
            }
            if (showDatePicker) {
                val pickerState = rememberDatePickerState(
                    initialSelectedDateMillis = state.dateEpochDay * 86_400_000L,
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                pickerState.selectedDateMillis?.let { millis ->
                                    viewModel.updateDate(millis / 86_400_000L)
                                }
                                showDatePicker = false
                            },
                        ) { Text("确定") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("取消") }
                    },
                ) {
                    DatePicker(state = pickerState)
                }
            }

            // 备注
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::updateNote,
                label = { Text("备注（选填）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isEdit) "保存修改" else "保存")
            }
        }
    }
}

@Composable
private fun CategoryCell(
    category: TxCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .width(48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = category.emoji,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        CircleShape,
                    )
                    .padding(8.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).toString()
