package com.wenbo.bookkeeping.domain.model

/**
 * 领域模型：交易类型与预置分类。
 *
 * 分类用硬编码枚举而非 Room 表：需求只要求预置分类，枚举省掉
 * 分类管理 CRUD 页与外键维护；枚举 name 即文本，将来若开放自定义
 * 分类，迁移路径清晰（budgets 表就是用 name 做主键）。
 */
enum class TransactionType { EXPENSE, INCOME }

/**
 * 记账分类。label/emoji 直接承载中文文案，UI 层零映射成本。
 */
enum class TxCategory(
    val type: TransactionType,
    val label: String,
    val emoji: String,
) {
    // 支出
    FOOD(TransactionType.EXPENSE, "餐饮", "🍜"),
    TRANSPORT(TransactionType.EXPENSE, "交通", "🚌"),
    SHOPPING(TransactionType.EXPENSE, "购物", "🛍️"),
    DAILY(TransactionType.EXPENSE, "日用", "🧴"),
    HOUSING(TransactionType.EXPENSE, "居住", "🏠"),
    FUN(TransactionType.EXPENSE, "娱乐", "🎮"),
    MEDICAL(TransactionType.EXPENSE, "医疗", "💊"),
    PHONE(TransactionType.EXPENSE, "通讯", "📱"),
    STUDY(TransactionType.EXPENSE, "学习", "📚"),
    SOCIAL(TransactionType.EXPENSE, "人情", "🎁"),
    TRAVEL(TransactionType.EXPENSE, "旅行", "✈️"),
    OTHER_EXPENSE(TransactionType.EXPENSE, "其他", "📦"),

    // 收入
    SALARY(TransactionType.INCOME, "工资", "💰"),
    BONUS(TransactionType.INCOME, "奖金", "🏆"),
    PART_TIME(TransactionType.INCOME, "兼职", "💼"),
    INVEST(TransactionType.INCOME, "理财", "📈"),
    RED_PACKET(TransactionType.INCOME, "红包", "🧧"),
    OTHER_INCOME(TransactionType.INCOME, "其他", "💵");

    companion object {
        /** CSV 导入容错入口：未知分类名降级到对应类型的"其他"。 */
        fun byNameOrDefault(name: String?, type: TransactionType): TxCategory {
            val hit = name?.let { n -> entries.firstOrNull { it.name == n } }
            if (hit != null) return hit
            return if (type == TransactionType.EXPENSE) OTHER_EXPENSE else OTHER_INCOME
        }

        fun expenseAll(): List<TxCategory> = entries.filter { it.type == TransactionType.EXPENSE }

        fun incomeAll(): List<TxCategory> = entries.filter { it.type == TransactionType.INCOME }
    }
}
