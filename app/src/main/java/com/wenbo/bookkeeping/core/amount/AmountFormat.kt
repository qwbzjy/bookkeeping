package com.wenbo.bookkeeping.core.amount

/**
 * 金额格式化与解析：数据库恒以"分"（Long）存储，杜绝 Double 精度问题；
 * UI 层输入输出都是"元"字符串。
 */
object AmountFormat {

    /** 分 → 元字符串，保留两位小数。如 2580 -> "25.80"。 */
    fun formatMinorToYuan(minor: Long): String {
        val negative = minor < 0
        val abs = if (negative) -minor else minor
        val yuan = abs / 100
        val fen = abs % 100
        val sign = if (negative) "-" else ""
        return "$sign$yuan.${fen.toString().padStart(2, '0')}"
    }

    /**
     * 元字符串 → 分。非法输入返回 null。
     * 接受 "12.5"、"12"、"12.50"；超过两位小数直接拒绝（调用方输入过滤已保证不会出现）。
     */
    fun parseYuanToMinor(input: String): Long? {
        val text = input.trim()
        if (text.isEmpty()) return null
        // 只允许数字 + 至多一个小数点
        if (!text.matches(Regex("\\d+(\\.\\d{0,2})?"))) return null
        val dot = text.indexOf('.')
        return if (dot < 0) {
            text.toLong() * 100
        } else {
            val yuan = text.substring(0, dot).ifEmpty { "0" }.toLong()
            val fenText = text.substring(dot + 1)
            val fen = when (fenText.length) {
                0 -> 0L
                1 -> fenText.toLong() * 10
                else -> fenText.toLong()
            }
            yuan * 100 + fen
        }
    }

    /**
     * 表单输入过滤：只留数字与第一个小数点，小数位最多两位。
     * 用户粘贴非法字符时静默剔除，保证输入恒为合法金额形态。
     */
    fun sanitizeInput(raw: String): String {
        var dotSeen = false
        val sb = StringBuilder()
        for (ch in raw) {
            when {
                ch.isDigit() -> {
                    // 小数点后最多两位
                    val dotIndex = sb.indexOf('.')
                    if (dotIndex >= 0 && sb.length - dotIndex > 2) continue
                    sb.append(ch)
                }
                ch == '.' && !dotSeen -> {
                    dotSeen = true
                    sb.append(ch)
                }
            }
        }
        return sb.toString()
    }

    /**
     * 图表/汇总用的缩写金额：整万以上显示 x.x万，否则显示两位小数。
     * 如 999_999 -> "9999.99"，1_000_000 -> "10.0万"（单位：分）。
     */
    fun formatCompact(minor: Long): String {
        return if (minor >= 1_000_000) {
            val wan = minor / 1_000_000.0
            val text = if (wan >= 100) wan.toLong().toString() else String.format("%.1f", wan)
            "${text}万"
        } else {
            formatMinorToYuan(minor)
        }
    }
}
