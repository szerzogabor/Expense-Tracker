package com.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.expensetracker.data.db.Account
import com.expensetracker.data.db.GenerationMode
import com.expensetracker.data.db.RecurringRule
import com.expensetracker.data.db.TransactionType
import com.expensetracker.domain.Money
import com.expensetracker.domain.RecurrenceEngine
import com.expensetracker.ui.AppViewModel
import com.expensetracker.ui.common.DateField
import com.expensetracker.ui.common.formatDate
import com.expensetracker.ui.common.todayEpoch
import com.expensetracker.ui.nav.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(vm: AppViewModel, nav: NavController) {
    val rules by vm.recurring.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { nav.navigate(Routes.recurringEdit(0)) }) { Icon(Icons.Filled.Add, "Add") } },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (rules.isEmpty()) EmptyHint("No recurring rules. Tap + to add one.")
            rules.forEach { rule ->
                val next = RecurrenceEngine.nextOccurrence(rule.rrule, rule.startDate, maxOf(rule.startDate, todayEpoch()))
                Row(
                    Modifier.fillMaxWidth().clickable { nav.navigate(Routes.recurringEdit(rule.id)) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(rule.description, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${Money.format(rule.amountMinor)} • ${RecurrenceEngine.describe(rule.rrule)} • ${if (rule.generationMode == GenerationMode.AUTO) "Auto" else "Pending"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            if (rule.enabled) "Next: ${next?.let { formatDate(it) } ?: "—"}" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { vm.deleteRecurring(rule) }) { Icon(Icons.Filled.Delete, "Delete") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringEditScreen(vm: AppViewModel, nav: NavController, id: Long) {
    val data by vm.data.collectAsState()
    val rules by vm.recurring.collectAsState()
    val existing = rules.firstOrNull { it.id == id }
    val key = existing?.id

    var type by remember(key) { mutableStateOf(existing?.type ?: TransactionType.EXPENSE) }
    var amount by remember(key) { mutableStateOf(existing?.amountMinor?.let { Money.toInput(it) } ?: "") }
    var description by remember(key) { mutableStateOf(existing?.description ?: "") }
    var accountId by remember(key) { mutableStateOf(existing?.accountId ?: data.defaultAccount?.id ?: 0L) }
    var destId by remember(key) { mutableStateOf(existing?.destAccountId) }
    var categoryId by remember(key) { mutableStateOf(existing?.categoryId) }
    var note by remember(key) { mutableStateOf(existing?.note ?: "") }
    var tags by remember(key) { mutableStateOf(existing?.templateTags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()) }
    var rrule by remember(key) { mutableStateOf(existing?.rrule ?: "FREQ=MONTHLY;INTERVAL=1") }
    var startDate by remember(key) { mutableStateOf(existing?.startDate ?: todayEpoch()) }
    var mode by remember(key) { mutableStateOf(existing?.generationMode ?: GenerationMode.AUTO) }
    var enabled by remember(key) { mutableStateOf(existing?.enabled ?: true) }
    var error by remember { mutableStateOf<String?>(null) }
    var catDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id == 0L) "New recurring rule" else "Edit recurring rule") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Expense" to TransactionType.EXPENSE,
                    "Income" to TransactionType.INCOME,
                    "Transfer" to TransactionType.TRANSFER,
                ).forEach { (label, t) ->
                    FilterChip(selected = type == t, onClick = { type = t }, label = { Text(label) })
                }
            }
            OutlinedTextField(amount, { amount = it }, label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            SimpleAccountDropdown("Account", data.activeAccounts, accountId) { accountId = it }
            if (type == TransactionType.TRANSFER) {
                SimpleAccountDropdown("To account", data.activeAccounts.filter { it.id != accountId }, destId ?: 0L) { destId = it }
            }
            if (type == TransactionType.EXPENSE || type == TransactionType.INCOME) {
                OutlinedButton(onClick = { catDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(data.category(categoryId)?.name ?: "Category: Other")
                }
            }

            Text("Repeat")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecurrenceEngine.PRESETS.forEach { (label, value) ->
                    FilterChip(selected = rrule == value, onClick = { rrule = value }, label = { Text(label) })
                }
            }
            OutlinedTextField(rrule, { rrule = it }, label = { Text("RRULE (RFC-5545)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            DateField("Start date", startDate) { startDate = it }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == GenerationMode.AUTO, onClick = { mode = GenerationMode.AUTO }, label = { Text("Auto-create") })
                FilterChip(selected = mode == GenerationMode.PENDING, onClick = { mode = GenerationMode.PENDING }, label = { Text("Pending approval") })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(enabled, { enabled = it }); Text("  Enabled")
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(onClick = {
                val minor = Money.parse(amount)
                if (minor == null || minor <= 0) { error = "Enter a valid amount"; return@Button }
                if (description.isBlank()) { error = "Description required"; return@Button }
                if (!RecurrenceEngine.isValid(rrule)) { error = "Invalid RRULE"; return@Button }
                if (accountId == 0L) { error = "Select an account"; return@Button }
                val rule = (existing ?: RecurringRule(rrule = rrule, startDate = startDate, type = type, amountMinor = minor, description = description, accountId = accountId)).copy(
                    rrule = rrule, startDate = startDate, type = type, amountMinor = minor,
                    description = description.trim(), accountId = accountId,
                    destAccountId = if (type == TransactionType.TRANSFER) destId else null,
                    categoryId = if (type == TransactionType.EXPENSE || type == TransactionType.INCOME) categoryId else null,
                    note = note.ifBlank { null }, templateTags = tags.joinToString(","),
                    generationMode = mode, enabled = enabled,
                )
                if (id == 0L) vm.addRecurring(rule) else vm.updateRecurring(rule)
                nav.popBackStack()
            }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }
    }

    if (catDialog) {
        CategoryPickerDialog(data.categories, categoryId, { categoryId = it; catDialog = false }, { catDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleAccountDropdown(label: String, accounts: List<Account>, selectedId: Long, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedId }
    Column {
        OutlinedTextField(
            value = selected?.name ?: "Select…",
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = false,
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
        )
        DropdownMenu(expanded, { expanded = false }) {
            accounts.forEach { acc -> DropdownMenuItem(text = { Text(acc.name) }, onClick = { onSelect(acc.id); expanded = false }) }
        }
    }
}
