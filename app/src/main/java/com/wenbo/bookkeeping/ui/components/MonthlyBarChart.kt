package com.wenbo.bookkeeping.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.wenbo.bookkeeping.core.time.MonthUtils
import com.wenbo.bookkeeping.domain.ChartScale
import com.wenbo.bookkeeping.domain.StatsAggregator
import com.wenbo.bookkeeping.ui.theme.ExpenseRed
import com.wenbo.bookkeeping.ui.theme.IncomeGreen
import com.wenbo.bookkeeping.ui.theme.LineGray
import com.wenbo.bookkeeping.ui.theme.TextSecondary

/**
 * 近 6 个月收支趋势柱状图：每组两根柱（支出红/收入绿），
 * y 轴上限经 [ChartScale] 归整到好看的数字；月份标签画在底部。
 */
@Composable
fun MonthlyBarChart(
    points: List<StatsAggregator.MonthPoint>,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelTextStyle = MaterialTheme.typography.labelSmall
    val labelColor = TextSecondary
    val gridColor = LineGray
    val expenseColor = ExpenseRed
    val incomeColor = IncomeGreen

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            if (points.isEmpty()) return@Canvas
            val rawMax = points.maxOf { maxOf(it.expenseMinor, it.incomeMinor) }
            val scaleMax = ChartScale.maxOf(rawMax)
            val labelHeight = 20.dp.toPx()
            val plotHeight = size.height - labelHeight
            val groupWidth = size.width / points.size
            val barWidth = groupWidth * 0.24f
            val corner = CornerRadius(4.dp.toPx())

            // 网格线：上沿 / 中线 / 底线
            listOf(0f, 0.5f, 1f).forEach { fraction ->
                val y = plotHeight * fraction
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }

            points.forEachIndexed { index, point ->
                val center = groupWidth * index + groupWidth / 2
                val expenseH = plotHeight * (point.expenseMinor.toFloat() / scaleMax)
                val incomeH = plotHeight * (point.incomeMinor.toFloat() / scaleMax)
                // 支出柱在左、收入柱在右
                drawRoundRect(
                    color = expenseColor,
                    topLeft = Offset(center - barWidth - 2.dp.toPx(), plotHeight - expenseH),
                    size = Size(barWidth, expenseH),
                    cornerRadius = corner,
                )
                drawRoundRect(
                    color = incomeColor,
                    topLeft = Offset(center + 2.dp.toPx(), plotHeight - incomeH),
                    size = Size(barWidth, incomeH),
                    cornerRadius = corner,
                )
                // 月份标签
                val label = MonthUtils.shortMonthLabel(point.month)
                val measured = textMeasurer.measure(
                    label,
                    labelTextStyle.copy(color = labelColor),
                )
                drawText(
                    measured,
                    topLeft = Offset(center - measured.size.width / 2f, plotHeight + 2.dp.toPx()),
                )
            }
        }
        // 图例
        Row(modifier = Modifier.padding(top = 8.dp)) {
            ChartLegendDot(expenseColor, "支出")
            Spacer(Modifier.width(12.dp))
            ChartLegendDot(incomeColor, "收入")
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
