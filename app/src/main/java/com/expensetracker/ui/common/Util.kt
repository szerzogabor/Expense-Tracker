package com.expensetracker.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val iconMap: Map<String, ImageVector> = mapOf(
    "wallet" to Icons.Filled.AccountBalanceWallet,
    "cash" to Icons.Filled.Money,
    "bank" to Icons.Filled.AccountBalance,
    "card" to Icons.Filled.CreditCard,
    "savings" to Icons.Filled.Savings,
    "category" to Icons.Filled.Category,
    "grocery" to Icons.Filled.LocalGroceryStore,
    "transport" to Icons.Filled.DirectionsCar,
    "dining" to Icons.Filled.Fastfood,
    "bills" to Icons.Filled.Receipt,
    "shopping" to Icons.Filled.ShoppingBag,
    "health" to Icons.Filled.Favorite,
    "home" to Icons.Filled.Home,
    "salary" to Icons.Filled.Payments,
)

val ICON_KEYS = iconMap.keys.toList()

fun iconFor(key: String): ImageVector = iconMap[key] ?: Icons.Filled.Category

val COLOR_CHOICES = listOf(
    0xFF1B5E20, 0xFF2E7D32, 0xFF388E3C, 0xFF1976D2, 0xFF0288D1, 0xFF00897B,
    0xFF7B1FA2, 0xFFC2185B, 0xFFE64A19, 0xFFF57C00, 0xFFFBC02D, 0xFF607D8B,
    0xFF455A64, 0xFF9E9E9E, 0xFFC62828, 0xFF5D4037,
)

private val dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val groupFmt = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")

fun formatDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dayFmt)

fun formatDateGroup(epochDay: Long): String {
    val date = LocalDate.ofEpochDay(epochDay)
    val today = LocalDate.now()
    return when (epochDay) {
        today.toEpochDay() -> "Today"
        today.minusDays(1).toEpochDay() -> "Yesterday"
        else -> date.format(groupFmt)
    }
}

fun todayEpoch(): Long = LocalDate.now().toEpochDay()
