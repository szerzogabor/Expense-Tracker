package com.expensetracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.expensetracker.ui.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavController) {
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importReplace by remember { mutableStateOf(false) }
    var askImportMode by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) scope.launch {
            val text = vm.exportCsv()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            }
            vm.message.value = "Exported CSV"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }
            if (text != null) vm.importCsv(text, importReplace)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text("Reports", Modifier.padding(start = 16.dp, top = 16.dp), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Include future transactions", Modifier.weight(1f))
                Switch(settings.includeFutureInReports, { vm.setIncludeFuture(it) })
            }
            Row(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Include pending transactions", Modifier.weight(1f))
                Switch(settings.includePendingInReports, { vm.setIncludePending(it) })
            }
            HorizontalDivider()
            Text("Data", Modifier.padding(start = 16.dp, top = 16.dp), style = MaterialTheme.typography.titleMedium)
            ListItem(
                headlineContent = { Text("Export to CSV") },
                supportingContent = { Text("Accounts, categories, tags, learning rules, transactions") },
                modifier = Modifier.clickable { exportLauncher.launch("expense-tracker.csv") },
            )
            ListItem(
                headlineContent = { Text("Import from CSV") },
                supportingContent = { Text("Replace or merge with existing data") },
                modifier = Modifier.clickable { askImportMode = true },
            )
        }
    }

    if (askImportMode) {
        AlertDialog(
            onDismissRequest = { askImportMode = false },
            title = { Text("Import mode") },
            text = { Text("Replace all existing data, or merge the imported data with what you already have?") },
            confirmButton = {
                TextButton(onClick = { importReplace = true; askImportMode = false; importLauncher.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values")) }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { importReplace = false; askImportMode = false; importLauncher.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values")) }) { Text("Merge") }
            },
        )
    }
}
