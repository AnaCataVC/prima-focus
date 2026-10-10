package com.ancata.prima_focus.core

import com.ancata.prima_focus.core.utils.LongPendingCelebrationCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cleanroom Black-Box Unit Test Suite for [LongPendingCelebrationCalculator].
 *
 * Formulated strictly and exclusively from formal contract specifications:
 * `docs/contracts/long-pending-task-celebration.contract.md`
 */
class LongPendingCelebrationCalculatorTest {

    private companion object {
        const val ONE_HOUR_MS = 3_600_000L
        const val ONE_DAY_MS = 86_400_000L // 24 * 60 * 60 * 1000L
    }

    // =========================================================================
    // 1. calculatePendingDays Tests
    // =========================================================================

    @Test
    fun calculatePendingDays_whenNowEqualsCreatedAt_returnsZero() {
        val timestamp = 1_700_000_000_000L
        val days = LongPendingCelebrationCalculator.calculatePendingDays(
            createdAtEpochMs = timestamp,
            nowEpochMs = timestamp
        )
        assertEquals(0, days)
    }

    @Test
    fun calculatePendingDays_whenNowIsBeforeCreatedAt_returnsZero() {
        val createdAt = 1_700_000_000_000L
        val now = createdAt - (5 * ONE_DAY_MS)
        val days = LongPendingCelebrationCalculator.calculatePendingDays(
            createdAtEpochMs = createdAt,
            nowEpochMs = now
        )
        assertEquals(0, days)
    }

    @Test
    fun calculatePendingDays_whenIncompleteDayElapsed_returnsZero() {
        val createdAt = 1_700_000_000_000L
        // 12 hours elapsed (incomplete day)
        val now12h = createdAt + (12 * ONE_HOUR_MS)
        assertEquals(0, LongPendingCelebrationCalculator.calculatePendingDays(createdAt, now12h))

        // 23 hours, 59 minutes, 59 seconds, 999 ms
        val nowAlmostOneDay = createdAt + ONE_DAY_MS - 1L
        assertEquals(0, LongPendingCelebrationCalculator.calculatePendingDays(createdAt, nowAlmostOneDay))
    }

    @Test
    fun calculatePendingDays_whenExactlyOneDayElapsed_returnsOne() {
        val createdAt = 1_700_000_000_000L
        val now = createdAt + ONE_DAY_MS
        assertEquals(1, LongPendingCelebrationCalculator.calculatePendingDays(createdAt, now))
    }

    @Test
    fun calculatePendingDays_whenMultipleDaysElapsed_returnsWholeDays() {
        val createdAt = 1_700_000_000_000L

        // 14 days
        val now14d = createdAt + (14 * ONE_DAY_MS)
        assertEquals(14, LongPendingCelebrationCalculator.calculatePendingDays(createdAt, now14d))

        // 30 days exactly
        val now30d = createdAt + (30 * ONE_DAY_MS)
        assertEquals(30, LongPendingCelebrationCalculator.calculatePendingDays(createdAt, now30d))

        // 30 days and 23 hours (should truncate to 30)
        val now30dPlusHours = createdAt + (30 * ONE_DAY_MS) + (23 * ONE_HOUR_MS)
        assertEquals(30, LongPendingCelebrationCalculator.calculatePendingDays(createdAt, now30dPlusHours))

        // 365 days
        val now365d = createdAt + (365 * ONE_DAY_MS)
        assertEquals(365, LongPendingCelebrationCalculator.calculatePendingDays(createdAt, now365d))
    }

    @Test
    fun calculatePendingDays_safeAgainstLongArithmeticOverflow() {
        val createdAt = 1_700_000_000_000L
        val largeDays = 1_000L
        val now = createdAt + (largeDays * ONE_DAY_MS)
        assertEquals(1000, LongPendingCelebrationCalculator.calculatePendingDays(createdAt, now))
    }

    // =========================================================================
    // 2. formatPendingDuration Tests
    // =========================================================================

    @Test
    fun formatPendingDuration_whenZeroOrNegative_returnsZeroDias() {
        assertEquals("0 días", LongPendingCelebrationCalculator.formatPendingDuration(0))
        assertEquals("0 días", LongPendingCelebrationCalculator.formatPendingDuration(-1))
        assertEquals("0 días", LongPendingCelebrationCalculator.formatPendingDuration(-50))
    }

