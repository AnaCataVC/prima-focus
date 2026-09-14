# Prima-Focus Architecture

## Overview
Prima-Focus is a local-first task management application built with a **modular Kotlin Multiplatform (KMP)** architecture focused on Android (phones and tablets). 
It is designed to provide a robust, private, and highly responsive experience by keeping all data locally on each device. It synchronizes across Android devices using secure local peer-to-peer (P2P) networking via Google Nearby Connections without requiring any cloud backend.

## Core Stack
- **Language**: Kotlin Multiplatform (Kotlin 2.2.10)
- **UI Toolkit**: Jetpack Compose (Material Design 3 with Adaptive Layouts) for Android
- **Local Persistence**: Room Database v6 (Android) with deterministic relational schemas and soft delete tombstones
- **Background Processing & Scheduling**: Android `WorkManager` & Foreground Services for mobile timers/notifications
- **P2P Networking/Sync**: Google Nearby Connections API (Android-to-Android P2P Star Topology)

## Module Structure
- **`:shared` (`com.ancata.prima_focus.core`)**: Common domain logic, `SharedPriorityEngine`, data models (`Task`, `Session`, `PriorityBand`), recurrence calculations (`SharedRecurrenceCalculator`), quiet hours scheduling (`SharedTimeUtils`), and the version-aware Last-Write-Wins (LWW) conflict resolution engine (`SyncMergeEngine`).
- **`:app` (`com.ancata.prima_focus`)**: Native Android client featuring Room v6 persistence, Material 3 Glassmorphism UI, Jetpack Glance Home Screen Widgets, and WorkManager background reminders.

## System Architecture Diagram

```text
+-------------------------------------------------------------------------+
|                    :shared (Core Domain Module)                         |
|                                                                         |
|  - SharedPriorityEngine (Base Score + Time Urgency + Aging)             |
|  - Models: Task, Session, PriorityBand                                  |
|  - SyncMergeEngine (LWW + Clock-Drift Immunity + Non-Regressive State)  |
|  - SharedRecurrenceCalculator & SharedTimeUtils                         |
+-------------------------------------------------------------------------+
                                    ^
                                    |
+-----------------------------------+-----------------------------------+
|                     :app (Android Native Client)                      |
|                                                                       |
|  - Compose UI (Phone/Tablet Adaptive Layouts)                         |
|  - Split View & Calendar Widget                                       |
|  - Room Database v6 (Tombstones & syncVersion)                        |
|  - Google Nearby P2P Sync (Host / Client Modes)                       |
|  - WorkManager Reminders & Jetpack Glance Widgets                     |
|  - JSON Backup Export/Import with Atomic LWW Merge Fallback           |
+-----------------------------------------------------------------------+
```

## Synchronization & Backup Architecture

### 1. Mobile-to-Mobile Nearby P2P Sync (Host vs Client Selection)
- Employs Google Nearby Connections using the `P2P_STAR` topology.
- In `SettingsScreen`, users explicitly choose their synchronization role:
  - **Host ("Ser Anfitrión")**: Executes `p2pSyncManager.startAdvertising()`, broadcasting the device model to nearby peers.
  - **Client ("Ser Cliente")**: Executes `p2pSyncManager.startDiscovery()`, scanning for available advertising peers.
- **Battery & Safety Protections**:
  - Discovery and Advertising automatically abort after 45 seconds of inactivity (`AUTO_TIMEOUT_MS = 45000L`).
  - Payloads exceeding 5 MB (`MAX_PAYLOAD_BYTES`) are rejected immediately.

### 2. Offline JSON Backup Fallback
- For devices separated across network topologies or without direct Bluetooth/Wi-Fi Direct connectivity, users can export and import complete JSON backups with atomic Last-Write-Wins merging.

### 3. Room Database v6 Schema & Data Guarantees
- **Removal of Legacy Tables**: Room v6 (`MIGRATION_5_6`) drops the unused dead table `events`, leaving a streamlined relational schema comprised solely of `tasks` and `sessions`.
- **Clock-Drift Immunity**: Two-tier conflict resolution checks `syncVersion` before `updatedAt`, ensuring offline edits on out-of-sync clocks are never discarded.
- **Non-Regressive Completed State**: Completed tasks remain completed during sync merges even if an older pending state has a slightly drifted timestamp.
- **Automatic 30-Day Tombstone Purge**: Hard deletions are converted to soft-deletions (`isDeleted = 1`), and entries older than 30 days are purged automatically at application startup to keep SQLite storage fast and compact.
