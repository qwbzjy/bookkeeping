package com.wenbo.bookkeeping.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.wenbo.bookkeeping.core.amount.AmountFormat
import com.wenbo.bookkeeping.domain.StatsAggregator
import com.wenbo.bookkeeping.domain.model.TxCategory
import com.wenbo.bookkeeping.ui.theme.CategoryPalette
import com.wenbo.bookkeeping.ui.theme.SkipGray
import com.wenbo.bookkeeping.ui.theme.TextSecondary

/** 分类 → 扇区颜色：按 ordinal 从调色板取模，OTHER 固定灰，保证同分类永远同色。 */
fun categoryColor(category: TxCategory): Color = when (category) {
    TxCategory.OTHER_EXPENSE, TxCategory.OTHER_INCOME -> SkipGray
    else -> CategoryPalette[category.ordinal % CategoryPalette.size]
}

/**
 * 分类占比环形图：Canvas 逐扇区 drawArc，中心叠加当期总额。
 * 空数据画一整圈灰环 + "暂无支出"。扇区点击命中不做（要算极角，收益低）。
 */
@Composable
fun DonutChart(
    shares: List<StatsAggregator.CategoryShare>,
    totalMinor: Long,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(190.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 26.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
            if (shares.isEmpty()) {
                drawArc(
                    color = SkipGray,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
            } else {
                var start = -90f
                shares.forEach { share ->
                    drawArc(
                        color = categoryColor(share.category),
                        startAngle = start,
                        sweepAngle = share.fraction * 360f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                    start += share.fraction * 360f
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (shares.isEmpty()) "暂无支出" else "合计",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Text(
                text = AmountFormat.formatMinorToYuan(totalMinor),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}
