# Research & Technical Reference: Compose Native Confetti & Card Border Rendering

## 1. Executive Summary

This reference document synthesizes the technical requirements and architecture for two related visual features in Prima Focus:
1. **Long-Pending Task Celebration with Native Jetpack Compose Confetti**: Rewarding the completion of tasks that have been pending for longer than a configurable threshold (default: 30 days) with an engaging celebratory modal and particle effect.
2. **Card Border & Corner Clipping Invariant in Jetpack Compose**: Resolving the visual anomaly where rounded card corners appear cut off or missing borders due to Modifier chaining order and transparent gradient endpoints.

---

## 2. Jetpack Compose Particle System (Native Confetti)

### 2.1. Dependency Evaluation vs. Native Implementation
- **Alternative A: Third-Party Libraries (`nl.dionsegijn:konfetti-compose`)**:
  - *Pros*: Pre-packaged particle presets.
  - *Cons*: Adds external transitive dependencies, potential version mismatch with Compose Multiplatform / newer Kotlin compiler versions, binary footprint increase, and lacks tight coupling with Prima Focus's custom `LocalPremiumGlows` color palette.
- **Alternative B: Native Compose Canvas Particle Engine (Selected)**:
  - *Pros*:
    - **Zero dependencies**: No new Gradle artifacts.
    - **Performance**: High frame rate using Compose `Canvas` and `withFrameNanos` loop.
    - **Design Consistency**: Seamless integration with `LocalPremiumGlows` (rose glow, primary accent, sage green, and soft amber).
    - **Memory safety**: Self-contained lifecycle tied to `LaunchedEffect`, automatically terminating and garbage-collecting particle pools upon completion or dismissal.

### 2.2. Physics & Particle Dynamics
Each particle contains:
- Coordinates: `x, y` normalized or screen-relative.
- Velocity: Initial upward explosion velocity `vy < 0` with horizontal spread `vx`, modified per frame by gravity `g` and air resistance friction `k`.
- Rotation: 2D angle `theta` and 3D flip ratio `cos(phi)` to simulate tumbling paper strips.
- Geometry: Varied mix of confetti ribbons (rectangles) and disks (circles).
- Color: Random sampling from the active theme's celebratory palette (`PrimaryRose`, `RoseGlow`, `AccentSage`, `Color(0xFFFBBF24)`).

---

## 3. Card Border Clipping & Gradient Alpha Invariants

### 3.1. The Root Cause of Missing Card Corners
In Jetpack Compose, the modifier chain order dictates the drawing and clipping pipeline:
```kotlin
// PROBLEMATIC PATTERN
Modifier
    .clip(RoundedCornerShape(18.dp))   // 1. Clips draw canvas to exact shape
    .background(glows.glassSurface)     // 2. Fills clipped canvas
    .border(                            // 3. Draws stroke; outer half clipped!
        width = 1.dp,
        brush = Brush.linearGradient(listOf(glows.glassBorderStart, glows.glassBorderEnd)),
        shape = RoundedCornerShape(18.dp)
    )
```
1. **Clipping the Stroke**: `clip(shape)` limits the layout's drawing bounds to the interior of `shape`. When `border(1.dp, ..., shape)` is invoked afterward, the anti-aliased outer curve of the border stroke falls outside the clipped area, truncating the corners.
2. **Gradient Fade to Zero**: `glassBorderEnd` was defined as `Color(0x00FFFFFF)` (100% transparent). In a top-left to bottom-right linear gradient, the bottom and right corners received 0% opacity, creating an optical illusion of missing borders.

### 3.2. Remediation Standard
1. **Chaining Order**:
```kotlin
// CANONICAL CORRECT PATTERN
Modifier
    .background(glows.glassSurface, shape) // Background adheres to shape
    .border(width = 1.dp, brush = brush, shape = shape) // Border stroke drawn unclipped
    .clip(shape) // Only clips inner child elements to prevent overflow
```
2. **Visible Border Gradient Floor**:
   Update `GlassBorderEnd` to retain a subtle, perceptible alpha floor (e.g. 8-12% opacity soft rose/white) rather than complete transparency, ensuring all 4 corners and bottom/right boundaries remain crisp while preserving the directional glass glow.

---

## 4. Settings & Storage Specifications

- **Preference Key**: `Constants.PREF_OLD_TASK_CELEBRATION_ENABLED` (`Boolean`, default: `true`).
- **Preference Key**: `Constants.PREF_OLD_TASK_THRESHOLD_DAYS` (`Int`, default: `30`).
- **Evaluation Logic**:
  `val isOldTask = !task.isRecurring && (System.currentTimeMillis() - task.createdAt) >= (thresholdDays * 86_400_000L)`
