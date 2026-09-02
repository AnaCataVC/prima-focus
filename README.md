# Prima-Focus

![Platform Android](https://img.shields.io/badge/Platform-Android%20%7C%20Desktop%20(PC)-3DDC84?style=flat&logo=android)
![Architecture Local-First](https://img.shields.io/badge/Architecture-Local--First%20(KMP)-blue?style=flat)
![Room Database v6](https://img.shields.io/badge/Room-v6%20%2B%20SQLite%20JDBC-4285F4?style=flat&logo=sqlite&logoColor=white)
![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform%202.2.10-0095D5?style=flat&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Compose-Multiplatform%20Ready-4285F4?style=flat&logo=android&logoColor=white)

*English version below | Versión en español abajo*

---

## English

### Project Description
Prima-Focus is a local-first task management and deep work application designed to help you focus on what truly matters. Built primarily as a **native Android mobile application** (and extended with a desktop workstation companion client via **Kotlin Multiplatform**), it uses an advanced predictive priority scoring system to dynamically select your "Today Task." All data is stored locally on each device for maximum privacy and performance, featuring hardened local LAN / P2P synchronization with cryptographic authentication without requiring any cloud backend.

### Key Architectural Highlights
- **Native Android Core (`:app`)**: The flagship mobile experience featuring Room Database v6 persistence, Material 3 Glassmorphism UI, Jetpack Glance Home Screen Widgets, and WorkManager background reminders.
- **Modular KMP Core (`:shared`)**: Unified predictive priority scoring (`SharedPriorityEngine`), recurrence projecting, quiet hours scheduling, and Last-Write-Wins (LWW) conflict resolution logic.
- **Companion Desktop Client (`:desktop`)**: Workstation productivity client for PC with independent SQLite storage, fast keyboard shortcuts (`Ctrl+N`, `Ctrl+Enter`), and offline JSON backup tools.
- **Hardened Local LAN Sync (Android & Desktop Companion)**: Secure local Wi-Fi synchronization powered by an embedded HTTP sync server (`DesktopSyncServer`, port 8765) and Android client (`LanSyncClient`). Features mutual authentication via **6-digit PIN hashing**, per-request **HMAC-SHA256 payload signatures**, and rate-limiting brute force protection (lockout after 5 failed attempts with automated PIN rotation).
- **Flexible Host / Client Mode Selection**:
  - *Mobile-to-Mobile (Android Nearby Connections)*: Seamless peer-to-peer Wi-Fi Direct and BLE synchronization with explicit user mode selection between **Host ("Ser Anfitrión")** and **Client ("Ser Cliente")**, backed by a 45-second auto-timeout for battery conservation.
  - *Mobile-to-Desktop (LAN Sync)*: Desktop workstation functions as the **Host / Sync Server**, broadcasting its local IP and 6-digit pairing PIN, while the Android mobile client initiates authenticated bidirectional synchronization.
- **Room Database v6 (Android)**: Clean, streamlined relational schema with `MIGRATION_5_6` permanently pruning legacy unused events. Full support for Soft Deletes (Tombstones: `isDeleted`, `deletedAt`) and monotonic `syncVersion` to prevent deleted items from resurrecting.
- **Clock-Drift Resilient LWW**: Advanced conflict resolution that prioritizes logical version increments over system clocks, immunizing sync against device time skew.
- **Non-Regressive Task Completion**: Completed tasks are guaranteed to remain completed during merges regardless of time drift.
- **Automatic 30-Day Tombstone Purge**: Built-in SQLite Garbage Collection that cleans up deleted tombstones older than 30 days upon startup.
- **Adaptive Android Layouts**: Responsive UI using Jetpack Compose providing Split Views and interactive calendars on tablets.

### Technologies Used
- **Languages & Frameworks**: Kotlin Multiplatform, Java 17 Toolchain
- **UI Toolkits**: Jetpack Compose (Android), Jetpack Glance (App Widgets), Swing / Desktop UI
- **Local Persistence**: Room Database v6 (Android) & SQLite JDBC (Desktop)
- **Networking & Security**: Google Nearby Connections, Embedded HTTP Sync Server, HMAC-SHA256, SHA-256 PIN Hashing
- **Background Processing**: WorkManager & Foreground Services

### Key Learnings
This project was a major architectural milestone. Throughout the process, I learned how to:
- Architect and build a modular **Kotlin Multiplatform (KMP)** project with pure shared domain logic.
- Implement independent local databases on mobile (**Room v6**) and desktop (**SQLite JDBC**).
- Design and red-team stress-test local network protocols with cryptographic authentication (HMAC/PIN) and brute-force defenses.
- Handle clock-drift resilience, soft-delete tombstones, and garbage collection in distributed local-first systems.
- Master declarative UI design using **Jetpack Compose** and home screen widgets with **Jetpack Glance**.

### Documentation Index
Explore our comprehensive technical documentation to understand how Prima-Focus works under the hood:
- [System Architecture](docs/architecture.md)
- [Database Schema v6](docs/database_schema.sql)
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
Prima-Focus es una aplicación de gestión de tareas y enfoque profundo "local-first" diseñada para ayudarte a concentrarte en lo que realmente importa. Construida primordialmente como una **aplicación móvil nativa para Android** (y extendida con un cliente companion de escritorio mediante **Kotlin Multiplatform**), utiliza un avanzado sistema predictivo de puntuación de prioridad para seleccionar dinámicamente tu "Tarea de Hoy". Todos los datos se almacenan localmente en cada dispositivo para garantizar máxima privacidad y rendimiento, con sincronización local LAN / P2P blindada criptográficamente sin necesidad de servidores en la nube.

### Puntos Destacados de la Arquitectura
- **Experiencia Insignia en Android (`:app`)**: App nativa completa con base de datos Room v6, diseño Material 3 Glassmorphism, widgets interactivos con Jetpack Glance y recordatorios en segundo plano con WorkManager.
- **Núcleo Modular KMP (`:shared`)**: Motor de prioridad unificado (`SharedPriorityEngine`), cálculo de recurrencias, ventanas de descanso y resolución determinista *Last-Write-Wins* (LWW).
- **Cliente Companion de Escritorio (`:desktop`)**: Aplicación para PC con almacenamiento SQLite local independiente, atajos de teclado rápidos (`Ctrl+N`, `Ctrl+Enter`) y herramientas de respaldo JSON.
- **Sincronización LAN Local Blindada (Android y Desktop Companion)**: Sincronización segura por red Wi-Fi local mediante servidor HTTP embebido (`DesktopSyncServer`, puerto 8765) y cliente Android (`LanSyncClient`). Protegida por **emparejamiento con PIN de 6 dígitos**, **firmas criptográficas HMAC-SHA256** por petición y protección contra fuerza bruta (bloqueo tras 5 intentos fallidos y regeneración automática del PIN).
- **Selección Flexible de Modo Anfitrión / Cliente (Host / Client)**:
  - *Móvil a Móvil (Nearby Connections Android)*: Sincronización directa peer-to-peer con selección explícita del usuario entre modo **Anfitrión ("Ser Anfitrión")** y **Cliente ("Ser Cliente")**, con temporizador de desconexión automática a los 45 segundos para ahorro de batería.
  - *Móvil a Escritorio (Sincronización LAN)*: La aplicación de PC actúa como **Anfitrión (Servidor)** mostrando su dirección IP local y PIN de 6 dígitos, mientras que el móvil Android se conecta como **Cliente** para realizar el intercambio bidireccional atómico.
- **Base de Datos Room v6 (Android)**: Esquema relacional limpio y optimizado con `MIGRATION_5_6` que elimina permanentemente tablas obsoletas sin uso (`events`). Soporte completo de borrado lógico (*Soft Deletes* con lápidas `isDeleted`, `deletedAt`) y `syncVersion` incremental para evitar la resurrección de tareas borradas.
- **Resolución de Conflictos LWW Inmune al Clock Drift**: Algoritmo que prioriza la versión lógica antes que el reloj del sistema, tolerando cualquier desfase horario entre dispositivos.
- **No-Regresión de Tareas Completadas**: Las tareas finalizadas permanecen completadas durante el merge independientemente de diferencias en la hora local.
- **Purga Automática de 30 Días**: Recolección de basura (*Garbage Collection*) en SQLite que elimina permanentemente lápidas antiguas al iniciar la app.
- **Diseño Adaptativo en Android**: Interfaz declarativa en Jetpack Compose con soporte para teléfonos y vista dividida con calendario interactivo en tabletas.

### Tecnologías Utilizadas
- **Lenguajes y Frameworks**: Kotlin Multiplatform, Java 17 Toolchain
- **Interfaces Gráficas**: Jetpack Compose (Android), Jetpack Glance (App Widgets), Swing / Desktop UI
- **Persistencia Local**: Base de datos Room v6 (Android) y SQLite JDBC (Escritorio)
- **Red y Seguridad**: Google Nearby Connections, Servidor HTTP Embebido, HMAC-SHA256, Hash SHA-256 para PIN
- **Procesamiento en Segundo Plano**: WorkManager y Foreground Services

### Aprendizajes Clave
Este proyecto representó un gran hito de ingeniería y arquitectura. A lo largo del proceso aprendí a:
- Diseñar y construir una arquitectura modular en **Kotlin Multiplatform (KMP)** con capa de dominio desacoplada.
- Implementar bases de datos locales independientes en móvil (**Room v6**) y escritorio (**SQLite JDBC**).
- Diseñar y someter a auditoría *Red Team* protocolos de red local protegidos con firmas criptográficas (HMAC/PIN) y mitigación de fuerza bruta.
- Resolver desafíos de *Clock Drift*, lápidas tombstones y recolección de basura en sistemas distribuidos *local-first*.
- Dominar el diseño de interfaces declarativas con **Jetpack Compose** y widgets con **Jetpack Glance**.

### Índice de Documentación Técnica
Explora nuestra documentación técnica completa para entender cómo funciona Prima-Focus internamente:
- [Arquitectura del Sistema](docs/architecture.md)
- [Esquema de Base de Datos v6](docs/database_schema.sql)
- [Notas de Implementación](docs/implementation_notes.md)
- [Lógica de Prioridades](docs/priority-logic.md)
- [Stress-Test Desktop y Sync LAN](docs/external-references/desktop-kmp-sync-stress-test.md)
- [Flujo de Notificaciones](docs/notification_flow.md)
- [Especificaciones de UI](docs/ui_spec.md)
- [Lecciones: P2P Sync, Tombstones y Clock Drift](docs/learning/p2p-sync-tombstones-clockdrift-gc.md)
- [Todos los Aprendizajes y Decisiones](docs/learning/)
