package com.expensetracker.ui.common

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.expensetracker.domain.Period
import com.expensetracker.domain.PeriodType
import java.time.LocalDate

@Composable
fun PeriodBar(period: Period, onPick: (Period) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(period.label()) },
            leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val options = listOf(
                "This month" to PeriodType.THIS_MONTH,
                "Last month" to PeriodType.LAST_MONTH,
                "This year" to PeriodType.THIS_YEAR,
                "All time" to PeriodType.ALL_TIME,
            )
            options.forEach { (label, type) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    expanded = false
                    onPick(Period(type))
                })
            }
            DropdownMenuItem(text = { Text("Custom…") }, onClick = {
                expanded = false
                val today = LocalDate.now()
                DatePickerDialog(context, { _, y, m, d ->
                    val start = LocalDate.of(y, m + 1, d)
                    DatePickerDialog(context, { _, y2, m2, d2 ->
                        val end = LocalDate.of(y2, m2 + 1, d2)
                        onPick(Period(PeriodType.CUSTOM, start.toEpochDay(), end.toEpochDay()))
                    }, start.year, start.monthValue - 1, start.dayOfMonth).show()
                }, today.year, today.monthValue - 1, 1).show()
            })
        }
    }
}
