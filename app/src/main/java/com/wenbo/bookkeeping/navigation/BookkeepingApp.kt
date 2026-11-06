package com.wenbo.bookkeeping.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.wenbo.bookkeeping.feature.budget.BudgetScreen
import com.wenbo.bookkeeping.feature.edit.EditScreen
import com.wenbo.bookkeeping.feature.home.HomeScreen
import com.wenbo.bookkeeping.feature.settings.SettingsScreen
import com.wenbo.bookkeeping.feature.stats.StatsScreen

private data class TopLevelDest(val route: Any, val label: String, val icon: ImageVector)

private val topLevelDests = listOf(
    TopLevelDest(HomeRoute, "明细", Icons.Filled.ReceiptLong),
    TopLevelDest(StatsRoute, "统计", Icons.Filled.BarChart),
    TopLevelDest(BudgetRoute, "预算", Icons.Filled.Savings),
    TopLevelDest(SettingsRoute, "设置", Icons.Filled.Settings),
)

/**
 * 应用骨架：底部 4 Tab + FAB 记一笔；编辑页隐藏底栏与 FAB。
 */
@Composable
fun BookkeepingAppScaffold() {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val isTopLevel = destination != null && topLevelDests.any { destination.hasRoute(it.route::class) }

    Scaffold(
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    topLevelDests.forEach { dest ->
                        val selected = destination.hasRoute(dest.route::class)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (isTopLevel) {
                FloatingActionButton(
                    onClick = { navController.navigate(EditRoute()) },
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "记一笔")
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeRoute> {
                HomeScreen(onEdit = { txId -> navController.navigate(EditRoute(txId)) })
            }
            composable<StatsRoute> { StatsScreen() }
            composable<BudgetRoute> { BudgetScreen() }
            composable<SettingsRoute> { SettingsScreen() }
            composable<EditRoute> { entry ->
                val route = entry.toRoute<EditRoute>()
                EditScreen(txId = route.txId, onClosed = { navController.popBackStack() })
            }
        }
    }
}
