package com.ancata.prima_focus.core.utils

/**
 * Utility calculator for evaluating and formatting celebrations for tasks
 * that have been pending for a long duration before being completed.
 */
object LongPendingCelebrationCalculator {

    private const val MILLIS_PER_DAY = 86_400_000L

    /**
     * Calculates the whole number of 24-hour days elapsed between [createdAtEpochMs] and [nowEpochMs].
     * If [nowEpochMs] is less than or equal to [createdAtEpochMs], returns 0.
     */
    fun calculatePendingDays(createdAtEpochMs: Long, nowEpochMs: Long): Int {
        if (nowEpochMs <= createdAtEpochMs) {
            return 0
        }
        val elapsedMs = nowEpochMs - createdAtEpochMs
        val days = elapsedMs / MILLIS_PER_DAY
        return days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    /**
     * Formats the elapsed [days] into an idiomatic Spanish duration string.
     * Rules:
     * - days <= 0 -> "0 días"
     * - days == 1 -> "1 día"
     * - days in 2..29 -> "$days días" (e.g., "14 días", "28 días")
     * - days in 30..31 -> "1 mes"
     * - days in 32..59 -> "$days días" (e.g., "45 días", "55 días")
     * - days in 60..364 -> "${days / 30} meses" (e.g., 60 -> "2 meses", 90 -> "3 meses")
     * - days in 365..729 -> "1 año"
     * - days >= 730 -> "${days / 365} años" (e.g., 730 -> "2 años")
     */
    fun formatPendingDuration(days: Int): String {
        return when {
            days <= 0 -> "0 días"
            days == 1 -> "1 día"
            days in 2..29 -> "$days días"
            days in 30..31 -> "1 mes"
            days in 32..59 -> "$days días"
            days in 60..364 -> "${days / 30} meses"
            days in 365..729 -> "1 año"
            else -> "${days / 365} años"
        }
    }

    /**
     * Evaluates whether a completed task should trigger a celebration based on user settings,
     * task recurrence, and elapsed time against the threshold.
     *
     * @param isRecurring Whether the task is recurring (e.g., recurrence != null). Recurring tasks must NEVER celebrate.
     * @param createdAtEpochMs The creation timestamp in epoch milliseconds.
     * @param nowEpochMs The current timestamp in epoch milliseconds.
     * @param thresholdDays The configured threshold in whole days (e.g., 14, 30, 60, 90).
     * @param isEnabled Whether the celebration feature toggle is turned on in preferences.
     *
     * @return true if and only if:
     *   - [isEnabled] is true
     *   - [isRecurring] is false
     *   - [thresholdDays] > 0
     *   - [calculatePendingDays(createdAtEpochMs, nowEpochMs)] >= [thresholdDays]
     */
    fun shouldTriggerCelebration(
        isRecurring: Boolean,
        createdAtEpochMs: Long,
        nowEpochMs: Long,
        thresholdDays: Int,
        isEnabled: Boolean
    ): Boolean {
        if (!isEnabled || isRecurring || thresholdDays <= 0) {
            return false
        }
        val pendingDays = calculatePendingDays(createdAtEpochMs, nowEpochMs)
        return pendingDays >= thresholdDays
    }
}