    @Test
    fun formatPendingDuration_whenOneDay_returnsOneDiaSingular() {
        assertEquals("1 día", LongPendingCelebrationCalculator.formatPendingDuration(1))
    }

    @Test
    fun formatPendingDuration_whenBetween2And29Days_returnsDaysPlural() {
        assertEquals("2 días", LongPendingCelebrationCalculator.formatPendingDuration(2))
        assertEquals("14 días", LongPendingCelebrationCalculator.formatPendingDuration(14))
        assertEquals("28 días", LongPendingCelebrationCalculator.formatPendingDuration(28))
        assertEquals("29 días", LongPendingCelebrationCalculator.formatPendingDuration(29))
    }

    @Test
    fun formatPendingDuration_whenBetween30And31Days_returnsUnMes() {
        assertEquals("1 mes", LongPendingCelebrationCalculator.formatPendingDuration(30))
        assertEquals("1 mes", LongPendingCelebrationCalculator.formatPendingDuration(31))
    }

    @Test
    fun formatPendingDuration_whenBetween32And59Days_returnsDaysPlural() {
        assertEquals("32 días", LongPendingCelebrationCalculator.formatPendingDuration(32))
        assertEquals("45 días", LongPendingCelebrationCalculator.formatPendingDuration(45))
        assertEquals("55 días", LongPendingCelebrationCalculator.formatPendingDuration(55))
        assertEquals("59 días", LongPendingCelebrationCalculator.formatPendingDuration(59))
    }

    @Test
    fun formatPendingDuration_whenBetween60And364Days_returnsMonthsCalculatedAsDaysDividedBy30() {
        assertEquals("2 meses", LongPendingCelebrationCalculator.formatPendingDuration(60))
        assertEquals("2 meses", LongPendingCelebrationCalculator.formatPendingDuration(89))
        assertEquals("3 meses", LongPendingCelebrationCalculator.formatPendingDuration(90))
        assertEquals("3 meses", LongPendingCelebrationCalculator.formatPendingDuration(119))
        assertEquals("4 meses", LongPendingCelebrationCalculator.formatPendingDuration(120))
        assertEquals("6 meses", LongPendingCelebrationCalculator.formatPendingDuration(180))
        assertEquals("12 meses", LongPendingCelebrationCalculator.formatPendingDuration(360))
        assertEquals("12 meses", LongPendingCelebrationCalculator.formatPendingDuration(364))
    }

    @Test
    fun formatPendingDuration_whenBetween365And729Days_returnsUnAno() {
        assertEquals("1 año", LongPendingCelebrationCalculator.formatPendingDuration(365))
        assertEquals("1 año", LongPendingCelebrationCalculator.formatPendingDuration(366))
        assertEquals("1 año", LongPendingCelebrationCalculator.formatPendingDuration(500))
        assertEquals("1 año", LongPendingCelebrationCalculator.formatPendingDuration(729))
    }

    @Test
    fun formatPendingDuration_when730DaysOrMore_returnsYearsCalculatedAsDaysDividedBy365() {
        assertEquals("2 años", LongPendingCelebrationCalculator.formatPendingDuration(730))
        assertEquals("2 años", LongPendingCelebrationCalculator.formatPendingDuration(731))
        assertEquals("2 años", LongPendingCelebrationCalculator.formatPendingDuration(1094))
        assertEquals("3 años", LongPendingCelebrationCalculator.formatPendingDuration(1095))
        assertEquals("4 años", LongPendingCelebrationCalculator.formatPendingDuration(1460))
    }

    // =========================================================================
    // 3. shouldTriggerCelebration Tests
    // =========================================================================

    @Test
    fun shouldTriggerCelebration_whenAllConditionsSatisfied_returnsTrue() {
        val createdAt = 1_700_000_000_000L
        val threshold = 14
        val now = createdAt + (14 * ONE_DAY_MS) // exactly at threshold

        val result = LongPendingCelebrationCalculator.shouldTriggerCelebration(
            isRecurring = false,
            createdAtEpochMs = createdAt,
            nowEpochMs = now,
            thresholdDays = threshold,
            isEnabled = true
        )
        assertTrue(result)
    }

