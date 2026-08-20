# AGENTS.md — AI Agent Guidelines & Architecture Manual

This document serves as the operational manual, architecture reference, and workflow guide for AI coding agents operating within the **Prima Focus** repository.

---

## 1. Project Overview & Architecture

**Prima Focus** is a native Android Pomodoro & Focus session management application built with **Kotlin**, **Jetpack Compose**, **Room Database**, and **Android Notification Services**. It helps users manage deep work sessions, breaks, task priorities, and productivity statistics.

### System Architecture:
- **`app/android/`**:
  - `app/src/main/`: Jetpack Compose UI (Timer, Task Manager, Stats, Settings), ViewModels, Room Entities & DAOs.
  - `app/build.gradle.kts`: Android SDK configuration (`minSdk = 26`, `targetSdk = 34+`), Compose compiler, and signing configs.
- **`docs/`**: Comprehensive design documents, database schemas (`database_schema.sql`), notification flow charts (`notification_flow.md`), and priority logic.
- **`releases/`**: Compiled APK binaries (`prima-focus-vX.Y.Z.apk`).

---

## 2. Directory Structure

```text
prima-focus/
├── app/
│   └── android/                   # Native Android Studio / Gradle project
│       ├── app/
│       │   ├── src/main/          # Kotlin source code, resources, manifests
│       │   └── build.gradle.kts   # App-level dependencies & build rules
│       ├── gradle/                # Version catalogs and Gradle wrapper
│       └── build.gradle.kts       # Root project build script
├── design/                        # UI wireframes, tokens, and component specifications
├── docs/                          # Architecture, database schema, and notification guides
├── releases/                      # Compiled APK artifacts (gitignored)
└── README.md                      # Bilingual project documentation (EN/ES)
```

---

## 3. Mandatory Agent Rules & Directives

### 🌐 Language & Communication
- **Source Code**: All Kotlin code (classes, functions, variables, comments) MUST be in **English**.
- **User Chat**: Communicate with the user in **Spanish** unless requested otherwise.
- **Git Commits**: Use **Conventional Commits** in **English** (e.g., `feat: ...`, `fix: ...`, `docs: ...`, `refactor: ...`).
- **README**: Maintain bilingual documentation (English and Spanish).

### 🔒 Security & Privacy
- **Source Code Protection**: This is a private application repository. NEVER leak private source code into public repositories or documentation.
- **Path Privacy**: NEVER leak absolute user paths (e.g., `C:\Users\...`) into code, documentation, or commit messages. Always use relative paths (`app/android/...`).

### 💻 PowerShell Environment
- **Command Chaining**: NEVER use `&&` or `||` in terminal commands. Use `;` or separate sequential commands.
- **GitHub CLI Context**: Switch to personal account `AnaCataVC` (`gh auth switch -u AnaCataVC --hostname github.com 2>$null`).

---

## 4. Development & Build Commands (PowerShell)

### Build & Run Tests
```powershell
# Navigate to Android project directory
cd app/android

# Run unit tests
./gradlew testDebugUnitTest

# Run lint checks
./gradlew lintDebug
```

### Assemble Production APK
```powershell
# Build signed release APK
cd app/android; ./gradlew assembleRelease
```

---

## 5. Showcase Synchronization Protocol

Whenever significant updates, new releases, or architecture documentation changes are made:
1. **Showcase Repo Location**: Public showcase repository is hosted at `https://github.com/AnaCataVC/prima-focus-showcase` (sibling directory `../prima-focus-showcase`).
2. **Version Bump Verification**: Compare release versions against the latest published tag in the public Showcase repository.
3. **Artifact Transfer**: Copy the signed release APK (`app-release.apk`) to `../prima-focus-showcase/apk/` renamed to `prima-focus-vX.Y.Z.apk`.
4. **Documentation Synchronization**: Copy any updated architectural guides or database schemas from `docs/` to `../prima-focus-showcase/docs/`.
5. **Zero Source Code Leaks**: NEVER copy raw source code (`/app`) to the public Showcase repository.
