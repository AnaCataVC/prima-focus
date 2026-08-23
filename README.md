# Prima-Focus

![Platform Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android)
![Architecture Local-First](https://img.shields.io/badge/Architecture-Local--First-blue?style=flat)
![Room Database v5](https://img.shields.io/badge/Room-v5%20(Tombstones%20%2B%20LWW)-4285F4?style=flat&logo=sqlite&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=flat&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat&logo=android&logoColor=white)

*English version below | Versión en español abajo*

---

## English

### Project Description
Prima-Focus is a local-first task management application designed to help you focus on what truly matters. Built natively for Android, it uses an advanced predictive priority scoring system to dynamically select your "Today Task." All data is stored locally on the device for maximum privacy and performance, featuring hardened Peer-to-Peer (P2P) synchronization across devices without requiring any cloud backend.

### Key Architectural Highlights
- **Room Database v5**: Full support for Soft Deletes (Tombstones: `isDeleted`, `deletedAt`) and monotonic `syncVersion` to prevent deleted items from resurrecting.
- **Clock-Drift Resilient LWW**: Advanced Last-Write-Wins conflict resolution that prioritizes logical version increments over system clocks, immunizing sync against device time skew.
- **Automatic 30-Day Tombstone Purge**: Built-in SQLite Garbage Collection that cleans up deleted tombstones older than 30 days upon startup.
- **Hardened P2PSyncManager**: Local device sync via Google Nearby Connections with a 45-second battery-saving auto-timeout, a 5MB payload safety limit, and friendly device identification (`Build.MODEL`).
- **Adaptive Layouts**: Responsive UI using Jetpack Compose and `WindowSizeClass` providing Split Views and interactive calendars on tablets.

### Technologies Used
- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose (Material Design 3)
- **Local Persistence**: Room Database v5 (SQLite)
- **P2P Synchronization**: Google Nearby Connections API
- **Background Processing**: WorkManager & Foreground Services

### Key Learnings
This project was a major milestone as it was **my very first time developing a native mobile application**. Throughout the process, I learned how to:
- Architect and build a complete mobile app from scratch.
- Master declarative UI design using **Jetpack Compose**.
- Implement local database persistence and migrations using **Room**.
- Handle complex background tasks and asynchronous notifications using **WorkManager** and **Foreground Services**.
- Design robust offline P2P data synchronization algorithms and conflict resolution strategies.

### Documentation Index
Explore our comprehensive technical documentation to understand how Prima-Focus works under the hood:
- [System Architecture](docs/architecture.md)
- [Database Schema v5](docs/database_schema.sql)
- [Implementation Notes](docs/implementation_notes.md)
- [Priority Logic & Scoring](docs/priority-logic.md)
- [Notification Flow](docs/notification_flow.md)
- [UI Specifications](docs/ui_spec.md)
- [Learnings: P2P Sync, Tombstones & Clock Drift](docs/learning/p2p-sync-tombstones-clockdrift-gc.md)
- [All Architecture Learnings](docs/learning/)

---

## Español

### Descripción del Proyecto
Prima-Focus es una aplicación de gestión de tareas "local-first" diseñada para ayudarte a enfocarte en lo que realmente importa. Construida nativamente para Android, utiliza un avanzado sistema predictivo de puntuación de prioridad para seleccionar dinámicamente tu "Tarea de Hoy". Todos los datos se almacenan localmente en el dispositivo para garantizar máxima privacidad y rendimiento, con sincronización local P2P blindada sin necesidad de servidores en la nube.

### Puntos Destacados de la Arquitectura
- **Base de Datos Room v5**: Soporte completo de borrado lógico (*Soft Deletes* con lápidas `isDeleted`, `deletedAt`) y `syncVersion` incremental para evitar la resurrección de tareas borradas.
- **Resolución de Conflictos LWW Blindada**: Algoritmo *Last-Write-Wins* resistente al *Clock Drift* (desfase de reloj), evaluando la versión lógica antes que el reloj del sistema.
- **Purga Automática de 30 Días**: Recolección de basura (*Garbage Collection*) en SQLite que elimina permanentemente lápidas con más de 30 días de antigüedad al iniciar la app.
- **P2PSyncManager Endurecido**: Sincronización local mediante Google Nearby Connections con auto-timeout de 45 segundos para cuidar la batería, límite de 5MB por payload y nombres legibles de dispositivos (`Build.MODEL`).
- **Diseño Adaptativo**: Interfaz declarativa en Jetpack Compose con soporte para teléfonos y vista dividida con calendario interactivo en tabletas (`WindowSizeClass`).

### Tecnologías Utilizadas
- **Lenguaje**: Kotlin
- **Interfaz Gráfica**: Jetpack Compose (Material Design 3)
- **Persistencia Local**: Base de datos Room v5 (SQLite)
- **Sincronización P2P**: Google Nearby Connections API
- **Procesamiento en Segundo Plano**: WorkManager y Foreground Services

### Aprendizajes Clave
Este proyecto representó un gran hito personal, ya que fue **la primera vez que desarrollé una aplicación móvil nativa**. A lo largo del proceso aprendí a:
- Diseñar y construir la arquitectura de una app móvil desde cero.
- Dominar el diseño de interfaces declarativas utilizando **Jetpack Compose**.
- Implementar almacenamiento local y migraciones con **Room**.
- Manejar tareas complejas en segundo plano y notificaciones asíncronas utilizando **WorkManager** y **Foreground Services**.
- Diseñar algoritmos de sincronización de datos P2P offline y resolución de conflictos.

### Índice de Documentación Técnica
Explora nuestra documentación técnica completa para entender cómo funciona Prima-Focus internamente:
- [Arquitectura del Sistema](docs/architecture.md)
- [Esquema de Base de Datos v5](docs/database_schema.sql)
- [Notas de Implementación](docs/implementation_notes.md)
- [Lógica de Prioridades](docs/priority-logic.md)
- [Flujo de Notificaciones](docs/notification_flow.md)
- [Especificaciones de UI](docs/ui_spec.md)
- [Lecciones: P2P Sync, Tombstones y Clock Drift](docs/learning/p2p-sync-tombstones-clockdrift-gc.md)
- [Todos los Aprendizajes y Decisiones](docs/learning/)