    @Test
    fun shouldTriggerCelebration_whenElapsedExceedsThreshold_returnsTrue() {
        val createdAt = 1_700_000_000_000L
        val threshold = 30
        val now = createdAt + (35 * ONE_DAY_MS) // 35 days elapsed > 30

        val result = LongPendingCelebrationCalculator.shouldTriggerCelebration(
            isRecurring = false,
            createdAtEpochMs = createdAt,
            nowEpochMs = now,
            thresholdDays = threshold,
            isEnabled = true
        )
        assertTrue(result)
    }

    @Test
    fun shouldTriggerCelebration_whenElapsedIsBelowThreshold_returnsFalse() {
        val createdAt = 1_700_000_000_000L
        val threshold = 30
        val now = createdAt + (29 * ONE_DAY_MS) // 29 days elapsed < 30

        val result = LongPendingCelebrationCalculator.shouldTriggerCelebration(
            isRecurring = false,
            createdAtEpochMs = createdAt,
            nowEpochMs = now,
            thresholdDays = threshold,
            isEnabled = true
        )
        assertFalse(result)
    }

    @Test
    fun shouldTriggerCelebration_whenElapsedIsOneMillisecondShortOfThreshold_returnsFalse() {
        val createdAt = 1_700_000_000_000L
        val threshold = 14
        val now = createdAt + (14 * ONE_DAY_MS) - 1L

        val result = LongPendingCelebrationCalculator.shouldTriggerCelebration(
            isRecurring = false,
            createdAtEpochMs = createdAt,
            nowEpochMs = now,
            thresholdDays = threshold,
            isEnabled = true
        )
        assertFalse(result)
    }

    @Test
    fun shouldTriggerCelebration_whenTaskIsRecurring_neverTriggersEvenIfOverThreshold() {
        val createdAt = 1_700_000_000_000L
        val now = createdAt + (100 * ONE_DAY_MS)

        val result = LongPendingCelebrationCalculator.shouldTriggerCelebration(
            isRecurring = true,
            createdAtEpochMs = createdAt,
            nowEpochMs = now,
            thresholdDays = 14,
            isEnabled = true
        )
        assertFalse("Recurring tasks must NEVER trigger celebration", result)
    }

    @Test
    fun shouldTriggerCelebration_whenFeatureIsDisabled_neverTriggers() {
        val createdAt = 1_700_000_000_000L
        val now = createdAt + (100 * ONE_DAY_MS)

        val result = LongPendingCelebrationCalculator.shouldTriggerCelebration(
            isRecurring = false,
            createdAtEpochMs = createdAt,
            nowEpochMs = now,
            thresholdDays = 14,
            isEnabled = false
        )
        assertFalse("Disabled celebration setting must NEVER trigger celebration", result)
    }

    @Test
    fun shouldTriggerCelebration_whenThresholdDaysIsZeroOrNegative_neverTriggers() {
        val createdAt = 1_700_000_000_000L
        val now = createdAt + (50 * ONE_DAY_MS)

        val resultZero = LongPendingCelebrationCalculator.shouldTriggerCelebration(
            isRecurring = false,
            createdAtEpochMs = createdAt,
            nowEpochMs = now,
            thresholdDays = 0,
            isEnabled = true
        )
        assertFalse("thresholdDays <= 0 must return false", resultZero)

        val resultNegative = LongPendingCelebrationCalculator.shouldTriggerCelebration(
            isRecurring = false,
            createdAtEpochMs = createdAt,
            nowEpochMs = now,
            thresholdDays = -5,
            isEnabled = true
        )
        assertFalse("Negative thresholdDays must return false", resultNegative)
    }

    @Test
    fun shouldTriggerCelebration_whenNowIsBeforeCreatedAt_returnsFalse() {
        val createdAt = 1_700_000_000_000L
        val now = createdAt - (10 * ONE_DAY_MS)

        val result = LongPendingCelebrationCalculator.shouldTriggerCelebration(
            isRecurring = false,
            createdAtEpochMs = createdAt,
            nowEpochMs = now,
            thresholdDays = 14,
            isEnabled = true
        )
        assertFalse(result)
    }
}
