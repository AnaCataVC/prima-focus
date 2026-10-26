# Prima-Focus System Architecture & Technical Specifications

## 1. Overview
Prima-Focus is a local-first task management and deep work application built with a **modular Kotlin Multiplatform (KMP)** architecture focused on Android (smartphones and tablets). It guarantees complete data ownership, low latency, and zero telemetry by keeping all persistent records on the local device. Peer-to-peer (P2P) synchronization operates entirely over local networks via Google Nearby Connections (Wi-Fi Direct and BLE) with deterministic conflict resolution, eliminating the need for cloud databases, user accounts, or external servers.

---

## 2. Core Stack & Module Boundaries

| Layer / Subsystem | Technology | Purpose |
| :--- | :--- | :--- |
| **Common Domain (`:shared`)** | Kotlin Multiplatform 2.2.10 | Pure business logic, `SharedPriorityEngine`, `SyncMergeEngine`, `SharedRecurrenceCalculator`, models (`Task`, `Session`, `PriorityBand`). Zero Android dependencies. |
| **UI Toolkit (`:app`)** | Jetpack Compose (Material 3) | Declarative UI with Adaptive Window Size layouts (Phone, Tablet Split-View, NavigationRail), Material You dynamic color, custom Canvas particle confetti. |
| **Local Persistence (`:app`)** | Room Database v7 (SQLite) | Relational database with soft-delete tombstones, monotonic `syncVersion`, and safe schema migrations. |
| **Home Screen Widgets (`:app`)** | Jetpack Glance | Interactive single-task and 3-task widgets with optimistic completion and undo notifications. |
| **P2P Networking (`:app`)** | Google Nearby Connections API | Wi-Fi Direct and BLE mesh in `P2P_STAR` topology with 6-digit numeric comparison pairing. |
| **Background Processing (`:app`)** | Android WorkManager & Foreground Services | Recurrence reconciliation, 30-day tombstone garbage collection, and persistent focus session timers. |

---

## 3. High-Level Modular Architecture

```mermaid
flowchart TD
    subgraph SharedModule [":shared — Pure Kotlin Multiplatform"]
        SPE["SharedPriorityEngine<br/>• Static Base & Aging Decay<br/>• Critical Tier Floor (>= 70.0)<br/>• Dynamic Calendar Urgency"]
        SME["SyncMergeEngine (LWW + Clock Drift)<br/>• Monotonic syncVersion Check<br/>• Non-Regressive Completed Guard<br/>• SHA-256 Content Hash Tie-Break"]
        SRC["SharedRecurrenceCalculator<br/>• Deterministic Instance UUIDs<br/>• Skip-Missed Occurrences"]
        SEC["LANAuthSecurity<br/>• HMAC-SHA256 Payload Signatures<br/>• SHA-256 PIN Hashing"]
    end

    subgraph AppUI [":app — Presentation & UI"]
        Compose["Jetpack Compose UI<br/>(Today Task, Pomodoro, Adaptive Tablets)"]
        Glance["Jetpack Glance Widgets<br/>(Single-Task & 3-Task List)"]
        VM["ViewModels<br/>(StateFlow & Unidirectional Data Flow)"]
    end

    subgraph AppData [":app — Data & Network Infrastructure"]
        RoomDB[("Room Database v7<br/>SQLite Local Store")]
        Nearby["P2PSyncManager<br/>(Google Nearby Connections API)"]
        Workers["WorkManager Workers<br/>(Recurrence Reconciliation & GC)"]
    end

    Compose --> VM
    Glance --> VM
    VM --> SPE
    VM --> RoomDB
    Nearby --> SEC
    Nearby --> SME
    SME --> RoomDB
    Workers --> SRC
    Workers --> RoomDB
```

---

## 4. Peer-to-Peer (P2P) Synchronization Protocol

Synchronization between devices uses the Google Nearby Connections `P2P_STAR` topology. One device operates as **Host ("Ser Anfitrión")** and the other as **Client ("Ser Cliente")**, authenticated through mutual out-of-band numeric confirmation.

### 4.1. Network Handshake & Mutual Data Exchange Sequence

```mermaid
sequenceDiagram
    autonumber
    participant Host as Device A (Host / Phone)
    participant Client as Device B (Client / Tablet)
    participant Engine as SyncMergeEngine (:shared)
    participant DB as Room Database v7

    Note over Host: User taps "Ser Anfitrión"
    Host->>Host: startAdvertising() [45s timeout guard]
    Note over Client: User taps "Ser Cliente"
    Client->>Client: startDiscovery() [45s timeout guard]

    Client->>Host: Connection Request (Device Name & Endpoint)
    Host-->>Client: onConnectionInitiated (Shows 6-Digit SAS Code)
    
    Note over Host,Client: Users visually verify matching 6-digit PIN on both screens
    Host->>Host: acceptConnection()
    Client->>Client: acceptConnection()

    Note over Host,Client: Session Key established (HMAC-SHA256 authenticated)
    Host->>Client: Payload: SyncDataPayload (Tasks, Sessions, Settings) + Signature
    Client->>Host: Payload: SyncDataPayload (Tasks, Sessions, Settings) + Signature

    Note over Host,Engine: Atomic Merge Execution
    loop For each incoming Task / Session
        Host->>Engine: resolveTaskConflict(localTask, remoteTask)
        Engine-->>Host: MergeAction (KeepLocal | Update | Insert)
    end

    Host->>Engine: findRecurringDuplicates(allTasks)
    Engine-->>Host: List of redundant recurrence instances
    Host->>DB: Apply updates, mark duplicates as deleted, commit transaction
    Host->>DB: purgeOldTombstones(now - 30 days)
    
    Note over Host,Client: Connection gracefully terminated. Summary counts displayed.
```

---

