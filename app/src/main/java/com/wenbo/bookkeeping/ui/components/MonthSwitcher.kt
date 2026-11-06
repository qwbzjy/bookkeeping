package com.wenbo.bookkeeping.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wenbo.bookkeeping.core.time.MonthUtils
import java.time.YearMonth

/**
 * 月份切换条：‹ 2026年11月 ›。右箭头在到达当前月时禁用（惯例：不禁止看未来月，
 * 只是阻止一路翻到还没发生的月份）。
 */
@Composable
fun MonthSwitcher(
    month: YearMonth,
    onChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = YearMonth.now()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onChange(month.minusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一月")
        }
        Text(
            text = MonthUtils.monthLabel(month),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        IconButton(
            onClick = { onChange(month.plusMonths(1)) },
            enabled = month < current,
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一月")
        }
    }
}
