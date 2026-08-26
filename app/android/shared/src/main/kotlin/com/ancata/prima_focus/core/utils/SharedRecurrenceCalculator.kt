package com.ancata.prima_focus.core.utils

import java.time.DayOfWeek
import java.time.LocalDate

object SharedRecurrenceCalculator {

    private val DAY_MAP = mapOf(
        "MO" to DayOfWeek.MONDAY,
        "TU" to DayOfWeek.TUESDAY,
        "WE" to DayOfWeek.WEDNESDAY,
        "TH" to DayOfWeek.THURSDAY,
        "FR" to DayOfWeek.FRIDAY,
        "SA" to DayOfWeek.SATURDAY,
        "SU" to DayOfWeek.SUNDAY
    )

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

    fun computeNextDate(rule: String, fromDate: LocalDate): LocalDate? {
        return when {
            rule == "DAILY" -> fromDate.plusDays(1)
            rule == "MONTHLY" -> fromDate.plusMonths(1)
            rule.startsWith("WEEKLY:") -> computeNextWeeklyDate(rule, fromDate)
            else -> null
        }
    }

    private fun computeNextWeeklyDate(rule: String, fromDate: LocalDate): LocalDate? {
        val dayCodes = rule.removePrefix("WEEKLY:").split(",").map { it.trim() }
        val targetDays = dayCodes.mapNotNull { DAY_MAP[it] }.toSet()
        if (targetDays.isEmpty()) return null

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
