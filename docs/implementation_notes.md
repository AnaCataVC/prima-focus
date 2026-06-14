# Implementation and Synchronization Notes

## Offline-First Strategy (Local Sync Queue)
The core tenet of Prima-Focus is that the user must never be blocked by a poor network connection.
- **Local Writes First:** All modifications immediately persist to Room (Android) or IndexedDB (Web).
- Keep `dirty = true` and increment `version` on every change at the record level.
- A `SyncQueue` (background agent) processes writes in batch when network is available and marks `dirty = false` asynchronously upon Firestore confirmation.

## Conflict Resolution
- Compare `version` and `updatedAt` before overwriting.
- Prefer client-side merging when possible.
- Fallback to *last-write-wins* based on `updatedAt`.

## Client-Side Encryption (E2EE)
- If E2EE is enabled, encrypt `title` and `description` before persisting locally and uploading to the cloud.
- Leave `categoryWeight`, `hasTime` and `timeUrgency` in plain text to allow the prioritization logic to run locally without decrypting everything.

## Recurrence
- Store rules in `RRULE` format in the `recurrence` field and generate local instances.
- Upon completing a recurring task, create the next instance based on the rule and send it to the sync queue.

## Migrations
- Strictly version the database schema.
- Provide migrations in Room (Android).
- For IndexedDB (Web), use `openDB` with an `upgrade` function that transforms existing objects if there are version changes in the schema.
