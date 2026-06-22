package com.expensetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.expensetracker.domain.Analytics
import com.expensetracker.domain.Money
import com.expensetracker.domain.Period
import com.expensetracker.domain.PeriodType
import com.expensetracker.ui.AppViewModel
import com.expensetracker.ui.TxFilter
import com.expensetracker.ui.common.PeriodBar
import com.expensetracker.ui.common.todayEpoch
import com.expensetracker.ui.nav.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(vm: AppViewModel, nav: NavController, id: Long) {
    val data by vm.data.collectAsState()
    val settings by vm.settings.collectAsState()
    val acc = data.account(id)

    if (acc == null) {
        Scaffold(topBar = { TopAppBar(title = { Text("Account") }, navigationIcon = { back(nav) }) }) { p ->
            Text("Not found", Modifier.padding(p).padding(16.dp))
        }
        return
    }

    val allTime = Analytics.accountTotals(data, id, null to null, true, true, todayEpoch())
    val periodTotals = Analytics.accountTotals(
        data, id, settings.period.range(), settings.includePendingInReports, settings.includeFutureInReports, todayEpoch()
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(acc.name) },
                navigationIcon = { back(nav) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Current balance", style = MaterialTheme.typography.labelMedium)
                    Text(Money.format(allTime.balanceMinor), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }

            Text("All-time totals", style = MaterialTheme.typography.titleMedium)
            TotalsCard(allTime)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Period totals", style = MaterialTheme.typography.titleMedium)
                PeriodBar(settings.period) { vm.setPeriod(it) }
            }
            TotalsCard(periodTotals)

            Button(onClick = {
                vm.setFilter(TxFilter(accountIds = setOf(id), title = acc.name))
                nav.navigate(Routes.TRANSACTIONS)
            }, modifier = Modifier.fillMaxWidth()) { Text("View transactions") }
        }
    }
}

@Composable
private fun TotalsCard(t: com.expensetracker.domain.AccountTotals) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            totalRow("Income", Money.format(t.incomeMinor))
            totalRow("Expenses", Money.format(t.expenseMinor))
            totalRow("Transfers in", Money.format(t.transferInMinor))
            totalRow("Transfers out", Money.format(t.transferOutMinor))
        }
    }
}

@Composable
private fun totalRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun back(nav: NavController) {
    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
}
