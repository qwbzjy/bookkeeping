package com.wenbo.bookkeeping.data.backup

import androidx.room.withTransaction
import com.wenbo.bookkeeping.core.time.MonthUtils
import com.wenbo.bookkeeping.data.db.BudgetEntity
import com.wenbo.bookkeeping.data.db.BookkeepingDatabase
import com.wenbo.bookkeeping.data.db.TransactionEntity
import com.wenbo.bookkeeping.domain.model.TransactionType

/**
 * 备份管理：导出全量编码为 CSV；导入为整体替换
 * （deleteAll + insertAll 于同一事务，语义最简单、最不易脏数据）。
 */
class BackupManager(private val db: BookkeepingDatabase) {

    /** 导出全部数据为 CSV 文本（含 BOM）。 */
    suspend fun export(): String {
        val rows = buildList {
            db.transactionDao().getAll().forEach { tx ->
                add(
                    BackupRow(
                        kind = "tx",
                        id = tx.id,
                        type = tx.type.name,
                        amountMinor = tx.amountMinor,
                        category = tx.category,
                        date = MonthUtils.dateToIso(tx.date),
                        note = tx.note,
                        createdAt = tx.createdAt,
                    )
                )
            }
            db.budgetDao().getAll().forEach { b ->
                add(BackupRow(kind = "budget", amountMinor = b.amountMinor, category = b.category))
            }
        }
        return CsvCodec.encode(rows)
    }

    /**
     * 整体替换导入。返回摘要文案，如 "导入 12 条交易、2 条预算，跳过 1 行"。
     * 无法解析的行跳过并计数，绝不因单行脏数据让整个导入失败。
     */
    suspend fun importReplace(rows: List<BackupRow>): String {
        val txs = rows.filter { it.kind == "tx" }.mapNotNull { row ->
            val type = row.type?.let { t ->
                runCatching { TransactionType.valueOf(t) }.getOrNull()
            } ?: return@mapNotNull null
            val amount = row.amountMinor ?: return@mapNotNull null
            val category = row.category ?: return@mapNotNull null
            val date = MonthUtils.isoToDate(row.date ?: "") ?: return@mapNotNull null
            TransactionEntity(
                id = row.id ?: 0L,
                type = type,
                amountMinor = amount,
                category = category,
                date = date,
                note = row.note ?: "",
                createdAt = row.createdAt ?: System.currentTimeMillis(),
            )
        }
        val budgets = rows.filter { it.kind == "budget" }.mapNotNull { row ->
            row.amountMinor?.let { amount ->
                row.category?.let { category -> BudgetEntity(category, amount) }
            }
        }
        db.withTransaction {
            db.transactionDao().deleteAll()
            db.budgetDao().deleteAll()
            db.transactionDao().insertAll(txs)
            budgets.forEach { db.budgetDao().upsert(it) }
        }
        val skipped = rows.size - txs.size - budgets.size
        return "导入 ${txs.size} 条交易、${budgets.size} 条预算" +
            if (skipped > 0) "，跳过 $skipped 行" else ""
    }
}
