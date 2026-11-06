package com.wenbo.bookkeeping.navigation

import kotlinx.serialization.Serializable

/** Type-safe 导航路由。Edit 的 txId == -1L 表示新建。 */
@Serializable
data object HomeRoute

@Serializable
data object StatsRoute

@Serializable
data object BudgetRoute

@Serializable
data object SettingsRoute

@Serializable
data class EditRoute(val txId: Long = -1L)
