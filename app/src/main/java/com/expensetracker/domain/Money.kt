package com.expensetracker.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.abs

object Money {
    private val symbols = DecimalFormatSymbols().apply {
        groupingSeparator = ' '
        decimalSeparator = '.'
    }
    private val grouped = DecimalFormat("#,##0", symbols)
    private val groupedDecimals = DecimalFormat("#,##0.00", symbols)

    /** Parse user input (e.g. "1234.5" or "1 234,5") to minor units (x100). Null if invalid. */
    fun parse(input: String): Long? {
        val cleaned = input.trim()
            .replace(" ", "")
            .replace(",", ".")
        if (cleaned.isEmpty()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        return Math.round(value * 100.0)
    }

    /** Format minor units to a grouped string, hiding ".00" when whole. */
    fun format(minor: Long): String {
        val absMinor = abs(minor)
        val whole = absMinor / 100
        val frac = absMinor % 100
        val body = if (frac == 0L) grouped.format(whole) else groupedDecimals.format(absMinor / 100.0)
        return if (minor < 0) "-$body" else body
    }

    fun formatSigned(minor: Long): String =
        if (minor >= 0) "+${format(minor)}" else format(minor)

    /** For an input field: show plain decimal value. */
    fun toInput(minor: Long): String {
        if (minor == 0L) return ""
        return if (minor % 100 == 0L) (minor / 100).toString()
        else (minor / 100.0).toString()
    }
}
