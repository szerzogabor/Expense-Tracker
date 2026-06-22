package com.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.expensetracker.data.repo.TransactionForm
import com.expensetracker.ui.AppViewModel
import com.expensetracker.ui.TxFilter
import com.expensetracker.ui.common.AmountText
import com.expensetracker.ui.common.ConfirmDialog
import com.expensetracker.ui.common.formatDate
import com.expensetracker.ui.common.typeLabel
import com.expensetracker.ui.nav.Routes

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailScreen(vm: AppViewModel, nav: NavController, id: Long) {
    val data by vm.data.collectAsState()
    val ui = data.transactions.firstOrNull { it.tx.id == id }
    var confirmDelete by remember { mutableStateOf(false) }

    if (ui == null) {
        Scaffold(topBar = { topBar(nav, "Transaction") }) { p ->
            Text("Not found", Modifier.padding(p).padding(16.dp))
        }
        return
    }
    val tx = ui.tx

    fun seedFrom(useToday: Boolean) = TransactionForm(
        id = 0,
        type = tx.type, amountMinor = tx.amountMinor, description = tx.description,
        date = if (useToday) com.expensetracker.ui.common.todayEpoch() else tx.date,
        accountId = tx.accountId, destAccountId = tx.destAccountId,
        categoryId = tx.categoryId, note = tx.note, tagNames = ui.tags.map { it.name },
    )

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Transaction") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.txEdit(tx.id)) }) { Icon(Icons.Filled.Edit, "Edit") }
                    IconButton(onClick = { vm.setEditorSeed(seedFrom(false)); nav.navigate(Routes.txEdit(0)) }) { Icon(Icons.Filled.ContentCopy, "Duplicate") }
                    IconButton(onClick = { vm.setEditorSeed(seedFrom(true)); nav.navigate(Routes.txEdit(0)) }) { Icon(Icons.Filled.Refresh, "Reuse") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(typeLabel(tx.type), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                AmountText(tx.amountMinor, tx.type)
            }
            Text(tx.description, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (tx.isPending) Text("Pending approval", color = MaterialTheme.colorScheme.tertiary)
            HorizontalDivider()
            DetailRow("Date", formatDate(tx.date))
            DetailRow("Account", ui.account?.name ?: "—")
            if (tx.type == com.expensetracker.data.db.TransactionType.TRANSFER) {
                DetailRow("To account", ui.destAccount?.name ?: "—")
            }
            if (tx.type == com.expensetracker.data.db.TransactionType.EXPENSE || tx.type == com.expensetracker.data.db.TransactionType.INCOME) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Category", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        ui.category?.name ?: "Other",
                        Modifier.clickable {
                            ui.category?.let { vm.setFilter(TxFilter(categoryId = it.id, title = it.name)); nav.navigate(Routes.TRANSACTIONS) }
                        },
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (ui.tags.isNotEmpty()) {
                Text("Tags", color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ui.tags.forEach { tag ->
                        AssistChip(onClick = {
                            vm.setFilter(TxFilter(tagId = tag.id, title = "#${tag.name}")); nav.navigate(Routes.TRANSACTIONS)
                        }, label = { Text("#${tag.name}") })
                    }
                }
            }
            tx.note?.let {
                HorizontalDivider()
                Text("Note", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(it)
            }
            if (tx.isPending) {
                androidx.compose.material3.Button(onClick = { vm.approvePending(tx) }, modifier = Modifier.fillMaxWidth()) { Text("Approve") }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete transaction?",
            text = "This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = { confirmDelete = false; vm.deleteTransaction(tx); nav.popBackStack() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun topBar(nav: NavController, title: String) {
    androidx.compose.material3.TopAppBar(
        title = { Text(title) },
        navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}
