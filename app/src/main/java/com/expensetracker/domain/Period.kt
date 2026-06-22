package com.expensetracker.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class PeriodType { THIS_MONTH, LAST_MONTH, THIS_YEAR, ALL_TIME, CUSTOM }

/** A selected reporting period. start/end are inclusive epoch days; null means unbounded. */
data class Period(
    val type: PeriodType,
    val customStart: Long? = null,
    val customEnd: Long? = null,
) {
    fun range(today: LocalDate = LocalDate.now()): Pair<Long?, Long?> = when (type) {
        PeriodType.THIS_MONTH -> {
            val first = today.withDayOfMonth(1)
            first.toEpochDay() to first.plusMonths(1).minusDays(1).toEpochDay()
        }
        PeriodType.LAST_MONTH -> {
            val first = today.withDayOfMonth(1).minusMonths(1)
            first.toEpochDay() to first.plusMonths(1).minusDays(1).toEpochDay()
        }
        PeriodType.THIS_YEAR -> {
            val first = today.withDayOfYear(1)
            first.toEpochDay() to first.plusYears(1).minusDays(1).toEpochDay()
        }
        PeriodType.ALL_TIME -> null to null
        PeriodType.CUSTOM -> customStart to customEnd
    }

    fun label(today: LocalDate = LocalDate.now()): String = when (type) {
        PeriodType.THIS_MONTH -> today.format(monthFmt)
        PeriodType.LAST_MONTH -> today.minusMonths(1).format(monthFmt)
        PeriodType.THIS_YEAR -> today.year.toString()
        PeriodType.ALL_TIME -> "All time"
        PeriodType.CUSTOM -> {
            val s = customStart?.let { LocalDate.ofEpochDay(it).format(dayFmt) } ?: "…"
            val e = customEnd?.let { LocalDate.ofEpochDay(it).format(dayFmt) } ?: "…"
            "$s – $e"
        }
    }

    companion object {
        private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
        private val dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        fun contains(range: Pair<Long?, Long?>, epochDay: Long): Boolean {
            val (start, end) = range
            if (start != null && epochDay < start) return false
            if (end != null && epochDay > end) return false
            return true
        }
    }
}
