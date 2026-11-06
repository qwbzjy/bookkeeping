package com.wenbo.bookkeeping.data.backup

import com.wenbo.bookkeeping.core.time.MonthUtils
import com.wenbo.bookkeeping.domain.model.TransactionType
import com.wenbo.bookkeeping.domain.model.TxCategory

/**
 * CSV 编解码：UTF-8 + BOM（Excel 打开中文不乱码）+ RFC 4180 转义。
 * 纯函数不碰 Android API，可完整单测；解析对坏行容错（跳过并计数），
 * 绝不因单行脏数据让整个备份失败。
 */
object CsvCodec {

    const val BOM = "﻿"
    private const val HEADER = "kind,id,type,amountMinor,category,date,note,createdAt"

    /** 编码为 CSV 文本（含 BOM 与表头）。 */
    fun encode(rows: List<BackupRow>): String {
        val sb = StringBuilder()
        sb.append(BOM).append(HEADER).append("\r\n")
        for (row in rows) {
            val fields = when (row.kind) {
                "tx" -> listOf(
                    "tx",
                    row.id?.toString() ?: "",
                    row.type ?: "",
                    row.amountMinor?.toString() ?: "",
                    row.category ?: "",
                    row.date ?: "",
                    row.note ?: "",
                    row.createdAt?.toString() ?: "",
                )
                "budget" -> listOf(
                    "budget",
                    "",
                    "BUDGET",
                    row.amountMinor?.toString() ?: "",
                    row.category ?: "",
                    "",
                    "",
                    "",
                )
                else -> continue // 未知 kind 不导出
            }
            sb.append(fields.joinToString(",") { escape(it) }).append("\r\n")
        }
        return sb.toString()
    }

    /**
     * 解析 CSV 文本。返回成功行与跳过的坏行数；
     * 未知分类降级到对应类型的"其他"，未知 type 跳过该行。
     */
    fun decode(text: String): DecodeResult {
        val body = text.removePrefix(BOM)
        val records = parseRecords(body)
        val rows = mutableListOf<BackupRow>()
        var skipped = 0
        for (fields in records) {
            // 表头（数据行的 kind 列只会是 tx/budget）
            if (fields.firstOrNull()?.trim() == "kind") continue
            if (fields.size < 8) {
                skipped++
                continue
            }
            val parsed = when (fields[0]) {
                "tx" -> parseTx(fields)
                "budget" -> parseBudget(fields)
                else -> null
            }
            if (parsed == null) skipped++ else rows.add(parsed)
        }
        return DecodeResult(rows, skipped)
    }

    data class DecodeResult(val rows: List<BackupRow>, val skipped: Int)

    private fun parseTx(f: List<String>): BackupRow? {
        val type = when (f[2]) {
            "EXPENSE" -> TransactionType.EXPENSE
            "INCOME" -> TransactionType.INCOME
            else -> return null
        }
        val amount = f[3].toLongOrNull() ?: return null
        val date = MonthUtils.isoToDate(f[5]) ?: return null
        val category = TxCategory.byNameOrDefault(f[4], type)
        return BackupRow(
            kind = "tx",
            id = f[1].toLongOrNull(),
            type = type.name,
            amountMinor = amount,
            category = category.name,
            date = MonthUtils.dateToIso(date),
            note = f[6],
            createdAt = f[7].toLongOrNull() ?: date * 86_400_000L,
        )
    }

    private fun parseBudget(f: List<String>): BackupRow? {
        val amount = f[3].toLongOrNull() ?: return null
        val category = f[4].trim()
        if (category.isEmpty()) return null
        return BackupRow(kind = "budget", amountMinor = amount, category = category)
    }

    // ---- RFC 4180 转义与逐行解析 ----

    /** 含逗号/引号/换行/前后空格的字段整体双引号包裹，内部引号翻倍。 */
    private fun escape(field: String): String {
        return if (field.contains(',') || field.contains('"') || field.contains('\n') ||
            field.contains('\r')
        ) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }

    /**
     * RFC 4180 记录解析：逐字符状态机，引号内的逗号/换行/双引号
     * 都属于字段内容，不能按行简单 split（多行 note 会被拆断）。
     */
    private fun parseRecords(text: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        fun endRecord() {
            fields.add(sb.toString())
            sb.clear()
            // 纯空记录（行尾空行）丢弃
            if (fields.size > 1 || fields[0].isNotEmpty()) records.add(fields)
            fields = mutableListOf()
        }

        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                inQuotes -> when {
                    ch == '"' -> {
                        if (i + 1 < text.length && text[i + 1] == '"') {
                            sb.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    }
                    else -> sb.append(ch)
                }
                ch == '"' -> inQuotes = true
                ch == ',' -> {
                    fields.add(sb.toString())
                    sb.clear()
                }
                ch == '\r' -> {
                    if (i + 1 < text.length && text[i + 1] == '\n') i++
                    endRecord()
                }
                ch == '\n' -> endRecord()
                else -> sb.append(ch)
            }
            i++
        }
        if (sb.isNotEmpty() || fields.isNotEmpty()) endRecord()
        return records
    }
}
