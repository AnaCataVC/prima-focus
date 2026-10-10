# UI & Architecture Learning: Compose Canvas Confetti & Card Border Rendering

**Date:** October 2026  
**Related Components:** `LongPendingCelebrationModal`, `LongPendingCelebrationCalculator`, `ConfettiCanvas`, `HomeScreen`, `TaskListScreen`, `SettingsScreen`

---

## 1. Context & Motivation

In Prima Focus v2.1.0, two complementary visual and behavioral improvements were introduced:
1. **Long-Pending Task Celebrations**: A positive reinforcement mechanism that celebrates the completion of tasks that have remained pending for an extended duration (default: 30 days, configurable to 14, 30, 60, or 90 days).
2. **Card Border & Corner Clipping Invariant**: Addressing visual artifacts where rounded cards appeared with cut-off borders or disappearing corner strokes.

---

## 2. Long-Pending Task Celebration Architecture

### 2.1. Cleanroom Contract-Driven Logic in `:shared`
To ensure complete portability, testability, and decoupling from Android UI frameworks, the celebration determination logic was designed via a formal contract (`docs/contracts/long-pending-task-celebration.contract.md`) and implemented in `:shared`:
- **`LongPendingCelebrationCalculator`**:
  - `calculatePendingDays(createdAtEpochMs, nowEpochMs)`: Computes whole 24-hour days elapsed with clock-skew safety (returns 0 if `now <= createdAt`).
  - `formatPendingDuration(days)`: Formats elapsed time into natural Spanish duration strings (`"14 días"`, `"1 mes"`, `"2 meses"`, `"1 año"`).
  - `shouldTriggerCelebration(...)`: Evaluates whether the celebration should fire based on user preference toggle, non-recurrence invariant (recurring tasks never celebrate), positive threshold, and elapsed duration.
- Covered by 100% passing cleanroom black-box unit tests in `LongPendingCelebrationCalculatorTest`.

### 2.2. Zero-Dependency Compose Canvas Particle Engine (`ConfettiCanvas`)
Instead of pulling in third-party libraries (e.g. `konfetti-compose`), which introduce dependency churn, Kotlin compiler incompatibilities, and extra APK bloat, we built an in-engine particle system:
- **Frame-Accurate Animation**: Driven by `LaunchedEffect` and `withFrameNanos`, measuring exact delta times (`dt`) capped at 50ms to prevent quantum jumps during frame drops.
- **Particle Dynamics**: Simulates gravity, sway (`sin(y * 12 + phase)`), horizontal spread, and 2D rotation.
- **Palette Alignment**: Renders ribbons and disks using the theme's `LocalPremiumGlows` celebratory colors (`PrimaryRose`, `RoseGlow`, `AccentSage`, warm gold, lavender, soft cyan).
- **Lifecycle & Memory Safety**: Particles are scoped to the modal's `Box`. When dismissed or auto-dismissed after 6 seconds, the coroutine terminates immediately and the particle list is garbage collected with zero memory leaks.

---

## 3. Jetpack Compose Card Border & Corner Clipping Invariant

### 3.1. The Root Cause of Disappearing Card Borders
Two distinct layout bugs previously caused cards in `HomeScreen`, `TaskListScreen`, and `SettingsScreen` to lose their borders on rounded corners or along the bottom/right edges:

1. **Incorrect Modifier Chaining Order**:
   ```kotlin
   // ANTI-PATTERN: Clips outer border stroke
   Modifier
       .clip(RoundedCornerShape(16.dp))
       .background(glows.glassSurface)
       .border(1.dp, brush, RoundedCornerShape(16.dp))
   ```
   `clip(shape)` restricts the canvas draw bounds to the geometric interior of the shape. When `border(1.dp, ...)` is called subsequently, half the stroke width (0.5dp) lies outside the clipping boundary and gets discarded by the anti-aliasing engine.

2. **Linear Gradient Fade to Zero (`0x00FFFFFF`)**:
   `GlassBorderEnd` was previously configured as transparent (`Color(0x00FFFFFF)`). For linear gradients drawn diagonally (top-left to bottom-right), the bottom and right edges of the card received 0% opacity, creating the visual defect of missing borders.

### 3.2. Standardized Remediation Pattern
All glass cards now adhere strictly to the canonical sequence:
```kotlin
// CANONICAL PATTERN: Crisp border and protected content
val itemShape = RoundedCornerShape(16.dp)
Modifier
    .background(glows.glassSurface, itemShape)
    .border(width = 1.dp, brush = borderBrush, shape = itemShape)
    .clip(itemShape)
```
Additionally, `GlassBorderEnd` and `LightGlassBorderEnd` maintain a minimum alpha floor (8% to 10% opacity) in `Color.kt` and `Theme.kt`, ensuring all four corners remain distinctly delineated while preserving directional lighting.
