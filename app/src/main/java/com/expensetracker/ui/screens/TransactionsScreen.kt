package com.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.expensetracker.domain.UiTransaction
import com.expensetracker.ui.AppViewModel
import com.expensetracker.ui.common.TransactionRow
import com.expensetracker.ui.common.formatDateGroup
import com.expensetracker.ui.nav.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(vm: AppViewModel, nav: NavController) {
    val data by vm.data.collectAsState()
    val settings by vm.settings.collectAsState()
    val injected by vm.pendingFilter.collectAsState()

    var search by remember { mutableStateOf("") }
    var accountFilter by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var tagFilter by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var categoryFilter by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf<String?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<UiTransaction?>(null) }

    // Consume an injected filter (from category/tag/account navigation) once.
    LaunchedEffect(injected) {
        injected?.let {
            categoryFilter = it.categoryId
            it.tagId?.let { t -> tagFilter = setOf(t) }
            if (it.accountIds.isNotEmpty()) accountFilter = it.accountIds
            title = it.title
            vm.setFilter(null)
        }
    }

    val filtered = data.transactions.filter { ui ->
        val t = ui.tx
        if (search.isNotBlank() && !t.description.contains(search, ignoreCase = true)) return@filter false
        if (accountFilter.isNotEmpty() && t.accountId !in accountFilter && t.destAccountId !in accountFilter) return@filter false
        if (categoryFilter != null && (ui.category?.id != categoryFilter)) return@filter false
        if (tagFilter.isNotEmpty()) {
            val ids = ui.tags.map { it.id }.toSet()
            val ok = if (settings.tagFilterAnd) ids.containsAll(tagFilter) else ids.any { it in tagFilter }
            if (!ok) return@filter false
        }
        true
    }

    val grouped = filtered.groupBy { it.tx.date }.toSortedMap(compareByDescending { it })
    val anyFilter = search.isNotBlank() || accountFilter.isNotEmpty() || tagFilter.isNotEmpty() || categoryFilter != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title ?: "Transactions") },
                actions = {
                    if (anyFilter) IconButton(onClick = {
                        search = ""; accountFilter = emptySet(); tagFilter = emptySet(); categoryFilter = null; title = null
                    }) { Icon(Icons.Filled.Clear, "Clear filters") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search description") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = tagFilter.isNotEmpty(),
                    onClick = { showTagDialog = true },
                    label = { Text(if (tagFilter.isEmpty()) "Tags" else "Tags (${tagFilter.size})") },
                    leadingIcon = { Icon(Icons.Filled.FilterList, null) },
                )
                if (tagFilter.size > 1) {
                    FilterChip(
                        selected = settings.tagFilterAnd,
                        onClick = { vm.setTagFilterAnd(!settings.tagFilterAnd) },
                        label = { Text(if (settings.tagFilterAnd) "Match ALL" else "Match ANY") },
                    )
                }
                data.activeAccounts.forEach { acc ->
                    FilterChip(
                        selected = acc.id in accountFilter,
                        onClick = {
                            accountFilter = if (acc.id in accountFilter) accountFilter - acc.id else accountFilter + acc.id
                        },
                        label = { Text(acc.name) },
                    )
                }
            }

            if (filtered.isEmpty()) {
                EmptyHint("No transactions")
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    grouped.forEach { (date, txs) ->
                        item(key = "h$date") {
                            Text(
                                formatDateGroup(date),
                                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        items(txs, key = { it.tx.id }) { ui ->
                            SwipeRow(
                                ui = ui,
                                onEdit = { nav.navigate(Routes.txEdit(ui.tx.id)) },
                                onDelete = { pendingDelete = ui },
                                onClick = { nav.navigate(Routes.txDetail(ui.tx.id)) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTagDialog) {
        TagPickerDialog(
            allTags = data.tags,
            selected = tagFilter,
            onDone = { tagFilter = it; showTagDialog = false },
            onDismiss = { showTagDialog = false },
        )
    }

    pendingDelete?.let { ui ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete transaction?") },
            text = { Text("\"${ui.tx.description}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteTransaction(ui.tx); pendingDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeRow(ui: UiTransaction, onEdit: () -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); false }
                SwipeToDismissBoxValue.StartToEnd -> { onEdit(); false }
                else -> false
            }
        }
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val toDelete = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val color = if (toDelete) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
            Box(Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp), contentAlignment = if (toDelete) Alignment.CenterEnd else Alignment.CenterStart) {
                Icon(if (toDelete) Icons.Filled.Delete else Icons.Filled.Edit, null)
            }
        },
    ) {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
            TransactionRow(ui, onClick)
        }
    }
}

@Composable
private fun TagPickerDialog(
    allTags: List<com.expensetracker.data.db.Tag>,
    selected: Set<Long>,
    onDone: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var sel by remember { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by tags") },
        text = {
            if (allTags.isEmpty()) Text("No tags yet")
            else LazyColumn {
                items(allTags, key = { it.id }) { tag ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = tag.id in sel, onCheckedChange = {
                            sel = if (tag.id in sel) sel - tag.id else sel + tag.id
                        })
                        Text("#${tag.name}")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(sel) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = { onDone(emptySet()) }) { Text("Clear") } },
    )
}
