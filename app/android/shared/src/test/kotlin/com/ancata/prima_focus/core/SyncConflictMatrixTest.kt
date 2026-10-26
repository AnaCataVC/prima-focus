package com.ancata.prima_focus.core

import com.ancata.prima_focus.core.model.Task
import com.ancata.prima_focus.core.sync.LANAuthSecurity
import com.ancata.prima_focus.core.sync.MergeAction
import com.ancata.prima_focus.core.sync.SyncMergeEngine
import com.ancata.prima_focus.core.sync.SyncSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Flagship Distributed State & Conflict Resolution Verification Suite.
 *
 * Explicitly tests and documents the 8 core distributed system guarantees of Prima Focus:
 * 1. Tombstone propagation (delete vs offline edit)
 * 2. Stale edit rejection (monotonic version guarding)
 * 3. Clock drift resilience (logical versions take precedence over wall-clock timestamps)
 * 4. Deterministic full-tie convergence (SHA-256 content hashing without network roundtrips)
 * 5. Non-regressive completion (completed tasks cannot be reopened by stale pending states)
 * 6. Concurrent recurrence deduplication (orphaned recurrence instances are tombstoned)
 * 7. Settings Last-Write-Wins with device ID tie-break
 * 8. Cryptographic HMAC-SHA256 integrity and tamper detection
 */
class SyncConflictMatrixTest {

    @Test
    fun scenario1_offlineEditVsLaterDelete_tombstoneWithHigherVersionWins() {
        val now = 1_700_000_000_000L
        val localActiveTask = Task(
            taskId = "task-tombstone-scenario",
            title = "Task modified offline on Device A",
            category = "trabajo",
            categoryWeight = 3.0,
            createdAt = now - 50_000L,
            updatedAt = now - 10_000L,
            isDeleted = false,
            syncVersion = 2L
        )

        // Device B was online later and deleted the task, producing a tombstone with version 3
        val remoteTombstone = localActiveTask.copy(
            isDeleted = true,
            deletedAt = now,
            updatedAt = now,
            syncVersion = 3L
        )

        val result = SyncMergeEngine.resolveTaskConflict(localActiveTask, remoteTombstone)
        assertTrue("Higher-version tombstone must be applied", result is MergeAction.Update)
        val applied = (result as MergeAction.Update).item
        assertTrue("Item must be marked deleted", applied.isDeleted)
        assertEquals(3L, applied.syncVersion)

        // Symmetrical check: If local already has tombstone v3, and receives stale edit v2 -> KeepLocal
        val reverseResult = SyncMergeEngine.resolveTaskConflict(remoteTombstone, localActiveTask)
        assertEquals("Stale edit must not resurrect tombstone", MergeAction.KeepLocal, reverseResult)
    }

    @Test
    fun scenario2_staleEditRejected_olderSyncVersionPreservesNewerLocal() {
        val now = 1_700_000_000_000L
        val localNewer = Task(
            taskId = "task-stale-edit",
            title = "Up-to-date Local Title",
            category = "estudio",
            categoryWeight = 2.5,
            createdAt = now - 100_000L,
            updatedAt = now,
            syncVersion = 4L
        )

        val remoteStale = localNewer.copy(
            title = "Stale Remote Title from un-synced peer",
            updatedAt = now - 50_000L,
            syncVersion = 3L
        )

        val result = SyncMergeEngine.resolveTaskConflict(localNewer, remoteStale)
        assertEquals("Local newer version must be kept", MergeAction.KeepLocal, result)
    }

    @Test
    fun scenario3_clockDriftResilience_higherVersionWinsEvenIfRemoteClockIsInThePast() {
        val now = 1_700_000_000_000L
        // Device A has severe forward clock skew (+1 hour into the future), but version 1
        val localClockAhead = Task(
            taskId = "task-clock-drift",
            title = "Device A edit with forward-skewed clock",
            category = "casa",
            categoryWeight = 2.0,
            createdAt = now,
            updatedAt = now + 3_600_000L, // 1 hour in the future!
            syncVersion = 1L
        )

        // Device B has backward clock skew (-1 hour into past), but user made a legitimate subsequent edit (version 2)
        val remoteClockBehind = localClockAhead.copy(
            title = "Device B legitimate subsequent edit",
            updatedAt = now - 3_600_000L, // 1 hour in the past!
            syncVersion = 2L
        )

        val result = SyncMergeEngine.resolveTaskConflict(localClockAhead, remoteClockBehind)
        assertTrue("Logical version must override wall-clock drift", result is MergeAction.Update)
        assertEquals("Device B legitimate subsequent edit", (result as MergeAction.Update).item.title)
    }

    @Test
    fun scenario4_fullTie_contentHashDeterministicallyPicksIdenticalWinnerOnBothPeers() {
        val now = 1_700_000_000_000L
        val phoneVersion = Task(
            taskId = "task-tie-test",
            title = "Title entered on Phone",
            category = "trabajo",
            categoryWeight = 3.0,
            createdAt = now,
            updatedAt = now + 1000L,
            syncVersion = 2L
        )

        val tabletVersion = phoneVersion.copy(
            title = "Title entered on Tablet at the exact same millisecond"
        )

        // Device 1 (Phone receives Tablet)
        val onPhone = SyncMergeEngine.resolveTaskConflict(local = phoneVersion, remote = tabletVersion)
        // Device 2 (Tablet receives Phone)
        val onTablet = SyncMergeEngine.resolveTaskConflict(local = tabletVersion, remote = phoneVersion)

        val finalPhoneTitle = if (onPhone is MergeAction.Update) onPhone.item.title else phoneVersion.title
        val finalTabletTitle = if (onTablet is MergeAction.Update) onTablet.item.title else tabletVersion.title

        assertEquals("Both devices must deterministically agree on the identical winning title", finalPhoneTitle, finalTabletTitle)
        // Exactly one side updates, ensuring zero divergence
        assertTrue((onPhone is MergeAction.Update) xor (onTablet is MergeAction.Update))
    }

