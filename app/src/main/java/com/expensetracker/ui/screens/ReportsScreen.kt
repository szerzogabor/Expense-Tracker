package com.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.expensetracker.domain.Money
import com.expensetracker.ui.AppViewModel
import com.expensetracker.ui.TxFilter
import com.expensetracker.ui.common.PeriodBar
import com.expensetracker.ui.common.todayEpoch
import com.expensetracker.ui.nav.Routes

private enum class Dimension { CATEGORY, TAG, ACCOUNT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(vm: AppViewModel, nav: NavController) {
    val data by vm.data.collectAsState()
    val settings by vm.settings.collectAsState()
    var dimension by remember { mutableStateOf(Dimension.CATEGORY) }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }

    val txs = Analytics.filterForReports(
        data, settings.period.range(), settings.includePendingInReports, settings.includeFutureInReports, todayEpoch()
    )

    data class Bar(val label: String, val amount: Long, val count: Int, val color: Long, val onClick: () -> Unit)

    val bars: List<Bar> = when (dimension) {
        Dimension.CATEGORY -> Analytics.categorySummaries(txs, data, type).sortedByDescending { it.totalMinor }.map {
            Bar(it.category.name, it.totalMinor, it.count, it.category.color) {
                vm.setFilter(TxFilter(categoryId = it.category.id, title = it.category.name)); nav.navigate(Routes.TRANSACTIONS)
            }
        }
        Dimension.TAG -> Analytics.tagSummaries(txs, type).sortedByDescending { it.totalMinor }.map {
            Bar("#${it.tag.name}", it.totalMinor, it.count, 0xFF455A64) {
                vm.setFilter(TxFilter(tagId = it.tag.id, title = "#${it.tag.name}")); nav.navigate(Routes.TRANSACTIONS)
            }
        }
        Dimension.ACCOUNT -> data.activeAccounts.map { acc ->
            val sub = txs.filter { it.tx.type == type && (it.tx.accountId == acc.id) }
            Bar(acc.name, sub.sumOf { it.tx.amountMinor }, sub.size, acc.color) {
                vm.setFilter(TxFilter(accountIds = setOf(acc.id), title = acc.name)); nav.navigate(Routes.TRANSACTIONS)
            }
        }.filter { it.amount > 0 }.sortedByDescending { it.amount }
    }
    val max = bars.maxOfOrNull { it.amount } ?: 0L
    val total = bars.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { PeriodBar(settings.period) { vm.setPeriod(it) } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxWidth()) {
            item {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Category" to Dimension.CATEGORY,
                        "Tag" to Dimension.TAG,
                        "Account" to Dimension.ACCOUNT,
                    ).forEach { (label, dim) ->
                        FilterChip(selected = dimension == dim, onClick = { dimension = dim }, label = { Text(label) })
                    }
                }
            }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SegmentedButton(selected = type == TransactionType.EXPENSE, onClick = { type = TransactionType.EXPENSE },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Expense") }
                    SegmentedButton(selected = type == TransactionType.INCOME, onClick = { type = TransactionType.INCOME },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Income") }
                }
            }
            item {
                Text("Total: ${Money.format(total)}", Modifier.padding(16.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            if (bars.isEmpty()) item { EmptyHint("No data for this period") }
            items(bars) { bar ->
                Column(Modifier.fillMaxWidth().clickable(onClick = bar.onClick).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(bar.label)
                        Text("${Money.format(bar.amount)}  (${bar.count})", fontWeight = FontWeight.Medium)
                    }
                    Box(
                        Modifier.fillMaxWidth(if (max > 0) (bar.amount.toFloat() / max).coerceIn(0.02f, 1f) else 0.02f)
                            .height(8.dp).padding(top = 4.dp)
                            .background(Color(bar.color))
                    )
                }
            }
        }
    }
}
