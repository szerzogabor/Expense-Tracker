package com.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.expensetracker.data.db.TransactionType
import com.expensetracker.domain.Analytics
import com.expensetracker.domain.CategorySummary
import com.expensetracker.domain.Money
import com.expensetracker.ui.AppViewModel
import com.expensetracker.ui.TxFilter
import com.expensetracker.ui.common.ColorDot
import com.expensetracker.ui.common.PeriodBar
import com.expensetracker.ui.common.TransactionRow
import com.expensetracker.ui.common.iconFor
import com.expensetracker.ui.common.todayEpoch
import com.expensetracker.ui.nav.Routes
import com.expensetracker.ui.theme.ExpenseRed
import com.expensetracker.ui.theme.IncomeGreen

private enum class CatSort { AMOUNT, NAME, COUNT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(vm: AppViewModel, nav: NavController) {
    val data by vm.data.collectAsState()
    val settings by vm.settings.collectAsState()
    var sort by remember { mutableStateOf(CatSort.AMOUNT) }
    var sortMenu by remember { mutableStateOf(false) }

    val range = settings.period.range()
    val reportTxs = Analytics.filterForReports(
        data, range, settings.includePendingInReports, settings.includeFutureInReports, todayEpoch()
    )
    val income = Analytics.income(reportTxs)
    val expense = Analytics.expense(reportTxs)
    val totalBalance = Analytics.totalActiveBalance(data)
    val balances = Analytics.accountBalances(data)
    val catSummaries = Analytics.categorySummaries(reportTxs, data).let { list ->
        when (sort) {
            CatSort.AMOUNT -> list.sortedByDescending { it.totalMinor }
            CatSort.NAME -> list.sortedBy { it.category.name }
            CatSort.COUNT -> list.sortedByDescending { it.count }
        }
    }
    val recent = data.transactions.take(8)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = { PeriodBar(settings.period) { vm.setPeriod(it) } },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxWidth()) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Current balance", style = MaterialTheme.typography.labelMedium)
                        Text(
                            Money.format(totalBalance),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Income", style = MaterialTheme.typography.labelSmall)
                                Text(Money.format(income), color = IncomeGreen, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Expenses", style = MaterialTheme.typography.labelSmall)
                                Text(Money.format(expense), color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            item { SectionHeader("Accounts") }
            items(data.activeAccounts, key = { it.id }) { acc ->
                Row(
                    Modifier.fillMaxWidth().clickable { nav.navigate(Routes.accountDetail(acc.id)) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ColorDot(acc.color) {
                        Icon(iconFor(acc.icon), null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Text(acc.name, Modifier.weight(1f).padding(start = 12.dp))
                    Text(Money.format(balances[acc.id] ?: 0L), fontWeight = FontWeight.SemiBold)
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Spending by category", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { sortMenu = true }) { Icon(Icons.Filled.Sort, "Sort") }
                    DropdownMenu(sortMenu, { sortMenu = false }) {
                        DropdownMenuItem(text = { Text("By amount") }, onClick = { sort = CatSort.AMOUNT; sortMenu = false })
                        DropdownMenuItem(text = { Text("By name") }, onClick = { sort = CatSort.NAME; sortMenu = false })
                        DropdownMenuItem(text = { Text("By count") }, onClick = { sort = CatSort.COUNT; sortMenu = false })
                    }
                }
            }
            if (catSummaries.isEmpty()) {
                item { EmptyHint("No spending in this period") }
            }
            items(catSummaries, key = { it.category.id }) { cs ->
                CategorySummaryRow(cs) {
                    vm.setFilter(TxFilter(categoryId = cs.category.id, title = cs.category.name))
                    nav.navigate(Routes.TRANSACTIONS)
                }
            }

            item { SectionHeader("Recent transactions") }
            items(recent, key = { it.tx.id }) { ui ->
                TransactionRow(ui) { nav.navigate(Routes.txDetail(ui.tx.id)) }
            }
            item { androidx.compose.foundation.layout.Spacer(Modifier.size(80.dp)) }
        }
    }
}

@Composable
private fun CategorySummaryRow(cs: CategorySummary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorDot(cs.category.color, size = 28) {
            Icon(iconFor(cs.category.icon), null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(cs.category.name)
            Text("${cs.count} txns", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(Money.format(cs.totalMinor), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SectionHeader(text: String) {
    HorizontalDivider()
    Text(
        text,
        Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
fun EmptyHint(text: String) {
    Text(
        text,
        Modifier.fillMaxWidth().padding(24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
