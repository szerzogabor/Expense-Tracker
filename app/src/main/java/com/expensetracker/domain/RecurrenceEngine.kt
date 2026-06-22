package com.expensetracker.domain

import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.recur.RecurrenceRule
import java.time.LocalDate

/**
 * RFC-5545 RRULE expansion against date-only occurrences.
 *
 * The common presets (FREQ=DAILY/WEEKLY/MONTHLY/YEARLY with optional
 * INTERVAL/COUNT/UNTIL) are expanded with pure java.time — this is the path the
 * UI generates and it never touches lib-recur, so it can't fail at runtime.
 * Anything more exotic falls back to lib-recur, fully guarded so a parsing or
 * iteration failure can never crash the app.
 */
object RecurrenceEngine {

    private data class Simple(
        val freq: String,
        val interval: Int,
        val count: Int?,
        val until: Long?,
    )

    /** Returns occurrence epoch days in [fromEpochDay, toEpochDay], skipping exceptions. */
    fun occurrencesBetween(
        rrule: String,
        startEpochDay: Long,
        fromEpochDay: Long,
        toEpochDay: Long,
        exceptions: Set<Long> = emptySet(),
        max: Int = 5000,
    ): List<Long> = try {
        val simple = parseSimple(rrule)
        if (simple != null) {
            expandSimple(simple, startEpochDay, fromEpochDay, toEpochDay, exceptions, max)
        } else {
            expandWithLibRecur(rrule, startEpochDay, fromEpochDay, toEpochDay, exceptions, max)
        }
    } catch (t: Throwable) {
        emptyList()
    }

    private fun expandSimple(
        rule: Simple,
        startEpochDay: Long,
        fromEpochDay: Long,
        toEpochDay: Long,
        exceptions: Set<Long>,
        max: Int,
    ): List<Long> {
        val result = ArrayList<Long>()
        var date = LocalDate.ofEpochDay(startEpochDay)
        var produced = 0
        var iter = 0
        while (iter < max) {
            iter++
            val ep = date.toEpochDay()
            if (ep > toEpochDay) break
            if (rule.until != null && ep > rule.until) break
            if (rule.count != null && produced >= rule.count) break
            produced++
            if (ep >= fromEpochDay && ep !in exceptions) result.add(ep)
            date = when (rule.freq) {
                "DAILY" -> date.plusDays(rule.interval.toLong())
                "WEEKLY" -> date.plusWeeks(rule.interval.toLong())
                "MONTHLY" -> date.plusMonths(rule.interval.toLong())
                "YEARLY" -> date.plusYears(rule.interval.toLong())
                else -> break
            }
        }
        return result
    }

    private fun expandWithLibRecur(
        rrule: String,
        startEpochDay: Long,
        fromEpochDay: Long,
        toEpochDay: Long,
        exceptions: Set<Long>,
        max: Int,
    ): List<Long> {
        val result = ArrayList<Long>()
        val rule = RecurrenceRule(rrule)
        val start = LocalDate.ofEpochDay(startEpochDay)
        val dtStart = DateTime(start.year, start.monthValue - 1, start.dayOfMonth)
        val it = rule.iterator(dtStart)
        var count = 0
        while (it.hasNext() && count < max) {
            val dt = it.nextDateTime()
            val ep = LocalDate.of(dt.year, dt.month + 1, dt.dayOfMonth).toEpochDay()
            if (ep > toEpochDay) break
            count++
            if (ep < fromEpochDay) continue
            if (ep in exceptions) continue
            result.add(ep)
        }
        return result
    }

    /** Parse a simple FREQ/INTERVAL/COUNT/UNTIL rule; null if it needs full RRULE handling. */
    private fun parseSimple(rrule: String): Simple? {
        var freq: String? = null
        var interval = 1
        var count: Int? = null
        var until: Long? = null
        for (part in rrule.trim().split(";")) {
            if (part.isBlank()) continue
            val kv = part.split("=", limit = 2)
            if (kv.size != 2) return null
            val key = kv[0].trim().uppercase()
            val value = kv[1].trim()
            when (key) {
                "FREQ" -> freq = value.uppercase()
                "INTERVAL" -> interval = value.toIntOrNull() ?: return null
                "COUNT" -> count = value.toIntOrNull() ?: return null
                "UNTIL" -> until = parseUntil(value) ?: return null
                "WKST" -> { /* ignored for simple expansion */ }
                else -> return null // BYDAY/BYMONTHDAY/... -> use lib-recur
            }
        }
        if (freq !in setOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")) return null
        return Simple(freq!!, interval.coerceAtLeast(1), count, until)
    }

    /** Parse the date part of an RFC-5545 UNTIL value (YYYYMMDD[...]) to an epoch day. */
    private fun parseUntil(value: String): Long? {
        val digits = value.takeWhile { it.isDigit() }
        if (digits.length < 8) return null
        return try {
            LocalDate.of(
                digits.substring(0, 4).toInt(),
                digits.substring(4, 6).toInt(),
                digits.substring(6, 8).toInt(),
            ).toEpochDay()
        } catch (t: Throwable) {
            null
        }
    }

    /** Next occurrence on/after [fromEpochDay], or null. */
    fun nextOccurrence(rrule: String, startEpochDay: Long, fromEpochDay: Long): Long? {
        val far = fromEpochDay + 366L * 20 // look ahead up to 20 years
        return occurrencesBetween(rrule, startEpochDay, fromEpochDay, far).firstOrNull()
    }

    fun isValid(rrule: String): Boolean {
        if (parseSimple(rrule) != null) return true
        return try {
            RecurrenceRule(rrule); true
        } catch (t: Throwable) {
            false
        }
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
