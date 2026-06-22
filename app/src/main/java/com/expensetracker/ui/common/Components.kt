package com.expensetracker.ui.common

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.data.db.TransactionType
import com.expensetracker.domain.Money
import com.expensetracker.domain.UiTransaction
import com.expensetracker.ui.theme.ExpenseRed
import com.expensetracker.ui.theme.IncomeGreen
import java.time.LocalDate

@Composable
fun DateField(label: String, epochDay: Long, modifier: Modifier = Modifier, onChange: (Long) -> Unit) {
    val context = LocalContext.current
    val date = LocalDate.ofEpochDay(epochDay)
    OutlinedTextField(
        value = formatDate(epochDay),
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        enabled = false,
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                DatePickerDialog(
                    context,
                    { _, y, m, d -> onChange(LocalDate.of(y, m + 1, d).toEpochDay()) },
                    date.year, date.monthValue - 1, date.dayOfMonth
                ).show()
            },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = "Confirm",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ColorDot(color: Long, size: Int = 36, content: @Composable () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(color)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun AmountText(amountMinor: Long, type: TransactionType, modifier: Modifier = Modifier) {
    val (prefix, color) = when (type) {
        TransactionType.INCOME -> "+" to IncomeGreen
        TransactionType.EXPENSE -> "-" to ExpenseRed
        TransactionType.TRANSFER -> "" to MaterialTheme.colorScheme.onSurfaceVariant
        TransactionType.ADJUSTMENT -> (if (amountMinor >= 0) "+" else "") to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = "$prefix${Money.format(amountMinor)}",
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

@Composable
fun TransactionRow(ui: UiTransaction, onClick: () -> Unit) {
    val tx = ui.tx
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorDot(ui.category?.color ?: 0xFF9E9E9E) {
            Icon(
                iconFor(ui.category?.icon ?: "category"),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier
            .weight(1f)
            .padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tx.description.ifBlank { typeLabel(tx.type) },
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                if (tx.isPending) {
                    Text(
                        "  • pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            val subtitle = buildString {
                append(ui.account?.name ?: "—")
                if (tx.type == TransactionType.TRANSFER && ui.destAccount != null) {
                    append(" → ${ui.destAccount.name}")
                }
                if (ui.tags.isNotEmpty()) append("  #" + ui.tags.joinToString(" #") { it.name })
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        AmountText(tx.amountMinor, tx.type)
    }
}

fun typeLabel(type: TransactionType): String = when (type) {
    TransactionType.EXPENSE -> "Expense"
    TransactionType.INCOME -> "Income"
    TransactionType.TRANSFER -> "Transfer"
    TransactionType.ADJUSTMENT -> "Balance adjustment"
}
