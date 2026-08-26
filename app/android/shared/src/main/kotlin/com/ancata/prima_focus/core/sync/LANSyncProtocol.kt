package com.ancata.prima_focus.core.sync

import com.ancata.prima_focus.core.model.Session
import com.ancata.prima_focus.core.model.Task
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class LANSyncPacket(
    val version: Int = 1,
    val deviceId: String,
    val deviceName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tasks: List<Task> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val authSignature: String? = null
)

data class PairingRequest(
    val deviceId: String,
    val deviceName: String,
    val pinHash: String
)

data class PairingResponse(
    val success: Boolean,
    val sessionToken: String? = null,
    val message: String? = null
)

object SyncMergeEngine {

    /**
     * Executes deterministic, version-aware Last-Write-Wins (LWW) conflict resolution.
     * Incorporates Red-Team mitigation: State COMPLETED is non-regressive against PENDING.
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

        // Rule 2: Version-aware hierarchy: syncVersion -> updatedAt
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
