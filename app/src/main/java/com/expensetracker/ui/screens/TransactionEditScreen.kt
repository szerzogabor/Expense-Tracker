package com.expensetracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.expensetracker.data.db.Account
import com.expensetracker.data.db.TransactionType
import com.expensetracker.data.repo.Repository
import com.expensetracker.data.repo.TransactionForm
import com.expensetracker.domain.Analytics
import com.expensetracker.domain.Money
import com.expensetracker.ui.AppViewModel
import com.expensetracker.ui.common.DateField
import com.expensetracker.ui.common.todayEpoch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEditScreen(vm: AppViewModel, nav: NavController, id: Long) {
    val data by vm.data.collectAsState()
    val seed by vm.editorSeed.collectAsState()

    val existing = if (id != 0L) data.transactions.firstOrNull { it.tx.id == id } else null

    // Initial values (keyed on the loaded id so they populate once data arrives)
    val key = existing?.tx?.id
    var type by remember(key) { mutableStateOf(existing?.tx?.type ?: seed?.type ?: TransactionType.EXPENSE) }
    var amount by remember(key) { mutableStateOf(existing?.tx?.amountMinor?.let { Money.toInput(it) } ?: seed?.amountMinor?.takeIf { it != 0L }?.let { Money.toInput(it) } ?: "") }
    var description by remember(key) { mutableStateOf(existing?.tx?.description ?: seed?.description ?: "") }
    var date by remember(key) { mutableStateOf(existing?.tx?.date ?: seed?.date ?: todayEpoch()) }
    var accountId by remember(key) { mutableStateOf(existing?.tx?.accountId ?: seed?.accountId ?: data.defaultAccount?.id ?: 0L) }
    var destId by remember(key) { mutableStateOf(existing?.tx?.destAccountId ?: seed?.destAccountId) }
    var categoryId by remember(key) { mutableStateOf(existing?.tx?.categoryId ?: seed?.categoryId) }
    var note by remember(key) { mutableStateOf(existing?.tx?.note ?: seed?.note ?: "") }
    var tags by remember(key) { mutableStateOf(existing?.tags?.map { it.name } ?: seed?.tagNames ?: emptyList()) }
    var userTouchedCategory by remember(key) { mutableStateOf(existing != null) }

    var dirty by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var showNegWarn by remember { mutableStateOf(false) }
    var showScope by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var catDialog by remember { mutableStateOf(false) }

    val isRecurring = existing?.tx?.recurringRuleId != null

    fun markDirty() { dirty = true }

    val suggestion = if (!userTouchedCategory && (type == TransactionType.EXPENSE || type == TransactionType.INCOME))
        Analytics.suggestCategory(description, data.learningRules) else null

    fun buildForm(): TransactionForm? {
        val minor = Money.parse(amount)
        if (minor == null || (type != TransactionType.ADJUSTMENT && minor <= 0)) { error = "Enter a valid amount"; return null }
        if (type == TransactionType.ADJUSTMENT && minor == 0L) { error = "Adjustment cannot be zero"; return null }
        if (description.isBlank()) { error = "Description is required"; return null }
        if (accountId == 0L) { error = "Select an account"; return null }
        if (type == TransactionType.TRANSFER) {
            if (destId == null) { error = "Select a destination account"; return null }
            if (destId == accountId) { error = "Source and destination must differ"; return null }
        }
        return TransactionForm(
            id = existing?.tx?.id ?: 0,
            type = type,
            amountMinor = minor,
            description = description,
            date = date,
            accountId = accountId,
            destAccountId = destId,
            categoryId = categoryId,
            note = note,
            tagNames = tags,
            isPending = existing?.tx?.isPending ?: false,
            recurringRuleId = existing?.tx?.recurringRuleId,
            occurrenceDate = existing?.tx?.occurrenceDate,
        )
    }

    fun wouldGoNegative(form: TransactionForm): Boolean {
        if (form.type == TransactionType.INCOME || form.type == TransactionType.ADJUSTMENT) return false
        val balances = Analytics.accountBalances(data)
        val cur = balances[form.accountId] ?: 0L
        // approximate effect of this transaction on the source account
        return (cur - form.amountMinor) < 0
    }

    fun doSave(saveAndNew: Boolean) {
        val form = buildForm() ?: return
        if (isRecurring) { showScope = true; return }
        if (wouldGoNegative(form)) { showNegWarn = true; return }
        persist(vm, form, Repository.EditScope.ALL) {
            if (saveAndNew) {
                amount = ""; description = ""; note = ""; tags = emptyList(); categoryId = null
                userTouchedCategory = false; dirty = false
            } else {
                dirty = false; nav.popBackStack()
            }
        }
    }

    BackHandler(enabled = dirty) { showDiscard = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id == 0L) "New transaction" else "Edit transaction") },
                navigationIcon = {
                    IconButton(onClick = { if (dirty) showDiscard = true else nav.popBackStack() }) {
                        Icon(Icons.Filled.Close, "Close")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Type selector
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeButton("Expense", type == TransactionType.EXPENSE, Modifier.weight(1f)) { type = TransactionType.EXPENSE; markDirty() }
                TypeButton("Income", type == TransactionType.INCOME, Modifier.weight(1f)) { type = TransactionType.INCOME; markDirty() }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeButton("Transfer", type == TransactionType.TRANSFER, Modifier.weight(1f)) { type = TransactionType.TRANSFER; markDirty() }
                TypeButton("Adjustment", type == TransactionType.ADJUSTMENT, Modifier.weight(1f)) { type = TransactionType.ADJUSTMENT; markDirty() }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; markDirty() },
                label = { Text(if (type == TransactionType.ADJUSTMENT) "Delta amount (+/-)" else "Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it; markDirty() },
                label = { Text("Description") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (suggestion != null) {
                val cat = data.category(suggestion)
                if (cat != null) {
                    AssistChip(onClick = { categoryId = suggestion; userTouchedCategory = true; markDirty() },
                        label = { Text("Suggested category: ${cat.name}") })
                }
            }

            DateField("Date", date) { date = it; markDirty() }

            AccountDropdown("Account", data.activeAccounts, accountId) { accountId = it; markDirty() }

            if (type == TransactionType.TRANSFER) {
                AccountDropdown("To account", data.activeAccounts.filter { it.id != accountId }, destId ?: 0L) { destId = it; markDirty() }
            }

            if (type == TransactionType.EXPENSE || type == TransactionType.INCOME) {
                OutlinedButton(onClick = { catDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(data.category(categoryId)?.name ?: "Category: Other")
                }
            }

            // Tags
            TagEditor(tags, onAdd = { tags = (tags + it).distinct(); markDirty() }, onRemove = { tags = tags - it; markDirty() })

            OutlinedTextField(
                value = note,
                onValueChange = { note = it; markDirty() },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { doSave(saveAndNew = true) }, modifier = Modifier.weight(1f)) { Text("Save & New") }
                Button(onClick = { doSave(saveAndNew = false) }, modifier = Modifier.weight(1f)) { Text("Save") }
            }
        }
    }

    if (catDialog) {
        CategoryPickerDialog(
            categories = data.categories,
            selected = categoryId,
            onPick = { categoryId = it; userTouchedCategory = true; markDirty(); catDialog = false },
            onDismiss = { catDialog = false },
        )
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes.") },
            confirmButton = { TextButton(onClick = { showDiscard = false; nav.popBackStack() }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Keep editing") } },
        )
    }

    if (showNegWarn) {
        val form = buildForm()
        AlertDialog(
            onDismissRequest = { showNegWarn = false },
            title = { Text("Negative balance") },
            text = { Text("This transaction makes the account balance negative. Save anyway?") },
            confirmButton = {
                TextButton(onClick = {
                    showNegWarn = false
                    if (form != null) persist(vm, form, Repository.EditScope.ALL) { dirty = false; nav.popBackStack() }
                }) { Text("Save anyway") }
            },
            dismissButton = { TextButton(onClick = { showNegWarn = false }) { Text("Cancel") } },
        )
    }

    if (showScope) {
        val form = buildForm()
        AlertDialog(
            onDismissRequest = { showScope = false },
            title = { Text("Edit recurring transaction") },
            text = { Text("Apply this change to:") },
            confirmButton = {
                Column {
                    TextButton(onClick = { showScope = false; form?.let { persist(vm, it, Repository.EditScope.THIS) { dirty = false; nav.popBackStack() } } }) { Text("This occurrence only") }
                    TextButton(onClick = { showScope = false; form?.let { persist(vm, it, Repository.EditScope.THIS_AND_FUTURE) { dirty = false; nav.popBackStack() } } }) { Text("This and future") }
                    TextButton(onClick = { showScope = false; form?.let { persist(vm, it, Repository.EditScope.ALL) { dirty = false; nav.popBackStack() } } }) { Text("All occurrences") }
                }
            },
            dismissButton = { TextButton(onClick = { showScope = false }) { Text("Cancel") } },
        )
    }
}

private fun persist(vm: AppViewModel, form: TransactionForm, scope: Repository.EditScope, onDone: () -> Unit) {
    vm.saveTransactionScoped(form, scope, onDone)
}

@Composable
private fun TypeButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(label: String, accounts: List<Account>, selectedId: Long, onSelect: (Long) -> Unit) {
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
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { acc ->
                DropdownMenuItem(text = { Text(acc.name) }, onClick = { onSelect(acc.id); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TagEditor(tags: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Add tag") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = { if (input.isNotBlank()) { onAdd(input.trim()); input = "" } }) { Text("Add") }
        }
        FlowRow(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { tag ->
                InputChip(
                    selected = false,
                    onClick = { onRemove(tag) },
                    label = { Text("#$tag") },
                    trailingIcon = { Icon(Icons.Filled.Close, null) },
                )
            }
        }
    }
}
