package com.wenbo.bookkeeping.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 月度预算。不绑定具体年月：预算是"每个月都生效的常设设定"，
 * 进度永远按当前选中月的支出对比这份上限。
 *
 * 主键为分类名；总预算用固定键 [ALL_KEY] 承载，只是预算列表的第一行。
 * 收入分类不设预算，UI 层只展示支出分类。
 */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: String, // TxCategory.name 或 ALL_KEY
    val amountMinor: Long, // 每月上限（分），0 视为未设
) {
    companion object {
        const val ALL_KEY = "__all__"
    }
}
