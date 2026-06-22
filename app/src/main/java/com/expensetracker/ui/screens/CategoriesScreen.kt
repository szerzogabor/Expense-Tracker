package com.expensetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.expensetracker.data.db.Category
import com.expensetracker.ui.AppViewModel
import com.expensetracker.ui.common.ColorDot
import com.expensetracker.ui.common.ConfirmDialog
import com.expensetracker.ui.common.iconFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(vm: AppViewModel, nav: NavController) {
    val data by vm.data.collectAsState()
    val editable = data.categories.filter { !it.isOther }
    val other = data.categories.firstOrNull { it.isOther }
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, "Add") } },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxWidth()) {
            itemsIndexed(editable, key = { _, c -> c.id }) { index, cat ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(cat.color, size = 32) { Icon(iconFor(cat.icon), null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    Text(cat.name, Modifier.weight(1f).padding(start = 12.dp))
                    IconButton(enabled = index > 0, onClick = {
                        val list = editable.toMutableList(); val tmp = list[index]; list[index] = list[index - 1]; list[index - 1] = tmp
                        vm.reorderCategories(list)
                    }) { Icon(Icons.Filled.ArrowUpward, "Up") }
                    IconButton(enabled = index < editable.lastIndex, onClick = {
                        val list = editable.toMutableList(); val tmp = list[index]; list[index] = list[index + 1]; list[index + 1] = tmp
                        vm.reorderCategories(list)
                    }) { Icon(Icons.Filled.ArrowDownward, "Down") }
                    IconButton(onClick = { editing = cat }) { Icon(Icons.Filled.Edit, "Edit") }
                    IconButton(onClick = { deleting = cat }) { Icon(Icons.Filled.Delete, "Delete") }
                }
            }
            other?.let {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(it.color, size = 32) { Icon(iconFor(it.icon), null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                        Text("${it.name} (default)", Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (creating) CategoryEditorDialog(null, { vm.addCategory(it); creating = false }, { creating = false })
    editing?.let { c -> CategoryEditorDialog(c, { vm.updateCategory(it); editing = null }, { editing = null }) }
    deleting?.let { c ->
        ConfirmDialog(
            title = "Delete ${c.name}?",
            text = "Transactions in this category will be moved to \"Other\".",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteCategory(c); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
fun CategoryEditorDialog(category: Category?, onSave: (Category) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var icon by remember { mutableStateOf(category?.icon ?: "category") }
    var color by remember { mutableStateOf(category?.color ?: 0xFF607D8B) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "New category" else "Edit category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Icon"); IconPicker(icon) { icon = it }
                Text("Color"); ColorPicker(color) { color = it }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) { error = "Name required"; return@TextButton }
                onSave((category ?: Category(name = name)).copy(name = name.trim(), icon = icon, color = color))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
