package com.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.expensetracker.data.db.Tag
import com.expensetracker.domain.Analytics
import com.expensetracker.domain.Money
import com.expensetracker.ui.AppViewModel
import com.expensetracker.ui.TxFilter
import com.expensetracker.ui.common.ConfirmDialog
import com.expensetracker.ui.nav.Routes

private enum class TagSort { NAME, COUNT, AMOUNT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(vm: AppViewModel, nav: NavController) {
    val data by vm.data.collectAsState()
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(TagSort.AMOUNT) }
    var sortMenu by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Tag?>(null) }

    val summaries = Analytics.tagSummaries(data.transactions, type = null).associateBy { it.tag.id }
    val rows = data.tags
        .filter { it.name.contains(query, ignoreCase = true) }
        .map { tag ->
            val s = summaries[tag.id]
            Triple(tag, s?.totalMinor ?: 0L, s?.count ?: 0)
        }
        .let { list ->
            when (sort) {
                TagSort.NAME -> list.sortedBy { it.first.name }
                TagSort.COUNT -> list.sortedByDescending { it.third }
                TagSort.AMOUNT -> list.sortedByDescending { it.second }
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tags") },
                actions = {
                    IconButton(onClick = { sortMenu = true }) { Icon(Icons.Filled.Sort, "Sort") }
                    DropdownMenu(sortMenu, { sortMenu = false }) {
                        DropdownMenuItem(text = { Text("By amount") }, onClick = { sort = TagSort.AMOUNT; sortMenu = false })
                        DropdownMenuItem(text = { Text("By name") }, onClick = { sort = TagSort.NAME; sortMenu = false })
                        DropdownMenuItem(text = { Text("By count") }, onClick = { sort = TagSort.COUNT; sortMenu = false })
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth()) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search tags") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            if (rows.isEmpty()) EmptyHint("No tags yet")
            LazyColumn {
                items(rows, key = { it.first.id }) { (tag, amount, count) ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            vm.setFilter(TxFilter(tagId = tag.id, title = "#${tag.name}")); nav.navigate(Routes.TRANSACTIONS)
                        }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("#${tag.name}", fontWeight = FontWeight.Medium)
                            Text("$count txns", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(Money.format(amount), fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { deleting = tag }) { Icon(Icons.Filled.Delete, "Delete") }
                    }
                }
            }
        }
    }

    deleting?.let { tag ->
        ConfirmDialog(
            title = "Delete #${tag.name}?",
            text = "The tag will be removed from all transactions.",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteTag(tag); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}
