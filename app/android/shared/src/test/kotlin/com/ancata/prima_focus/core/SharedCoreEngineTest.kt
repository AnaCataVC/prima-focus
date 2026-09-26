package com.ancata.prima_focus.core

import com.ancata.prima_focus.core.engine.SharedPriorityEngine
import com.ancata.prima_focus.core.model.Session
import com.ancata.prima_focus.core.model.Task
import com.ancata.prima_focus.core.sync.LANAuthSecurity
import com.ancata.prima_focus.core.sync.MergeAction
import com.ancata.prima_focus.core.sync.SyncMergeEngine
import com.ancata.prima_focus.core.sync.SyncSettings
import com.ancata.prima_focus.core.utils.SharedRecurrenceCalculator
import com.ancata.prima_focus.core.utils.SharedTimeUtils
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class SharedCoreEngineTest {

    private val engine = SharedPriorityEngine()

    @Test
    fun calculatePriority_boostAndDemote_workSymmetrically() {
        val now = System.currentTimeMillis()
        val base = Task(
            taskId = "t-1",
            title = "Test Task",
            category = "trabajo",
            categoryWeight = 3.0,
            createdAt = now,
            updatedAt = now
        )

        val normal = engine.calculatePriority(base, now)
        val boosted = engine.calculatePriority(base.copy(manualBoost = 15.0), now)
        val demoted = engine.calculatePriority(base.copy(manualBoost = -15.0), now)

        assertEquals(15.0, boosted.priorityScore - normal.priorityScore, 0.001)
        assertEquals(-15.0, demoted.priorityScore - normal.priorityScore, 0.001)
    }

    @Test
    fun syncMerge_completedStatusIsNonRegressive() {
        val now = System.currentTimeMillis()
        val localCompleted = Task(
            taskId = "t-comp",
            title = "Finished Task",
            category = "trabajo",
            categoryWeight = 2.0,
            status = "completed",
            createdAt = now - 50000,
            updatedAt = now,
            syncVersion = 2L
        )

        val remoteStalePending = localCompleted.copy(
            status = "pending",
            updatedAt = now + 100000, // Clock was ahead on remote
            syncVersion = 1L
        )

        val result = SyncMergeEngine.resolveTaskConflict(localCompleted, remoteStalePending)
        assertTrue(result is MergeAction.Update)
        val updated = (result as MergeAction.Update).item
        assertEquals("completed", updated.status)
        assertTrue(updated.syncVersion >= 3L)
    }

    @Test
    fun syncMerge_higherSyncVersionBeatsOlderRegardlessOfClockDrift() {
        val now = System.currentTimeMillis()
        val local = Task(
            taskId = "t-version",
            title = "Old Local",
            category = "casa",
            categoryWeight = 1.0,
            createdAt = now - 50000,
            updatedAt = now + 20000, // Clock ahead
            syncVersion = 1L
        )

        val remote = local.copy(
            title = "New Remote",
            updatedAt = now - 10000, // Clock behind
            syncVersion = 2L
        )

        val result = SyncMergeEngine.resolveTaskConflict(local, remote)
        assertTrue(result is MergeAction.Update)
        assertEquals("New Remote", (result as MergeAction.Update).item.title)
    }

    @Test
    fun lanAuthSecurity_hmacSignAndVerify() {
        val payload = """{"tasks":[{"taskId":"123","title":"Security Test"}]}"""
        val secretKey = "shared_lan_secret_pin_key_123456"

        val signature = LANAuthSecurity.signPayload(payload, secretKey)
        assertNotNull(signature)
        assertTrue(signature.isNotEmpty())

        val isValid = LANAuthSecurity.verifyPayloadSignature(payload, secretKey, signature)
        assertTrue(isValid)

        val isTamperedValid = LANAuthSecurity.verifyPayloadSignature(
            payload.replace("Security Test", "Tampered Test"),
            secretKey,
            signature
        )
        assertFalse(isTamperedValid)
    }

    @Test
    fun recurrenceCalculator_computesNextDate() {
        val baseDate = LocalDate.of(2026, 8, 20) // Thursday
        val nextDaily = SharedRecurrenceCalculator.computeNextDate("DAILY", baseDate)
        assertEquals(LocalDate.of(2026, 8, 21), nextDaily)

        val nextWeekly = SharedRecurrenceCalculator.computeNextDate("WEEKLY:MO,FR", baseDate)
        assertEquals(LocalDate.of(2026, 8, 21), nextWeekly) // Next is Friday
    }

    @Test
    fun recurrenceCalculator_skipMissed_neverReturnsOverdueDate() {
        val today = LocalDate.of(2026, 9, 25) // Friday
        val threeDaysAgo = today.minusDays(3)

        assertEquals(today, SharedRecurrenceCalculator.computeNextDate("DAILY", threeDaysAgo, today, skipMissed = true))
        assertEquals(threeDaysAgo.plusDays(1), SharedRecurrenceCalculator.computeNextDate("DAILY", threeDaysAgo, today, skipMissed = false))
        assertEquals(today.plusDays(1), SharedRecurrenceCalculator.computeNextDate("DAILY", today, today, skipMissed = true))
    }

    @Test
    fun recurrenceCalculator_skipMissed_weeklyPicksNextMatchingWeekdayOnOrAfterToday() {
        val today = LocalDate.of(2026, 9, 25) // Friday
        val twoWeeksAgoMonday = LocalDate.of(2026, 9, 7)

        val next = SharedRecurrenceCalculator.computeNextDate("WEEKLY:MO,FR", twoWeeksAgoMonday, today, skipMissed = true)
        assertEquals(today, next)

        val nextAfterToday = SharedRecurrenceCalculator.computeNextDate("WEEKLY:MO", twoWeeksAgoMonday, today, skipMissed = true)
        assertEquals(LocalDate.of(2026, 9, 28), nextAfterToday)
    }

    @Test
    fun recurrenceCalculator_instanceId_isStablePerSeriesAndDate() {
        val date = LocalDate.of(2026, 9, 25)
        assertEquals(
            SharedRecurrenceCalculator.instanceId("group-1", date),
            SharedRecurrenceCalculator.instanceId("group-1", date)
        )
        assertNotEquals(
            SharedRecurrenceCalculator.instanceId("group-1", date),
            SharedRecurrenceCalculator.instanceId("group-1", date.plusDays(1))
        )
    }

    @Test
    fun syncMerge_fullTie_bothDevicesPickTheSameWinner() {
        val base = Task(
            taskId = "t-tie",
            title = "Phone edit",
            category = "casa",
            categoryWeight = 1.0,
            createdAt = 1000L,
            updatedAt = 5000L,
            syncVersion = 3L
        )
        val phone = base
        val tablet = base.copy(title = "Tablet edit")

        val onPhone = SyncMergeEngine.resolveTaskConflict(local = phone, remote = tablet)
        val onTablet = SyncMergeEngine.resolveTaskConflict(local = tablet, remote = phone)

        val phoneResult = if (onPhone is MergeAction.Update) onPhone.item else phone
        val tabletResult = if (onTablet is MergeAction.Update) onTablet.item else tablet
        assertEquals(phoneResult.title, tabletResult.title)
        // Exactly one side adopts the other's version.
        assertTrue((onPhone is MergeAction.Update) xor (onTablet is MergeAction.Update))
    }

    @Test
    fun syncMerge_identicalContentIsKeptLocally() {
        val task = Task(taskId = "t-same", title = "Same", category = "casa", categoryWeight = 1.0, createdAt = 1L, updatedAt = 2L)
        assertEquals(MergeAction.KeepLocal, SyncMergeEngine.resolveTaskConflict(task, task.copy(priorityScore = 42.0)))
    }

    @Test
    fun syncMerge_findRecurringDuplicates_keepsHighestVersion() {
        val a = Task(
            taskId = "a", title = "Med", category = "salud", categoryWeight = 4.0, date = "2026-09-26",
            recurrenceGroupId = "g", createdAt = 1L, updatedAt = 10L, syncVersion = 1L
        )
        val b = a.copy(taskId = "b", syncVersion = 2L)
        val otherDate = a.copy(taskId = "c", date = "2026-09-27")
        val completed = a.copy(taskId = "d", status = "completed")

        val losers = SyncMergeEngine.findRecurringDuplicates(listOf(a, b, otherDate, completed))
        assertEquals(listOf("a"), losers.map { it.taskId })
    }

    @Test
    fun syncMerge_resolveSettings_lastWriteWinsWithDeviceIdTiebreak() {
        val local = SyncSettings(mapOf("casa" to "A"), skipMissedOccurrences = true, updatedAt = 100L)
        val newer = SyncSettings(mapOf("casa" to "B"), skipMissedOccurrences = false, updatedAt = 200L)
        val older = newer.copy(updatedAt = 50L)
        val tied = newer.copy(updatedAt = 100L)

        assertEquals(newer, SyncMergeEngine.resolveSettings(local, "dev-a", newer, "dev-b"))
        assertNull(SyncMergeEngine.resolveSettings(local, "dev-a", older, "dev-b"))
        assertNull(SyncMergeEngine.resolveSettings(local, "dev-a", null, null))
        // Tie: the higher device id wins on both sides.
        assertEquals(tied, SyncMergeEngine.resolveSettings(local, "dev-a", tied, "dev-b"))
        assertNull(SyncMergeEngine.resolveSettings(tied, "dev-b", local, "dev-a"))
    }
}
