package com.ancata.prima_focus.utils

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Artisanal parser for Prima-Focus MVP recurrence rules.
 *
 * Supported formats:
 *   - "DAILY"                  -> repeats every day
 *   - "WEEKLY:MO,WE,FR"        -> repeats on specified weekdays (2-letter codes)
 *   - "MONTHLY"                -> repeats on the same day of the month
 *
 * The next occurrence is always calculated FROM the base date of the
 * completed task (not from today), so the series rhythm is preserved
 * even if the user completes the task late.
 */
object RecurrenceCalculator {

    /**
     * Day-of-week abbreviation map (RFC5545-style 2-letter codes -> DayOfWeek).
     */
    private val DAY_MAP = mapOf(
        "MO" to DayOfWeek.MONDAY,
        "TU" to DayOfWeek.TUESDAY,
        "WE" to DayOfWeek.WEDNESDAY,
        "TH" to DayOfWeek.THURSDAY,
        "FR" to DayOfWeek.FRIDAY,
        "SA" to DayOfWeek.SATURDAY,
        "SU" to DayOfWeek.SUNDAY
    )

    /**
     * Human-readable Spanish label for display in the UI.
     */
    fun toLabel(rule: String?): String? = when {
        rule == null -> null
        rule == "DAILY" -> "Diario"
        rule == "MONTHLY" -> "Mensual"
        rule.startsWith("WEEKLY:") -> {
            val days = rule.removePrefix("WEEKLY:")
                .split(",")
                .mapNotNull { toSpanishAbbrev(it.trim()) }
            "Semanal: ${days.joinToString(", ")}"
        }
        else -> null
    }

    /**
     * Computes the next occurrence date for [rule] starting strictly AFTER [fromDate].
     *
     * Returns null if the rule is unrecognized or cannot be parsed.
     */
    fun computeNextDate(rule: String, fromDate: LocalDate): LocalDate? {
        return when {
            rule == "DAILY" -> fromDate.plusDays(1)
            rule == "MONTHLY" -> fromDate.plusMonths(1)
            rule.startsWith("WEEKLY:") -> computeNextWeeklyDate(rule, fromDate)
            else -> null
        }
    }

    // Private helpers

    /**
     * For WEEKLY rules, finds the next weekday in [rule]s BYDAY list that
     * falls strictly after [fromDate]. Searches up to 14 days ahead to handle
     * the wrap-around from one week to the next.
     */
    private fun computeNextWeeklyDate(rule: String, fromDate: LocalDate): LocalDate? {
        val dayCodes = rule.removePrefix("WEEKLY:").split(",").map { it.trim() }
        val targetDays = dayCodes.mapNotNull { DAY_MAP[it] }.toSet()
        if (targetDays.isEmpty()) return null

        // Search the next 14 days (covers at most 2 full weeks)
        for (offset in 1..14) {
            val candidate = fromDate.plusDays(offset.toLong())
            if (candidate.dayOfWeek in targetDays) return candidate
        }
        return null
    }

    private fun toSpanishAbbrev(code: String): String? = when (code) {
        "MO" -> "Lun"
        "TU" -> "Mar"
        "WE" -> "Mie"
        "TH" -> "Jue"
        "FR" -> "Vie"
        "SA" -> "Sab"
        "SU" -> "Dom"
        else -> null
    }
}
