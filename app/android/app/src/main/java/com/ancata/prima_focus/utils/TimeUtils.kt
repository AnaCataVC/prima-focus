package com.ancata.prima_focus.utils

import com.ancata.prima_focus.core.utils.SharedTimeUtils
import java.time.LocalDate

/**
 * Delegating Time utilities to the core SharedTimeUtils.
 */
object TimeUtils {

    fun isCurrentlyInQuietHours(start: String, end: String): Boolean =
        SharedTimeUtils.isCurrentlyInQuietHours(start, end)

    fun getMinutesUntilQuietHoursEnd(start: String, end: String): Long =
        SharedTimeUtils.getMinutesUntilQuietHoursEnd(start, end)

    fun formatEpochToDisplay(epochMillis: Long): String =
        SharedTimeUtils.formatEpochToDisplay(epochMillis)

    fun isFutureScheduled(dateStr: String?, today: LocalDate = LocalDate.now()): Boolean =
        SharedTimeUtils.isFutureScheduled(dateStr, today)
}


