# Implementation Notes

## Local-First Strategy
The core tenet of Prima-Focus is privacy and speed. 
- All modifications immediately persist to the Room Database (SQLite) on the Android device.
- There is no cloud synchronization or background syncing queue.
- No network connection is required to use the app.

## Recurrence
- Store rules in `RRULE` format in the `recurrence` field and generate local instances.
- Upon completing a recurring task, create the next instance based on the rule and save it to the local database.

## Migrations
- Strictly version the database schema.
- Provide migrations in Room using `Migration` classes to handle schema updates without data loss.

## UI Implementation
- The visual interface is natively built with **Jetpack Compose** following Material 3 guidelines and enforcing a Dark Mode aesthetic.
- The Pomodoro timer relies on an Android `Foreground Service` (`TimerService`) to ensure persistence and reliability even when the app is backgrounded.

## Background Processing & Notifications
- **WorkManager** is used for periodic background execution (`NotificationWorker`) every 15 minutes.
- The background worker recalculates priority scores dynamically (since time urgency and age change) and triggers local notifications based on the top task's score.
- **Notification Thresholds:** Aggressive (Score >= 70), Standard (Score 40-69), Soft (Score < 40).

## Database & Domain Integration
- `TaskViewModel` acts as the bridge connecting the Compose UI with Room `TaskDao` and `SessionDao`.
- Task priority is calculated deterministically through the `PriorityEngine` before every insertion and periodically by the background worker.
