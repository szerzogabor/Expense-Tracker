package com.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.expensetracker.data.db.Account
import com.expensetracker.domain.Analytics
import com.expensetracker.domain.Money
import com.expensetracker.ui.AppViewModel
import com.expensetracker.ui.common.ColorDot
import com.expensetracker.ui.common.iconFor
import com.expensetracker.ui.nav.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(vm: AppViewModel, nav: NavController) {
    val data by vm.data.collectAsState()
    val balances = Analytics.accountBalances(data)
    var editing by remember { mutableStateOf<Account?>(null) }
    var creating by remember { mutableStateOf(false) }
    var archiving by remember { mutableStateOf<Account?>(null) }

    val active = data.activeAccounts
    val archived = data.accounts.filter { it.isArchived }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Accounts") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, "Add account") }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxWidth()) {
            items(active, key = { it.id }) { acc ->
                Row(
                    Modifier.fillMaxWidth().clickable { nav.navigate(Routes.accountDetail(acc.id)) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ColorDot(acc.color) { Icon(iconFor(acc.icon), null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(acc.name, fontWeight = FontWeight.Medium)
                            if (acc.isDefault) Icon(Icons.Filled.Star, "Default", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp).padding(start = 4.dp))
                        }
                        Text(Money.format(balances[acc.id] ?: 0L), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { editing = acc }) { Icon(Icons.Filled.Edit, "Edit") }
                    IconButton(onClick = { archiving = acc }) { Icon(Icons.Filled.Archive, "Archive") }
                }
            }
            if (archived.isNotEmpty()) {
                item { SectionHeader("Archived") }
                items(archived, key = { it.id }) { acc ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(acc.name, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Money.format(balances[acc.id] ?: 0L), style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { vm.unarchiveAccount(acc.id) }) { Icon(Icons.Filled.Unarchive, "Unarchive") }
                    }
                }
            }
        }
    }

    if (creating) {
        AccountEditorDialog(null, onSave = { vm.addAccount(it); creating = false }, onDismiss = { creating = false })
    }
    editing?.let { acc ->
        AccountEditorDialog(acc, onSave = { vm.updateAccount(it); editing = null }, onDismiss = { editing = null })
    }
    archiving?.let { acc ->
        ArchiveDialog(acc, active.filter { it.id != acc.id }, balances[acc.id] ?: 0L,
            onConfirm = { newDefault -> vm.archiveAccount(acc.id, newDefault); archiving = null },
            onDismiss = { archiving = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountEditorDialog(account: Account?, onSave: (Account) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var icon by remember { mutableStateOf(account?.icon ?: "wallet") }
    var color by remember { mutableStateOf(account?.color ?: 0xFF2E7D32) }
    var opening by remember { mutableStateOf(account?.openingBalanceMinor?.let { Money.toInput(it) } ?: "") }
    var isDefault by remember { mutableStateOf(account?.isDefault ?: false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) "New account" else "Edit account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(opening, { opening = it }, label = { Text("Opening balance") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Icon")
                IconPicker(icon) { icon = it }
                Text("Color")
                ColorPicker(color) { color = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(isDefault, { isDefault = it }); Text("Default account")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) { error = "Name required"; return@TextButton }
                val openMinor = Money.parse(opening) ?: 0L
                onSave(
                    (account ?: Account(name = name)).copy(
                        name = name.trim(), icon = icon, color = color,
                        openingBalanceMinor = openMinor, isDefault = isDefault,
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ArchiveDialog(account: Account, others: List<Account>, balance: Long, onConfirm: (Long?) -> Unit, onDismiss: () -> Unit) {
    var newDefault by remember { mutableStateOf(others.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }
    val needNewDefault = account.isDefault && others.isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Archive ${account.name}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (balance != 0L) Text("Warning: this account has a non-zero balance of ${Money.format(balance)}.")
                if (needNewDefault) {
                    Text("Choose a new default account:")
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(others.firstOrNull { it.id == newDefault }?.name ?: "Select")
                    }
                    DropdownMenu(expanded, { expanded = false }) {
                        others.forEach { o -> DropdownMenuItem(text = { Text(o.name) }, onClick = { newDefault = o.id; expanded = false }) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(if (needNewDefault) newDefault else null) }) { Text("Archive") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(selected: String, onPick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        com.expensetracker.ui.common.ICON_KEYS.forEach { key ->
            FilterChip(selected = key == selected, onClick = { onPick(key) },
                label = { Icon(iconFor(key), null, modifier = Modifier.size(18.dp)) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(selected: Long, onPick: (Long) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        com.expensetracker.ui.common.COLOR_CHOICES.forEach { c ->
            Column(Modifier.clickable { onPick(c) }.padding(2.dp)) {
                ColorDot(c, size = if (c == selected) 34 else 28)
            }
        }
    }
}
