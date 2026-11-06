package com.wenbo.bookkeeping.data.backup

/**
 * CSV 备份的行模型：交易与预算共用一个 schema，[kind] 列区分。
 * 字段全部按 CSV 文本形态（可空），编解码在 [CsvCodec] 中完成。
 */
data class BackupRow(
    val kind: String, // "tx" 或 "budget"
    val id: Long? = null, // tx 行：保留原 id，导入时可整体恢复
    val type: String? = null, // tx 行：EXPENSE / INCOME
    val amountMinor: Long? = null,
    val category: String? = null, // tx 行：TxCategory.name；budget 行：name 或 "__all__"
    val date: String? = null, // tx 行：ISO yyyy-MM-dd
    val note: String? = null,
    val createdAt: Long? = null, // tx 行
)
