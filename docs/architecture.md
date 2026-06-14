# Prima-Focus Architecture

## Overview
Prima-Focus is an offline-first task management application built with a dual-client architecture:
1. **Android Client**: Native experience built entirely with Kotlin and Jetpack Compose. Utilizes Room for robust local persistence, and `WorkManager` paired with Foreground Services for resilient background timers and notifications.
2. **Web Client (PWA)**: Built with React and Vite for broad accessibility. Uses a 3-column layout architecture and IndexedDB for local persistence. Notifications are handled via native Service Workers and the Web Push API.

Both clients communicate with a backend (Firestore) using an eventual consistency model based on `dirty` and `version` flags.

## Offline-First Approach
1. **Local First**: All reads and writes happen directly against the local database (Room/IndexedDB).
2. **Sync Queue**: Any mutation marks the entity as `dirty=true` and increments `version`.
3. **Eventual Sync**: A background process (Sync-Agent logic) detects dirty records and pushes them to Firestore. If successful, `dirty` is cleared. If offline, the queue waits.

## Components
- **Data Layer**: Local DB schemas (Room entities, IndexedDB object stores), DAOs, and Repositories.
- **Domain Layer**: Business rules, primarily the predictive `priorityScore` calculation (`TaskEngine` / `priorityLogic.ts`).
- **Sync Layer**: Eventual consistency handlers using `dirty` and `version` flags for pushing to/pulling from Firestore.
- **UI Layer**: Presentation logic built with a minimalist technical pastel design system. Android uses Jetpack Compose (`HomeTodayScreen`, `TimerScreen`), while the Web uses modular React components.
- **Notification Layer**: A dynamic scheduler using Android's `WorkManager` and Web `Service Workers` that assesses the priority score to trigger aggressive, standard, or soft reminders.

## System Architecture Diagram

```text
+----------------------+        +----------------------+        +----------------------+
|  Cliente Android     | <----> |   Cloud Firestore    | <----> |  Cliente Web PWA     |
|  (Kotlin)            |        |   (Firebase)         |        |  (JS / PWA)          |
|                      |        |                      |        |                      |
|  - UI (Inbox, Hoy)   |        |  - Documents: tasks  |        |  - UI (Inbox, Hoy)   |
|  - Domain rules      |        |  - Sessions, users   |        |  - Domain rules      |
|  - Room local cache  |        |  - Security rules    |        |  - IndexedDB cache   |
|  - WorkManager / FS  |        |  - Optional CFs      |        |  - Service Worker    |
|  - SpeechRecognizer  |        |    (Cloud Functions) |        |  - Web Speech API    |
+----------------------+        +----------------------+        +----------------------+
         |  ^                             ^   |
         |  |                             |   |
         v  |                             |   v
+----------------------+        +----------------------+
| Notification Manager | <----> | Firebase Cloud       |
| (local actions)      |        | Messaging (FCM)      |
+----------------------+        +----------------------+
```

### Implementation Notes:
- **Sync flow**: The client saves locally → marks `dirty=true` → Firestore SDK synchronizes when network connection is available.
- **Notifications**: Firebase Cloud Messaging (FCM) delivers push notifications; Cloud Functions will be used optionally to schedule server-side reminders.
- **Conflict Resolution**: Conflicts are resolved using the `updatedAt` and `version` fields on the client, applying a last-write-wins strategy per document.
