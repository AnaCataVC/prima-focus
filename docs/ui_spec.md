# Wireframe Scope (Textual)

Visual specifications ready for implementation in Native Android and PWA (Desktop). Includes 4 key screens + settings/recurrence screen, visual specifications (layout, sizes, suggested colors), microinteractions, states, and exact copy for each control, aligned with the data model and priority rules.

## Visual Conventions and Tokens
- **Typography**: Roboto (Android) / Inter (Web).
- **Scales**: Titles H1 24sp/20px, H2 18sp/16px, body 14sp/14px, micro 12sp/12px.
- **Suggested Colors**: Primary #0A84FF (blue), Accent #34C759 (green), Bg #FFFFFF, Surface #F6F7FB, TextPrimary #0B1226, Muted #6B7280, Error #FF3B30.
- **Iconography**: Material Icons; quick actions: Start (play), Pause, Check, Snooze (clock), Edit (pencil).
- **Touch targets**: minimum 48×48 dp / px.
- **Animations**: 180–240 ms for micro-transitions; 600 ms for confetti/celebration.

## Screen 1: Ultra-Fast Inbox (modal / FAB)
**Goal**: capture in ≤3s via text or voice.
### Android modal (mobile)
- **Header**: small handle bar (drag to dismiss).
- **Top Row**: Mic icon (48dp circle) on the left; centered text field placeholder "Anotar en 2s"; quick date button on the right "Hoy" / "Sin fecha" (chip).
- **Hidden Fields**: subtasks, estimatedMinutes, category (accordion). Show only if the user taps "Más detalles".
- **Actions**: Guardar (Primary, blue, 56dp pill) and Cancelar (text).
- **Microinteraction**: tap mic -> show waveform and real-time transcription; if transcription is empty after 2s, save as "Nota rápida X".
- **Shortcut**: FAB on all screens bottom right (icon + label "Inbox").
### PWA modal (desktop)
- **Layout**: centered modal 640×360 px; mic and text field inline; date and category as chips; Save button on the right.
- **Exact Microcopy**: Placeholder: "Anotar en 2s", Save: "Añadir", Date chips: "Hoy", "Sin fecha".

## Screen 2: Today Task Home (main screen)
**Goal**: show exactly 1 priority task with minimal steps and a large Start button.
### Android (mobile)
- **Top bar**: date (e.g., Viernes 12 jun) and day progress (1/3 completadas) in micro text.
- **Central Card (16dp padding card)**:
  - Title H1 (single line, ellipsis).
  - Meta row: category icon + subcategory label; estimatedMinutes chip; priority badge (color based on score: ≥70 red/orange; 40–69 blue; <40 gray).
  - Subtasks preview: up to 3 inline checkboxes (small). Text "3 pasos" if more. Each checkbox is tappable.
  - Start button: circular primary 72dp centered with Play icon and "Empezar" label below (bold).
  - Secondary actions row: Edit (pencil), Capture idea (plus), Snooze (clock icon).
- **Footer**: small timeline with next notification and Inbox FAB.
- **States**:
  - No tasks: show empty card with "Añadir tarea" CTA and microcopy "Tu Tarea Hoy aparecerá aquí".
  - IsProject true: show yellow banner "Proyecto grande — dividir en subtareas" with Auto-split button.
### PWA (desktop)
- **3-column Layout**: left short Inbox; center Today Task (large 640px card); right history and points.
- **Start**: launches timer in modal or right panel.
- **Exact Card Copy**: Title example: "Enviar informe trimestral", Subtext: "Trabajo · entrega · 45 min", Start label: "Empezar".

## Screen 3: Start Screen and Timer
**Goal**: initiate adapted Pomodoro, show progress and visible options.
### Android full screen
- **Header**: small task title and minimize button (minimize to persistent notification).
- **Center**: large circular counter (220dp diameter) with remaining time in H1. Circular progress bar around it.
- **Below**: current subtask (if applicable) and 2 large buttons: Pausa (outline) and Terminé (accent green).
- **Visible Options**: overflow menu with Adjust time (25/15/custom), Mute notifications, Abandon.
- **Auto-fallback**: if not started within 10 min of Start tap, suggest "¿Prefieres 15/3 en vez de 25/5?" with CTA Cambiar.
- **Foreground service**: persistence if app is backgrounded.
- **Microinteractions**: Upon tapping Terminé, 600 ms confetti animation + subtle sound; then open Quick Review modal.
### PWA
- Timer in right panel; browser notifications if tab is backgrounded.

## Screen 4: Post-Session Quick Review
**Goal**: 2 quick questions for feedback and logging.
- **Small modal**
- **Question 1**: ¿Completaste? buttons: Sí (green), Parcial (amber), No (gray).
- **Question 2**: ¿Cómo te sentiste? 3 tappable emojis (😐, 🙂, 😃) with micro labels.
- **Save Button (primary)** and small text "Se guardará en Historial".
- **If No or Partial**: show collapsible quick tip "¿Quieres posponer o dividir en subtareas?" with Snooze / Split buttons.
- **Saved Data**: create Session and update task.status if completed; if recurring, create next instance.

## Screen 5: Recurrence and Priority Settings
**Goal**: create recurrences, adjust manualBoost and nonPostponable rules.
- **Recurrence Section**: RRULE input with presets: Daily, Weekly, Monthly, Custom. Show human readable summary.
- **Auto-split toggle** (ask on create) — default off.
- **ManualBoost slider** (0–30) default 5. Show effect on estimated score in real-time.
- **NonPostponable toggles** per subcategory (prechecked for health→medication and paperwork→urgent).
- **Save changes**.

## Microinteractions and Critical States
- **Sync state**: small cloud icon top bar: green (synced), orange (syncing), red (error). Tap shows last sync time and retry.
- **Conflict UI**: toast + small banner "Conflicto detectado en [título]" with actions View changes / Keep local / Accept remote.
- **Notification actions**: Start / Snooze 1h / Mute 1h. If nonPostponable, Snooze disabled.
- **Accessibility**: all buttons have contentDescription; color contrast WCAG AA; support large fonts and TalkBack/VoiceOver labels.

## Developer Specifications (handoff)
- **Assets**: provide SVG icons (play, pause, snooze, edit, confetti).
- **Spacing**: base grid 8dp. Card padding 16dp.
- **API hooks**: `TaskRepository.getTodayTask()`, `SessionManager.startSession()`, `SchedulerService.scheduleRemindersForTask(taskId)`.
- **Edge cases**: empty state copy, offline flows, background timer persistence, browser permission fallbacks.

## Pre-Implementation Checklist
- [x] Implement unique listener for Today Task to minimize reads.
- [x] Foreground service in Android for timer.
- [x] Service Worker + Notifications API in PWA with fallback UI.
- [x] Confirm manualBoost UI and default value 5.
- [x] NonPostponable defaults applied for medication and urgent paperwork.
- [x] Auto-split prompt on create when estimatedMinutes > 120.
