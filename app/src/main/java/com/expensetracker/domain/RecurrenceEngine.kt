package com.expensetracker.domain

import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.recur.RecurrenceRule
import java.time.LocalDate

/**
 * Thin wrapper over lib-recur providing RFC-5545 RRULE expansion against
 * date-only (floating, all-day) occurrences.
 */
object RecurrenceEngine {

    /** Returns occurrence epoch days in [fromEpochDay, toEpochDay], skipping exceptions. */
    fun occurrencesBetween(
        rrule: String,
        startEpochDay: Long,
        fromEpochDay: Long,
        toEpochDay: Long,
        exceptions: Set<Long> = emptySet(),
        max: Int = 5000,
    ): List<Long> {
        val result = ArrayList<Long>()
        val rule = try {
            RecurrenceRule(rrule)
        } catch (e: Exception) {
            return result
        }
        val start = LocalDate.ofEpochDay(startEpochDay)
        val dtStart = DateTime(start.year, start.monthValue - 1, start.dayOfMonth)
        val it = rule.iterator(dtStart)
        var count = 0
        while (it.hasNext() && count < max) {
            val dt = it.nextDateTime()
            val ld = LocalDate.of(dt.year, dt.month + 1, dt.dayOfMonth)
            val ep = ld.toEpochDay()
            if (ep > toEpochDay) break
            count++
            if (ep < fromEpochDay) continue
            if (ep in exceptions) continue
            result.add(ep)
        }
        return result
    }

    /** Next occurrence on/after [fromEpochDay], or null. */
    fun nextOccurrence(rrule: String, startEpochDay: Long, fromEpochDay: Long): Long? {
        val far = fromEpochDay + 366L * 20 // look ahead up to 20 years
        return occurrencesBetween(rrule, startEpochDay, fromEpochDay, far, max = 1)
            .firstOrNull()
    }

    fun isValid(rrule: String): Boolean = try {
        RecurrenceRule(rrule); true
    } catch (e: Exception) {
        false
    }

    /** Build a simple RRULE for the common presets. */
    fun build(freq: String, interval: Int): String =
        "FREQ=$freq;INTERVAL=${interval.coerceAtLeast(1)}"

    val PRESETS = listOf(
        "Daily" to "FREQ=DAILY;INTERVAL=1",
        "Weekly" to "FREQ=WEEKLY;INTERVAL=1",
        "Every 2 weeks" to "FREQ=WEEKLY;INTERVAL=2",
        "Monthly" to "FREQ=MONTHLY;INTERVAL=1",
        "Yearly" to "FREQ=YEARLY;INTERVAL=1",
    )

    fun describe(rrule: String): String =
        PRESETS.firstOrNull { it.second == rrule }?.first ?: rrule
}
