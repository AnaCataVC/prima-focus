package com.ancata.prima_focus.utils

import com.ancata.prima_focus.core.utils.SharedRecurrenceCalculator
import java.time.LocalDate

/**
 * Delegating parser for Prima-Focus recurrence rules to the core SharedRecurrenceCalculator.
 */
object RecurrenceCalculator {

    fun toLabel(rule: String?): String? = SharedRecurrenceCalculator.toLabel(rule)

    fun computeNextDate(rule: String, fromDate: LocalDate): LocalDate? =
        SharedRecurrenceCalculator.computeNextDate(rule, fromDate)
}

