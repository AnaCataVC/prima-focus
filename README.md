# Prima-Focus

![Platform Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android)
![Architecture Local-First](https://img.shields.io/badge/Architecture-Local--First-blue?style=flat)
![License All Rights Reserved](https://img.shields.io/badge/License-All_Rights_Reserved-red?style=flat)

Prima-Focus is a local-first task management application designed to help you focus on what truly matters. It is built natively for Android and uses an advanced predictive priority scoring system to dynamically select your "Today Task." All data is stored locally on the device for maximum privacy and performance.

## Current Status

- **UI Development**: The native Jetpack Compose visual interface (including HomeScreen, InboxModal, TimerScreen, QuickReviewModal, and SettingsScreen) has been completely implemented according to the UI wireframes.

## Key Features

- **Predictive Priority Scoring:** Automatically calculates the most important task based on urgency, category weight, and manual boosts.
- **Dynamic Notifications:** Uses aggressive, standard, or soft notifications depending on the task's computed score.
- **Native Android Experience:** Built with Jetpack Compose for a modern UI, Room database for local persistence, and WorkManager for reliable background processing.
- **Local-First & Private:** All actions and data are kept 100% locally on your device, requiring no internet connection or cloud syncing.

## Documentation Index

Explore our comprehensive technical documentation to understand how Prima-Focus works under the hood:

- [System Architecture](docs/architecture.md)
- [Priority Logic & Scoring](docs/priority-logic.md)
- [Notification Flow](docs/notification_flow.md)
- [Database Schema](docs/database_schema.sql)
- [UI Specifications](docs/ui_spec.md)
- [Implementation & Sync Notes](docs/implementation_notes.md)
