package com.wenbo.bookkeeping.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wenbo.bookkeeping.domain.model.TxCategory

/**
 * 分类 emoji 圆片徽章：时间线行、分类选择器共用。
 */
@Composable
fun CategoryBadge(
    category: TxCategory,
    modifier: Modifier = Modifier,
    size: Int = 40,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = category.emoji, style = MaterialTheme.typography.titleMedium)
    }
}
