package com.wenbo.bookkeeping.domain

/**
 * 柱状图 y 轴刻度归整：把原始最大值向上取整到 1/2/2.5/5 × 10^n，
 * 让网格线落在"好看"的数字上（8700 -> 10000、123 -> 200）。
 */
object ChartScale {

    /** 归整后的刻度上限。输入 <= 0 时返回 100（避免除零与空轴）。 */
    fun maxOf(rawMax: Long): Long {
        if (rawMax <= 0) return 100L
        var magnitude = 1L
        while (magnitude * 10 <= rawMax) magnitude *= 10
        val candidates = longArrayOf(1, 2, 5, 10).map { it * magnitude }
        return candidates.first { it >= rawMax }
    }
}
