package com.ancata.prima_focus.core.sync

import com.ancata.prima_focus.core.model.Session
import com.ancata.prima_focus.core.model.Task
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Settings shared between paired devices, resolved Last-Write-Wins by [updatedAt].
 * Per-device preferences (theme) are intentionally not part of it.
 */
data class SyncSettings(
    val categoryEmojis: Map<String, String> = emptyMap(),
    val skipMissedOccurrences: Boolean = true,
    val updatedAt: Long = 0L
)

object SyncMergeEngine {

    /**
     * Executes deterministic, version-aware Last-Write-Wins (LWW) conflict resolution.
     * Incorporates Red-Team mitigation: State COMPLETED is non-regressive against PENDING.
     * Full ties (same syncVersion and updatedAt, different content) are broken by a content
     * hash, so both devices pick the same winner without exchanging anything else.
     */
    fun resolveTaskConflict(local: Task?, remote: Task): MergeAction<Task> {
        if (local == null) {
            return MergeAction.Insert(remote)
        }

        // Rule 1: Guard against zombie reopening of completed tasks due to clock drift
        if (local.status == "completed" && remote.status == "pending" && !remote.isDeleted) {
            // Keep completed status, but merge highest version/timestamp
            val merged = local.copy(
                syncVersion = maxOf(local.syncVersion, remote.syncVersion) + 1,
                updatedAt = maxOf(local.updatedAt, remote.updatedAt)
            )
            return MergeAction.Update(merged)
        }

        // Rule 2: Version-aware hierarchy: syncVersion -> updatedAt -> content hash
        val shouldRemoteWin = when {
            remote.syncVersion != local.syncVersion -> remote.syncVersion > local.syncVersion
            remote.updatedAt != local.updatedAt -> remote.updatedAt > local.updatedAt
            else -> contentHash(remote) > contentHash(local)
        }

        return if (shouldRemoteWin) {
            MergeAction.Update(remote)
        } else {
            MergeAction.KeepLocal
        }
    }

    fun resolveSessionConflict(local: Session?, remote: Session): MergeAction<Session> {
        if (local == null) {
            return MergeAction.Insert(remote)
        }

        val shouldRemoteWin = when {
            remote.syncVersion > local.syncVersion -> true
            remote.syncVersion == local.syncVersion && remote.updatedAt > local.updatedAt -> true
            else -> false
        }

        return if (shouldRemoteWin) {
            MergeAction.Update(remote)
        } else {
            MergeAction.KeepLocal
        }
    }

    /**
     * Returns the remote settings when they should replace the local ones, or null to keep local.
     * Equal timestamps fall back to comparing device ids so both sides agree on the winner.
     */
    fun resolveSettings(
        local: SyncSettings,
        localDeviceId: String,
        remote: SyncSettings?,
        remoteDeviceId: String?
    ): SyncSettings? {
        if (remote == null) return null
        val remoteWins = when {
            remote.updatedAt != local.updatedAt -> remote.updatedAt > local.updatedAt
            remote == local -> false
            else -> (remoteDeviceId ?: "") > localDeviceId
        }
        return if (remoteWins) remote else null
    }

    /**
     * Finds pending occurrences that duplicate another pending occurrence of the same recurring
     * series on the same date. For each group the copy with the highest syncVersion (then
     * updatedAt, then taskId) survives; the returned tasks are the ones to tombstone.
     */
    fun findRecurringDuplicates(tasks: List<Task>): List<Task> =
        tasks.asSequence()
            .filter { it.status == "pending" && !it.isDeleted && it.recurrenceGroupId != null && it.date != null }
            .groupBy { it.recurrenceGroupId to it.date }
            .values
            .filter { it.size > 1 }
            .flatMap { group ->
                val keeper = group.maxWith(
                    compareBy<Task>({ it.syncVersion }, { it.updatedAt }, { it.taskId })
                )
                group.filter { it.taskId != keeper.taskId }
            }

    /**
     * Hash of the user-visible fields. Device-local derived values (priorityScore, timeUrgency)
     * are excluded because each device recalculates them.
     */
    private fun contentHash(task: Task): String {
        val content = listOf(
            task.title, task.description, task.category, task.subcategory, task.categoryWeight,
            task.date, task.time, task.estimatedMinutes, task.isProject, task.recurrence,
            task.recurrenceGroupId, task.manualBoost, task.nonPostponable, task.status,
            task.postponedReason, task.meta, task.isDeleted, task.missedPolicy
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

sealed class MergeAction<out T> {
    data class Insert<T>(val item: T) : MergeAction<T>()
    data class Update<T>(val item: T) : MergeAction<T>()
    object KeepLocal : MergeAction<Nothing>()
}

object LANAuthSecurity {

    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun signPayload(jsonPayload: String, sessionKey: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(sessionKey.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKey)
        val hmac = mac.doFinal(jsonPayload.toByteArray(Charsets.UTF_8))
        return hmac.joinToString("") { "%02x".format(it) }
    }

    fun verifyPayloadSignature(jsonPayload: String, sessionKey: String, signature: String): Boolean {
        val expected = signPayload(jsonPayload, sessionKey)
        return MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), signature.toByteArray(Charsets.UTF_8))
    }
}
