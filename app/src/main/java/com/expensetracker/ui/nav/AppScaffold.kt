package com.expensetracker.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expensetracker.data.db.TransactionType
import com.expensetracker.data.repo.TransactionForm
import com.expensetracker.ui.AppViewModel
import com.expensetracker.ui.common.todayEpoch
import com.expensetracker.ui.screens.AccountDetailScreen
import com.expensetracker.ui.screens.AccountsScreen
import com.expensetracker.ui.screens.CategoriesScreen
import com.expensetracker.ui.screens.DashboardScreen
import com.expensetracker.ui.screens.LearningScreen
import com.expensetracker.ui.screens.MoreScreen
import com.expensetracker.ui.screens.RecurringEditScreen
import com.expensetracker.ui.screens.RecurringListScreen
import com.expensetracker.ui.screens.ReportsScreen
import com.expensetracker.ui.screens.SettingsScreen
import com.expensetracker.ui.screens.TagsScreen
import com.expensetracker.ui.screens.TransactionDetailScreen
import com.expensetracker.ui.screens.TransactionEditScreen
import com.expensetracker.ui.screens.TransactionsScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val ACCOUNTS = "accounts"
    const val TAGS = "tags"
    const val MORE = "more"
    const val TX_DETAIL = "txDetail/{id}"
    const val TX_EDIT = "txEdit/{id}"
    const val ACCOUNT_DETAIL = "accountDetail/{id}"
    const val CATEGORIES = "categories"
    const val REPORTS = "reports"
    const val RECURRING = "recurring"
    const val RECURRING_EDIT = "recurringEdit/{id}"
    const val LEARNING = "learning"
    const val SETTINGS = "settings"

    fun txDetail(id: Long) = "txDetail/$id"
    fun txEdit(id: Long) = "txEdit/$id"
    fun accountDetail(id: Long) = "accountDetail/$id"
    fun recurringEdit(id: Long) = "recurringEdit/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.DASHBOARD, "Home", Icons.Filled.Dashboard),
    Tab(Routes.TRANSACTIONS, "List", Icons.AutoMirrored.Filled.List),
    Tab(Routes.ACCOUNTS, "Accounts", Icons.Filled.AccountBalanceWallet),
    Tab(Routes.TAGS, "Tags", Icons.Filled.Sell),
    Tab(Routes.MORE, "More", Icons.Filled.MoreHoriz),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(vm: AppViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val message by vm.message.collectAsState()

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBars = currentRoute in tabs.map { it.route }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBars) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = backStack?.destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Routes.DASHBOARD || currentRoute == Routes.TRANSACTIONS) {
                FloatingActionButton(onClick = {
                    vm.setEditorSeed(
                        TransactionForm(
                            type = TransactionType.EXPENSE, amountMinor = 0,
                            description = "", date = todayEpoch(),
                            accountId = vm.data.value.defaultAccount?.id ?: 0,
                        )
                    )
                    nav.navigate(Routes.txEdit(0))
                }) { Icon(Icons.Filled.Add, contentDescription = "Add") }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.DASHBOARD,
            modifier = androidx.compose.ui.Modifier.padding(padding),
        ) {
            composable(Routes.DASHBOARD) { DashboardScreen(vm, nav) }
            composable(Routes.TRANSACTIONS) { TransactionsScreen(vm, nav) }
            composable(Routes.ACCOUNTS) { AccountsScreen(vm, nav) }
            composable(Routes.TAGS) { TagsScreen(vm, nav) }
            composable(Routes.MORE) { MoreScreen(nav) }
            composable(Routes.TX_DETAIL) { entry ->
                TransactionDetailScreen(vm, nav, entry.arguments?.getString("id")?.toLongOrNull() ?: 0)
            }
            composable(Routes.TX_EDIT) { entry ->
                TransactionEditScreen(vm, nav, entry.arguments?.getString("id")?.toLongOrNull() ?: 0)
            }
            composable(Routes.ACCOUNT_DETAIL) { entry ->
                AccountDetailScreen(vm, nav, entry.arguments?.getString("id")?.toLongOrNull() ?: 0)
            }
            composable(Routes.CATEGORIES) { CategoriesScreen(vm, nav) }
            composable(Routes.REPORTS) { ReportsScreen(vm, nav) }
            composable(Routes.RECURRING) { RecurringListScreen(vm, nav) }
            composable(Routes.RECURRING_EDIT) { entry ->
                RecurringEditScreen(vm, nav, entry.arguments?.getString("id")?.toLongOrNull() ?: 0)
            }
            composable(Routes.LEARNING) { LearningScreen(vm, nav) }
            composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
        }
    }
}
