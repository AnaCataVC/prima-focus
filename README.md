<p align="center">
  <img src="icon.png" alt="prima-focus Logo" width="120" />
</p>

# Prima-Focus

[English](README.md) | [Español](README.es.md)

![Platform Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android)
![Architecture Local-First](https://img.shields.io/badge/Architecture-Local--First%20(KMP)-blue?style=flat)
![Room Database v7](https://img.shields.io/badge/Room-v7%20(SQLite)-4285F4?style=flat&logo=sqlite&logoColor=white)
![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform%202.2.10-0095D5?style=flat&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203-4285F4?style=flat&logo=android&logoColor=white)

---

### Project Description
Prima-Focus is a local-first task management and deep work application designed to help you focus on what truly matters. Built as a **native Android mobile application** with **Kotlin Multiplatform** shared domain logic, it uses an advanced predictive priority scoring system to dynamically select your "Today Task." All data is stored locally on each device for maximum privacy and performance, featuring hardened local peer-to-peer (P2P) synchronization via Google Nearby Connections without requiring any cloud backend.

### Key Architectural Highlights
- **Native Android Core (`:app`)**: The flagship mobile experience featuring Room Database v7 persistence, Material 3 Glassmorphism UI, Jetpack Glance Home Screen Widgets, and WorkManager background reminders.
- **Modular KMP Core (`:shared`)**: Unified predictive priority scoring (`SharedPriorityEngine`), recurrence projecting, quiet hours scheduling, and Last-Write-Wins (LWW) conflict resolution logic.
- **Flexible Host / Client P2P Synchronization (Nearby Connections)**:
  - *Mobile-to-Mobile*: Seamless peer-to-peer Wi-Fi Direct and BLE synchronization with explicit user mode selection between **Host ("Ser Anfitrión")** and **Client ("Ser Cliente")**, backed by a 45-second auto-timeout for battery conservation, a 5 MB payload limit, and a numeric-digit pairing confirmation shown on both devices.
- **Room Database v7 (Android)**: Clean, streamlined relational schema with `MIGRATION_6_7` adding a per-task `missedPolicy` override for skipped recurrences. Full support for Soft Deletes (Tombstones: `isDeleted`, `deletedAt`) and monotonic `syncVersion` to prevent deleted items from resurrecting.
- **Clock-Drift Resilient LWW**: Advanced conflict resolution that prioritizes logical version increments over system clocks, immunizing sync against device time skew.
- **Non-Regressive Task Completion**: Completed tasks are guaranteed to remain completed during merges regardless of time drift.
- **Automatic 30-Day Tombstone Purge**: Built-in SQLite Garbage Collection that cleans up deleted tombstones older than 30 days upon startup.
- **Adaptive Android Layouts**: Responsive UI using Jetpack Compose providing Split Views and interactive calendars on tablets, plus a `NavigationRail` on medium/expanded widths.
- **Category Emojis**: Each task category renders a configurable emoji (curated picker + free-text fallback) across Home, List, Inbox and both widgets.
- **Adaptive Theming**: System/Light/Dark mode plus Material You dynamic color (Android 12+), applied live without restarting the app.
- **Skip-Missed Recurrences**: A global default and a per-task override decide whether an overdue recurring task (e.g. daily medication) jumps straight to the next due occurrence instead of piling up overdue instances.
- **Sync Hardening**: Deterministic recurring-instance IDs and dedup prevent cross-device duplicate spawns, a stable per-device ID tiebreaks equal-version conflicts, categories/theme/skip-missed settings sync alongside tasks, and a post-merge summary reports received/updated/new counts.
- **Instant Widget Feedback**: Completing a task from a widget shows an optimistic checkmark immediately and posts an "Undo" notification, instead of waiting on a full widget refresh.

### Technologies Used
- **Languages & Frameworks**: Kotlin Multiplatform, Java 17 Toolchain
- **UI Toolkits**: Jetpack Compose (Android), Jetpack Glance (App Widgets)
- **Local Persistence**: Room Database v7 (Android SQLite)
- **Networking & Security**: Google Nearby Connections API (P2P Star Topology)
- **Background Processing**: WorkManager & Foreground Services

### Key Learnings
This project was a major architectural milestone. Throughout the process, I learned how to:
- Architect and build a modular **Kotlin Multiplatform (KMP)** project with pure shared domain logic.
- Implement robust local databases on mobile (**Room v7**) with migration safety and soft-delete tombstones.
- Design and red-team stress-test local peer-to-peer network protocols with battery safeguards and payload size limits.
- Handle clock-drift resilience, soft-delete tombstones, and garbage collection in distributed local-first systems.
- Master declarative UI design using **Jetpack Compose** and home screen widgets with **Jetpack Glance**.

### Documentation Index
Explore our comprehensive technical documentation to understand how Prima-Focus works under the hood:
- [System Architecture](docs/architecture.md)
- [Database Schema v7](docs/database_schema.sql)
- [Implementation Notes](docs/implementation_notes.md)
- [Priority Logic & Scoring](docs/priority-logic.md)
- [Desktop & LAN Sync Stress-Test](docs/external-references/desktop-kmp-sync-stress-test.md)
- [Notification Flow](docs/notification_flow.md)
- [UI Specifications](docs/ui_spec.md)
- [Learnings: P2P Sync, Tombstones & Clock Drift](docs/learning/p2p-sync-tombstones-clockdrift-gc.md)
- [All Architecture Learnings](docs/learning/)

---

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

