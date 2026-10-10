# Contract: Long-Pending Task Celebration Logic

## 1. Specification & Package
- **Package**: `com.ancata.prima_focus.core.utils`
- **Component**: `LongPendingCelebrationCalculator` (`object`)
- **Location**: `app/android/shared/src/main/kotlin/com/ancata/prima_focus/core/utils/LongPendingCelebrationCalculator.kt`
- **Test Location**: `app/android/shared/src/test/kotlin/com/ancata/prima_focus/core/LongPendingCelebrationCalculatorTest.kt`

---

## 2. Public Interface & Signatures

```kotlin
package com.ancata.prima_focus.core.utils

object LongPendingCelebrationCalculator {

    /**
     * Calculates the whole number of 24-hour days elapsed between [createdAtEpochMs] and [nowEpochMs].
     * If [nowEpochMs] is less than or equal to [createdAtEpochMs], returns 0.
     */
    fun calculatePendingDays(createdAtEpochMs: Long, nowEpochMs: Long): Int

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
    fun formatPendingDuration(days: Int): String

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
    ): Boolean
}
```

---

## 3. Behavioral Acceptance Criteria (Given-When-Then)

### Scenario 1: Calculate pending days
- **Given** a creation timestamp `createdAt` and current time `now`
- **When** `now` is exactly 30 days after `createdAt` ($30 \times 86,400,000$ ms)
- **Then** `calculatePendingDays` returns `30`
- **When** `now` is less than `createdAt` (negative elapsed time)
- **Then** `calculatePendingDays` returns `0`
- **When** `now - createdAt` is 12 hours ($12 \times 3,600,000$ ms)
- **Then** `calculatePendingDays` returns `0` (incomplete day)

### Scenario 2: Duration formatting in Spanish
- **Given** integer day counts
- **When** `days = 0` $\to$ Returns `"0 días"`
- **When** `days = 1` $\to$ Returns `"1 día"`
- **When** `days = 14` $\to$ Returns `"14 días"`
- **When** `days = 30` or `31` $\to$ Returns `"1 mes"`
- **When** `days = 45` $\to$ Returns `"45 días"`
- **When** `days = 60` $\to$ Returns `"2 meses"`
- **When** `days = 90` $\to$ Returns `"3 meses"`
- **When** `days = 365` $\to$ Returns `"1 año"`
- **When** `days = 730` $\to$ Returns `"2 años"`

### Scenario 3: Celebration trigger evaluation
- **Given** `isEnabled = true`, `isRecurring = false`, `thresholdDays = 30`
- **When** task was created 35 days ago
- **Then** `shouldTriggerCelebration` returns `true`
- **When** task was created 29 days ago
- **Then** `shouldTriggerCelebration` returns `false`
- **When** `isRecurring = true` (even if created 100 days ago)
- **Then** `shouldTriggerCelebration` returns `false`
- **When** `isEnabled = false` (even if created 100 days ago)
- **Then** `shouldTriggerCelebration` returns `false`
- **When** `thresholdDays <= 0`
- **Then** `shouldTriggerCelebration` returns `false`

---

## 4. Boundary Values & Edge Cases
- `nowEpochMs <= createdAtEpochMs` $\to$ `calculatePendingDays` must return `0`, no exception.
- `days < 0` $\to$ `formatPendingDuration` must return `"0 días"`.
- `thresholdDays <= 0` $\to$ `shouldTriggerCelebration` must return `false`.
- Long overflow safety: operations using `(now - createdAt) / (1000L * 60 * 60 * 24)` must use Long arithmetic before converting to `Int`.
