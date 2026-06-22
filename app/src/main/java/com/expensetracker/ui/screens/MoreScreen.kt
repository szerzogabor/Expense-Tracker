package com.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.expensetracker.ui.nav.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(nav: NavController) {
    Scaffold(topBar = { TopAppBar(title = { Text("More") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth()) {
            ListItem(
                headlineContent = { Text("Reports") },
                leadingContent = { Icon(Icons.Filled.Assessment, null) },
                modifier = Modifier.clickable { nav.navigate(Routes.REPORTS) },
            )
            ListItem(
                headlineContent = { Text("Categories") },
                leadingContent = { Icon(Icons.Filled.Category, null) },
                modifier = Modifier.clickable { nav.navigate(Routes.CATEGORIES) },
            )
            ListItem(
                headlineContent = { Text("Recurring transactions") },
                leadingContent = { Icon(Icons.Filled.Repeat, null) },
                modifier = Modifier.clickable { nav.navigate(Routes.RECURRING) },
            )
            ListItem(
                headlineContent = { Text("Category learning rules") },
                leadingContent = { Icon(Icons.Filled.Lightbulb, null) },
                modifier = Modifier.clickable { nav.navigate(Routes.LEARNING) },
            )
            ListItem(
                headlineContent = { Text("Settings & data") },
                leadingContent = { Icon(Icons.Filled.Settings, null) },
                modifier = Modifier.clickable { nav.navigate(Routes.SETTINGS) },
            )
        }
    }
}
