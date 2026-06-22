package com.expensetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.expensetracker.data.db.LearningRule
import com.expensetracker.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(vm: AppViewModel, nav: NavController) {
    val data by vm.data.collectAsState()
    var editing by remember { mutableStateOf<LearningRule?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Category learning rules") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, "Add") } },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth()) {
            Text(
                "When a description contains the text on the left, the category on the right is suggested (never auto-applied).",
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (data.learningRules.isEmpty()) EmptyHint("No rules yet")
            LazyColumn {
                items(data.learningRules, key = { it.id }) { rule ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("\"${rule.pattern}\" → ${data.category(rule.categoryId)?.name ?: "Other"}")
                        }
                        OutlinedButton(onClick = { editing = rule }) { Text("Edit") }
                        IconButton(onClick = { vm.deleteLearningRule(rule) }) { Icon(Icons.Filled.Delete, "Delete") }
                    }
                }
            }
        }
    }

    if (creating) {
        LearningEditorDialog(null, data.categories, onSave = { vm.addLearningRule(it); creating = false }, onDismiss = { creating = false })
    }
    editing?.let { rule ->
        LearningEditorDialog(rule, data.categories, onSave = { vm.updateLearningRule(it); editing = null }, onDismiss = { editing = null })
    }
}

@Composable
private fun LearningEditorDialog(
    rule: LearningRule?,
    categories: List<com.expensetracker.data.db.Category>,
    onSave: (LearningRule) -> Unit,
    onDismiss: () -> Unit,
) {
    var pattern by remember { mutableStateOf(rule?.pattern ?: "") }
    var categoryId by remember { mutableStateOf(rule?.categoryId ?: categories.firstOrNull { !it.isOther }?.id ?: categories.firstOrNull()?.id ?: 0L) }
    var catDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (rule == null) "New rule" else "Edit rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(pattern, { pattern = it }, label = { Text("Match text") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { catDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Category: ${categories.firstOrNull { it.id == categoryId }?.name ?: "—"}")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pattern.isBlank()) { error = "Match text required"; return@TextButton }
                onSave((rule ?: LearningRule(pattern = pattern, categoryId = categoryId)).copy(pattern = pattern.trim(), categoryId = categoryId))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (catDialog) {
        CategoryPickerDialog(categories.filter { !it.isOther }, categoryId, { it?.let { id -> categoryId = id }; catDialog = false }, { catDialog = false })
    }
}
