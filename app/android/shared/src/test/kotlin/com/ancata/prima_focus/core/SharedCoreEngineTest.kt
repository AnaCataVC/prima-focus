package com.ancata.prima_focus.core

import com.ancata.prima_focus.core.engine.SharedPriorityEngine
import com.ancata.prima_focus.core.model.Session
import com.ancata.prima_focus.core.model.Task
import com.ancata.prima_focus.core.sync.LANAuthSecurity
import com.ancata.prima_focus.core.sync.MergeAction
import com.ancata.prima_focus.core.sync.SyncMergeEngine
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
}