## 5. Distributed Conflict Resolution Engine (`SyncMergeEngine`)

Because devices may work disconnected for days across varying time zones or with drifted system clocks, Prima Focus abandons naive wall-clock Last-Write-Wins in favor of a **version-aware, multi-tier deterministic resolution algorithm**.

### 5.1. Conflict Resolution Flowchart

```mermaid
flowchart TD
    Start["resolveTaskConflict(local, remote)"] --> IsNull{"local == null ?"}
    IsNull -- Yes --> ActionInsert["MergeAction.Insert(remote)"]
    IsNull -- No --> Rule1{"Rule 1: Non-Regressive Completed?<br/>local == completed AND<br/>remote == pending AND !remote.isDeleted"}

    Rule1 -- Yes --> FixRegress["Preserve completed status<br/>Bump syncVersion = max(vL, vR) + 1<br/>MergeAction.Update(merged)"]
    
    Rule1 -- No --> Rule2{"Rule 2: syncVersion check<br/>remote.syncVersion != local.syncVersion ?"}
    Rule2 -- Yes --> VersionCheck{"remote.syncVersion > local.syncVersion ?"}
    VersionCheck -- Yes --> ActionUpdate["MergeAction.Update(remote)"]
    VersionCheck -- No --> ActionKeep["MergeAction.KeepLocal"]

    Rule2 -- No --> Rule3{"Rule 3: updatedAt check<br/>remote.updatedAt != local.updatedAt ?"}
    Rule3 -- Yes --> TimestampCheck{"remote.updatedAt > local.updatedAt ?"}
    TimestampCheck -- Yes --> ActionUpdate
    TimestampCheck -- No --> ActionKeep

    Rule3 -- No --> Rule4{"Rule 4: Deterministic Tie-Break<br/>SHA-256(remoteContent) > SHA-256(localContent)"}
    Rule4 -- Yes --> ActionUpdate
    Rule4 -- No --> ActionKeep
```

---

## 6. Distributed State Guarantees

The following 8 invariants are verified by automated unit tests in `SyncConflictMatrixTest.kt` and `P2PSyncTest.kt`:

| Invariant | Problem Solved | Mechanism & Guarantee |
| :--- | :--- | :--- |
| **1. Tombstone Propagation** | Hard deletes on Device A resurrecting on Device B after sync. | Hard deletes are converted to soft-delete tombstones (`isDeleted = true`, `deletedAt`). Higher `syncVersion` ensures the tombstone overwrites older active instances on all peers. |
| **2. Stale Edit Rejection** | Edits made on an out-of-date offline device overwriting newer edits. | Monotonic version checking (`syncVersion`) strictly rejects any incoming payload with an inferior version number (`MergeAction.KeepLocal`). |
| **3. Clock-Drift Immunity** | Devices with un-synchronized or manipulated system clocks overwriting valid edits. | `syncVersion` is strictly checked before `updatedAt`. A device whose clock is hours in the past but has incremented its version will cleanly beat a device with a future-skewed clock. |
| **4. Full-Tie Convergence** | Two devices edit the same task during the exact same millisecond at the same version. | The SHA-256 content hash of all user-visible fields breaks the tie. Both devices compute the hash locally and deterministically converge to the exact same winner without additional network roundtrips. |
| **5. Non-Regressive Completion** | Completed tasks reopening as "pending" because another device synced an older state. | If local state is `completed` and remote is `pending`, the completed state is strictly preserved, and `syncVersion` is bumped to `max(vL, vR) + 1` to heal the divergence permanently. |
| **6. Concurrent Recurrence Dedup** | Two offline devices independently generating the next recurrence for the same day. | Deterministic instance IDs (`UUID.nameUUIDFromBytes("$groupId|$date")`) + `findRecurringDuplicates` identifies and tombstones redundant instances, keeping only the highest-version copy. |
| **7. Settings Convergence** | Synchronized preferences (category emojis, skip-missed policy) diverging across peers. | Last-Write-Wins on `settingsUpdatedAt`, with a stable per-device ID tie-breaker (`remoteDeviceId > localDeviceId`) in case of timestamp equality. |
| **8. Cryptographic Tamper Rejection** | MITM packet manipulation or corrupted transmissions on local Wi-Fi. | Payloads are signed with HMAC-SHA256 using the session secret key derived during the 6-digit SAS pairing handshake; tampered packets fail validation and are dropped immediately. |

---

## 7. Room Database v7 Relational Schema & Migration Lifecycle

Prima Focus maintains an SQLite database with safe linear migrations:

```mermaid
flowchart LR
    V1["Schema v1<br/>Initial Tasks"] --> V2["Schema v2<br/>Sessions Added"]
    V2 --> V3["Schema v3<br/>Recurrence Groups"]
    V3 --> V4["Schema v4<br/>Dynamic Urgency"]
    V4 --> V5["Schema v5 (MIGRATION_4_5)<br/>+ isDeleted<br/>+ deletedAt<br/>+ syncVersion"]
    V5 --> V6["Schema v6 (MIGRATION_5_6)<br/>DROP TABLE events<br/>(Pruned Dead Legacy Table)"]
    V6 --> V7["Schema v7 (MIGRATION_6_7)<br/>+ missedPolicy<br/>(Per-Task Override)"]
```

### Automatic 30-Day Tombstone Garbage Collection
To prevent soft-deleted tombstones from bloating SQLite storage indefinitely, an automated garbage collection routine runs on application startup:

```sql
DELETE FROM tasks WHERE isDeleted = 1 AND deletedAt < :cutoffTimestamp;
DELETE FROM sessions WHERE isDeleted = 1 AND deletedAt < :cutoffTimestamp;
```
*(Where `:cutoffTimestamp = System.currentTimeMillis() - 30.days`)*.
