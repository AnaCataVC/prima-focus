# Prima-Focus

![Platform Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android)
![Architecture Local-First](https://img.shields.io/badge/Architecture-Local--First%20(KMP)-blue?style=flat)
![Room Database v7](https://img.shields.io/badge/Room-v7%20(SQLite)-4285F4?style=flat&logo=sqlite&logoColor=white)
![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform%202.2.10-0095D5?style=flat&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203-4285F4?style=flat&logo=android&logoColor=white)

*English version below | Versión en español abajo*

---

## English

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

## Español

### Descripción del Proyecto
Prima-Focus es una aplicación de gestión de tareas y enfoque profundo "local-first" diseñada para ayudarte a concentrarte en lo que realmente importa. Construida como una **aplicación móvil nativa para Android** con lógica de dominio compartida mediante **Kotlin Multiplatform**, utiliza un avanzado sistema predictivo de puntuación de prioridad para seleccionar dinámicamente tu "Tarea de Hoy". Todos los datos se almacenan localmente en cada dispositivo para garantizar máxima privacidad y rendimiento, con sincronización local peer-to-peer (P2P) mediante Google Nearby Connections sin necesidad de servidores en la nube.

### Puntos Destacados de la Arquitectura
- **Experiencia Insignia en Android (`:app`)**: App nativa completa con base de datos Room v7, diseño Material 3 Glassmorphism, widgets interactivos con Jetpack Glance y recordatorios en segundo plano con WorkManager.
- **Núcleo Modular KMP (`:shared`)**: Motor de prioridad unificado (`SharedPriorityEngine`), cálculo de recurrencias, ventanas de descanso y resolución determinista *Last-Write-Wins* (LWW).
- **Sincronización P2P Flexible Anfitrión / Cliente (Nearby Connections)**:
  - *Móvil a Móvil*: Sincronización directa peer-to-peer con selección explícita del usuario entre modo **Anfitrión ("Ser Anfitrión")** y **Cliente ("Ser Cliente")**, con temporizador de desconexión automática a los 45 segundos para ahorro de batería, límite de seguridad de 5 MB por transferencia y confirmación por dígitos numéricos en ambos dispositivos antes de emparejar.
- **Base de Datos Room v7 (Android)**: Esquema relacional limpio y optimizado con `MIGRATION_6_7` que agrega `missedPolicy` por tarea para las recurrencias saltadas. Soporte completo de borrado lógico (*Soft Deletes* con lápidas `isDeleted`, `deletedAt`) y `syncVersion` incremental para evitar la resurrección de tareas borradas.
- **Resolución de Conflictos LWW Inmune al Clock Drift**: Algoritmo que prioriza la versión lógica antes que el reloj del sistema, tolerando cualquier desfase horario entre dispositivos.
- **No-Regresión de Tareas Completadas**: Las tareas finalizadas permanecen completadas durante el merge independientemente de diferencias en la hora local.
- **Purga Automática de 30 Días**: Recolección de basura (*Garbage Collection*) en SQLite que elimina permanentemente lápidas antiguas al iniciar la app.
- **Diseño Adaptativo en Android**: Interfaz declarativa en Jetpack Compose con soporte para teléfonos, vista dividida con calendario interactivo en tabletas, y `NavigationRail` en anchos medianos/expandidos.
- **Emojis por Categoría**: Cada categoría de tarea muestra un emoji configurable (selector curado + campo de texto libre) en Home, Lista, Inbox y ambos widgets.
- **Tema Adaptativo**: Modo Sistema/Claro/Oscuro más color dinámico Material You (Android 12+), aplicado en vivo sin reiniciar la app.
- **Recurrencias Salteadas**: Un ajuste global y una anulación por tarea deciden si una tarea recurrente vencida (ej. medicación diaria) salta directo a la próxima ocurrencia en vez de acumular instancias atrasadas.
- **Sincronización Reforzada**: IDs deterministas para instancias recurrentes y deduplicación evitan spawns duplicados entre dispositivos, un ID de dispositivo estable desempata conflictos con igual versión, las categorías/tema/recurrencias salteadas se sincronizan junto a las tareas, y un resumen post-merge informa cuántas se recibieron/actualizaron/crearon.
- **Feedback Instantáneo en Widgets**: Completar una tarea desde un widget muestra un check optimista de inmediato y publica una notificación "Deshacer", sin esperar el refresco completo del widget.

### Tecnologías Utilizadas
- **Lenguajes y Frameworks**: Kotlin Multiplatform, Java 17 Toolchain
- **Interfaces Gráficas**: Jetpack Compose (Android), Jetpack Glance (App Widgets)
- **Persistencia Local**: Base de datos Room v7 (Android SQLite)
- **Red y Seguridad**: Google Nearby Connections API (Topología Star P2P)
- **Procesamiento en Segundo Plano**: WorkManager y Foreground Services

### Aprendizajes Clave
Este proyecto representó un gran hito de ingeniería y arquitectura. A lo largo del proceso aprendí a:
- Diseñar y construir una arquitectura modular en **Kotlin Multiplatform (KMP)** con capa de dominio desacoplada.
- Implementar bases de datos locales robustas en móvil (**Room v7**) con seguridad en migraciones y lápidas tombstones.
- Diseñar y someter a auditoría *Red Team* protocolos de red local peer-to-peer con límites de batería y tamaño de paquetes.
- Resolver desafíos de *Clock Drift*, lápidas tombstones y recolección de basura en sistemas distribuidos *local-first*.
- Dominar el diseño de interfaces declarativas con **Jetpack Compose** y widgets con **Jetpack Glance**.

### Índice de Documentación Técnica
Explora nuestra documentación técnica completa para entender cómo funciona Prima-Focus internamente:
- [Arquitectura del Sistema](docs/architecture.md)
- [Esquema de Base de Datos v7](docs/database_schema.sql)
- [Notas de Implementación](docs/implementation_notes.md)
- [Lógica de Prioridades](docs/priority-logic.md)
- [Stress-Test Desktop y Sync LAN](docs/external-references/desktop-kmp-sync-stress-test.md)
- [Flujo de Notificaciones](docs/notification_flow.md)
- [Especificaciones de UI](docs/ui_spec.md)
- [Lecciones: P2P Sync, Tombstones y Clock Drift](docs/learning/p2p-sync-tombstones-clockdrift-gc.md)
- [Todos los Aprendizajes y Decisiones](docs/learning/)
