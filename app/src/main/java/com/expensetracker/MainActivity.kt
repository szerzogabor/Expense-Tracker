package com.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.ui.AppViewModel
import com.expensetracker.ui.nav.AppScaffold
import com.expensetracker.ui.theme.ExpenseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ExpenseApp).container
        setContent {
            ExpenseTheme {
                val vm: AppViewModel = viewModel(factory = AppViewModel.factory(container))
                AppScaffold(vm)
            }
        }
    }
}
