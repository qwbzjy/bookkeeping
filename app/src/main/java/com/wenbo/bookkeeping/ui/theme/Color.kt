package com.wenbo.bookkeeping.ui.theme

import androidx.compose.ui.graphics.Color

// 主色：蓝绿系，记账工具的中性冷静色
val TealPrimary = Color(0xFF00696E)
val TealContainer = Color(0xFFB4EBEE)
val OnTealContainer = Color(0xFF002022)

// 语义色：收入绿 / 支出红
val IncomeGreen = Color(0xFF2E7D32)
val ExpenseRed = Color(0xFFC62828)
val MissRed = Color(0xFFBA1A1A)
val MissRedContainer = Color(0xFFFFDAD6)

// 中性辅助
val SkipGray = Color(0xFFB9BDBE)
val LineGray = Color(0xFFDDE1E0)
val TextSecondary = Color(0xFF51605F)

// 分类调色板：按 TxCategory.ordinal 取模映射，保证同分类永远同色
val CategoryPalette = listOf(
    Color(0xFFE57373), // 红陶
    Color(0xFFBA68C8), // 紫
    Color(0xFF64B5F6), // 蓝
    Color(0xFF4DB6AC), // 青
    Color(0xFFAED581), // 草绿
    Color(0xFFFFD54F), // 杏黄
    Color(0xFFFF8A65), // 橘
    Color(0xFF90A4AE), // 蓝灰
    Color(0xFF7986CB), // 靛蓝
    Color(0xFFF06292), // 粉
    Color(0xFFFFB74D), // 琥珀
    Color(0xFF81C784), // 绿
)