    @Test
    fun scenario5_nonRegressiveCompletion_completedStateNeverOverwrittenByStalePending() {
        val now = 1_700_000_000_000L
        val localCompleted = Task(
            taskId = "task-completed-guard",
            title = "Crucial Report",
            category = "trabajo",
            categoryWeight = 3.5,
            status = "completed",
            createdAt = now - 50_000L,
            updatedAt = now,
            syncVersion = 2L
        )

        // Stale remote peer with clock drifted ahead claims task is still pending
        val remoteStalePending = localCompleted.copy(
            status = "pending",
            updatedAt = now + 100_000L, // Remote clock skewed into future
            syncVersion = 1L
        )

        val result = SyncMergeEngine.resolveTaskConflict(localCompleted, remoteStalePending)
        assertTrue("Must update version to heal the divergence", result is MergeAction.Update)
        val merged = (result as MergeAction.Update).item
        assertEquals("Completed status must never regress to pending", "completed", merged.status)
        assertTrue("Sync version must be monotonically incremented above highest", merged.syncVersion >= 3L)
    }

    @Test
    fun scenario6_offlineRecurrenceConcurrentGeneration_deduplicatesAndTombstonesStaleCopy() {
        val targetDate = "2026-10-26"
        val groupId = "daily-medication-series"

        // Device A spawned an occurrence while offline
        val instanceA = Task(
            taskId = "inst-device-a",
            title = "Take Daily Vitamin",
            category = "salud",
            categoryWeight = 4.0,
            date = targetDate,
            recurrenceGroupId = groupId,
            status = "pending",
            createdAt = 1000L,
            updatedAt = 2000L,
            syncVersion = 1L
        )

        // Device B spawned an occurrence while offline, but subsequently updated it to version 2
        val instanceB = instanceA.copy(
            taskId = "inst-device-b",
            updatedAt = 3000L,
            syncVersion = 2L
        )

        val duplicatesToTombstone = SyncMergeEngine.findRecurringDuplicates(listOf(instanceA, instanceB))
        assertEquals(1, duplicatesToTombstone.size)
        assertEquals("inst-device-a", duplicatesToTombstone.first().taskId)
    }

    @Test
    fun scenario7_settingsLWW_withStableDeviceIdTieBreak() {
        val localSettings = SyncSettings(
            categoryEmojis = mapOf("trabajo" to "💼"),
            skipMissedOccurrences = true,
            updatedAt = 1000L
        )

        val newerRemote = SyncSettings(
            categoryEmojis = mapOf("trabajo" to "⚡"),
            skipMissedOccurrences = false,
            updatedAt = 2000L
        )

        // Newer timestamp wins
        val applied = SyncMergeEngine.resolveSettings(localSettings, "dev-a", newerRemote, "dev-b")
        assertEquals(newerRemote, applied)

        // Identical timestamp ties break deterministically by device ID
        val tiedSettings = newerRemote.copy(updatedAt = 1000L)
        val tieWinnerOnDevA = SyncMergeEngine.resolveSettings(localSettings, "dev-a", tiedSettings, "dev-b")
        assertEquals("dev-b > dev-a so remote wins", tiedSettings, tieWinnerOnDevA)

        val tieWinnerOnDevB = SyncMergeEngine.resolveSettings(tiedSettings, "dev-b", localSettings, "dev-a")
        assertNull("dev-a < dev-b so local is kept", tieWinnerOnDevB)
    }

    @Test
    fun scenario8_payloadSecurity_hmacSha256VerificationAndTamperDetection() {
        val payloadJson = """{"version":2,"deviceId":"phone-1","tasks":[{"taskId":"t1","title":"Secure Task"}]}"""
        val secretKey = "shared_paired_secret_pin_code_987654"

        // 1. Clean signature generation
        val signature = LANAuthSecurity.signPayload(payloadJson, secretKey)
        assertNotNull(signature)
        assertEquals(64, signature.length) // Hex-encoded 256-bit HMAC

        // 2. Legitimate verification passes
        val verified = LANAuthSecurity.verifyPayloadSignature(payloadJson, secretKey, signature)
        assertTrue("Signature verification must succeed for untampered payload", verified)

        // 3. 1-byte tamper in payload fails verification
        val tamperedPayload = payloadJson.replace("Secure Task", "Hacked Task")
        val tamperedVerified = LANAuthSecurity.verifyPayloadSignature(tamperedPayload, secretKey, signature)
        assertFalse("Tampered payload must fail cryptographic verification", tamperedVerified)

        // 4. Invalid secret key fails verification
        val invalidKeyVerified = LANAuthSecurity.verifyPayloadSignature(payloadJson, "wrong_secret_key", signature)
        assertFalse("Incorrect shared key must fail cryptographic verification", invalidKeyVerified)
    }
}
