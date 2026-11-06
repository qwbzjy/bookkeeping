package com.wenbo.bookkeeping.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wenbo.bookkeeping.domain.model.TransactionType

/**
 * 一笔交易。金额以"分"存储恒为正数，收支方向由 [type] 表达。
 * 日期存 epochDay（当天 0 点的纪元日数），是月查询与按天分组的主键。
 */
@Entity(
    tableName = "transactions",
    indices = [Index("date")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: TransactionType,
    val amountMinor: Long,
    val category: String, // TxCategory.name；存 String 让 Room 零转换器，UI 层再映射枚举
    val date: Long, // LocalDate.toEpochDay()
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(), // 同日内排序的 tie-breaker
)
