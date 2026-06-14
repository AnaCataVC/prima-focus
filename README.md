# Prima-Focus

![Platform Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android)
![Platform Web](https://img.shields.io/badge/Platform-Web-09D3AC?style=flat&logo=pwa)
![Architecture Offline-First](https://img.shields.io/badge/Architecture-Offline--First-blue?style=flat)
![License All Rights Reserved](https://img.shields.io/badge/License-All_Rights_Reserved-red?style=flat)

Prima-Focus is an offline-first task management application designed to help you focus on what truly matters. It features a dual-client architecture (Native Android + Web PWA) and uses an advanced predictive priority scoring system to dynamically select your "Today Task."

## Key Features

- **Predictive Priority Scoring:** Automatically calculates the most important task based on urgency, category weight, and manual boosts.
- **Dynamic Notifications:** Uses aggressive, standard, or soft notifications depending on the task's computed score.
- **Dual Client Architecture:** 
  - **Android Native:** Built with Jetpack Compose, Room database, and WorkManager for background processing.
  - **Web PWA:** Built with React, Vite, IndexedDB, and Service Workers for a seamless desktop/mobile web experience.
- **Offline-First:** All actions are performed locally first, synchronizing with Firestore in the background using a robust `version` and `dirty` flag system.

## Documentation Index

Explore our comprehensive technical documentation to understand how Prima-Focus works under the hood:

- [Developer Guide & Onboarding](docs/developer-guide.md)
- [System Architecture](docs/architecture.md)
- [Priority Logic & Scoring](docs/priority-logic.md)
- [Notification Flow](docs/notification_flow.md)
- [Database Schema](docs/database_schema.sql)
- [UI Specifications](docs/ui_spec.md)
- [Implementation & Sync Notes](docs/implementation_notes.md)
