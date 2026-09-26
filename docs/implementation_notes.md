# Implementation Notes

## Local-First Strategy & Synchronization
The core tenet of Prima-Focus is privacy, speed, and device independence.
- All modifications immediately persist to the local Room Database v7 (SQLite) on Android.
- There is no cloud synchronization or remote server dependency. No internet connection is required.
- **Local P2P Sync**: Devices sync their state offline using the Google Nearby Connections API (`P2P_STAR` topology).
- **Host / Client Selection**: Android Nearby users explicitly toggle between "Ser Anfitrión" (Host / Advertising) and "Ser Cliente" (Client / Discovery), confirming the same Nearby authentication digits on both devices before any payload is exchanged.
- **Clock-Drift Resilient LWW**: Conflict resolution compares `syncVersion` first, then `updatedAt`, then a content hash, then a stable per-device `deviceId` — so both peers deterministically pick the same winner even on an exact tie.
- **Deterministic Recurring IDs & Dedup**: The next instance of a recurring series gets `taskId = UUID.nameUUIDFromBytes("$groupId|$date")`, so both devices spawning the same occurrence produce the same row instead of duplicates; the merge additionally dedupes any pre-existing duplicate sharing `recurrenceGroupId` + date.
- **Settings Sync**: category emojis and `skipMissedOccurrences` sync alongside tasks (LWW by `settingsUpdatedAt`); theme is device-local and never synced. A post-merge summary reports received/updated/new counts.
- **Soft Deletes (Tombstones)**: Deletions flag `isDeleted = 1` with a `deletedAt` timestamp and incremented `syncVersion`, preventing deleted tasks from resurrecting when synced against offline peers.
- **30-Day Tombstone Purge**: The database executes an automatic garbage collection query on application start (`TaskViewModel`), physically deleting tombstones older than 30 days to keep SQLite performant.
- **P2P Safety Protections**:
  - `AUTO_TIMEOUT_MS = 45000L`: Discovery and Advertising automatically abort after 45 seconds of inactivity to protect battery life.
  - `MAX_PAYLOAD_BYTES = 5MB`: Payloads exceeding 5 MB are rejected immediately to prevent heap exhaustion.

## Migrations (v1 -> v7)
- Strictly version the database schema.
- Provide migrations in Room using `Migration` classes to handle schema updates without data loss:
  - `MIGRATION_1_2`: Added `recurrenceGroupId` to tasks.
  - `MIGRATION_2_3`: Removed deprecated `subtasksCount` column via table recreate.
  - `MIGRATION_3_4`: Removed `durationMinutes` from sessions table via table recreate.
  - `MIGRATION_4_5`: Added `isDeleted`, `deletedAt`, and `syncVersion` to both `tasks` and `sessions` tables for distributed sync.
  - `MIGRATION_5_6`: Dropped the unused `events` dead table (`DROP TABLE IF EXISTS events`).
  - `MIGRATION_6_7`: Added nullable `missedPolicy` to `tasks` (per-task override for skipped/accumulated missed recurring occurrences; `NULL` defers to the global setting).

## UI Implementation & Adaptive Layouts
- The visual interface is natively built with **Jetpack Compose** following Material 3 guidelines, with a System/Light/Dark theme (plus Material You dynamic color on Android 12+) instead of a fixed Dark Mode.
- **Multi-Screen Support**: Utilizes `WindowSizeClass` to support adaptive scaling across form factors. Tablets (Expanded layout) feature a Split View interface with a custom Interactive Calendar that allows filtering tasks by date; medium/expanded widths also swap the bottom `NavigationBar` for a `NavigationRail`, and List/Settings content centers at `widthIn(max = 720.dp)`.
- The Pomodoro timer relies on an Android `Foreground Service` (`TimerService`) to ensure persistence and reliability even when the app is backgrounded.

## Background Processing & Notifications
- **WorkManager** is used for periodic background execution (`NotificationWorker`). The frequency is dynamically set based on `UserPreferences.notificationFrequency`, the single facade over the underlying `SharedPreferences`.
- The background worker recalculates priority scores dynamically (since time urgency and age change) and triggers local notifications based on the top task's score.
- **Notification Thresholds:** Aggressive (Score >= 70), Standard (Score 40-69), Soft (Score < 40).
- **Recurrence Reconciliation**: `RecurrenceReconciliationWorker` and `TaskCompletionUseCase` share `buildNextOccurrence()` to spawn the next instance of a completed recurring task. With `skipMissedOccurrences` on (global default, overridable per task via `missedPolicy`), an overdue series jumps straight to the next due date instead of spawning every missed occurrence.
- **Widget Feedback**: completing a task from a widget shows an optimistic checkmark, posts an "Undo" notification (`WidgetActionReceiver`), and refreshes both widgets through the shared `WidgetUpdater.refreshAll()`.

## Database & Domain Integration
- **Atomic Task Completion (No Timing)**: Tasks operate as discrete atomic items (Done / Pending) with no duration estimation or timing requirements. Tasks store `estimatedMinutes = null`, and the prioritization engine calculates scores using category weight, calendar day hierarchy, and age decay without duration bias.
- **Deterministic Multi-Column Ordering**: The SQL query enforces a strict 6-tier order (`priorityScore DESC`, `hasTime DESC`, `CASE WHEN date IS NULL THEN 1 ELSE 0 END`, `date ASC`, `createdAt ASC`, `taskId ASC`), ensuring predictable Top 3 selection and flicker-free tie-breaking.
- **Relational History Tracking**: Room POJO `TaskWithSessions` pairs completed tasks with their session sentiment and duration in a single `@Transaction` query. When `isHistoryTrackingEnabled` is disabled, timers complete tasks with zero modal friction and history views are hidden.
- **Storage Access Framework (SAF) Backup**: Serializes `TaskEntity` and `SessionEntity` collections into structured JSON, supporting non-destructive merges (via `updatedAt` LWW) and complete atomic overwrites.

## Infrastructure & Clean Code
- **Dependency Management**: Central Version Catalog (`libs.versions.toml`) manages all Gradle dependencies, keeping `build.gradle.kts` files clean and preventing version conflicts.
- **Constants & Utilities**: "Magic strings" are strictly centralized in `Constants.kt`. Shared mathematical or date/time logic is extracted to pure functions in `TimeUtils.kt`. `UserPreferences` centralizes all `SharedPreferences` access, `WidgetUpdater.refreshAll()` centralizes widget refresh, and `TaskActionRow` centralizes the 48dp edit/snooze/boost/demote/delete row used by the Home hero, secondary cards, and list items.
