# Upgrade "Cinthia's Quest" to a Premium Experience

This plan outlines the refactoring of "Cinthia's Quest" to include a character integration, swipe-to-swap gestures, premium animations, and modern UI aesthetics.

## Proposed Changes

### 1. Character Integration & Game Logic
- **[MODIFY] [GamePiece.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/gameboard/GamePiece.kt)**: Define `CinthiaState` enum.
- **[MODIFY] [GameViewModel.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/gameboard/GameViewModel.kt)**:
    - Add `cinthiaState` to `GameState`.
    - Update `cinthiaState` based on game events (cheering on combos/matches of 4, worried on low moves, sad on game over).
    - Implement swipe direction calculation logic.

### 2. Swipe-to-Swap Gestures
- **[MODIFY] [GameBoardScreen.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/gameboard/GameBoardScreen.kt)**:
    - Replace click-to-swap with `Modifier.pointerInput` and `detectDragGestures`.
    - Trigger `viewModel.onSwipe` on gesture completion.

### 3. Premium Animations & UI Aesthetics
- **[MODIFY] [GameBoardScreen.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/gameboard/GameBoardScreen.kt)**:
    - Add `CinthiaCharacter` component with breathing animation.
    - Implement `AnimatedGemPiece` using `animateIntOffsetAsState`.
    - Add "Combo!" floating text animation.
    - Apply `LinearGradient`, `RoundedCornerShape`, and `Elevation` to the board and pieces.
- **[MODIFY] [Theme.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/theme/Theme.kt)** & **[Color.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/theme/Color.kt)**: Ensure dynamic colors and premium palette.

## Verification Plan

### Automated Tests
- Run existing unit tests (if any) and add new tests for swipe direction logic.
- `./gradlew :app:testDebugUnitTest`

### Manual Verification
- Launch the app and verify:
    - Swipe gestures swap pieces correctly.
    - Pieces animate smoothly during swaps.
    - Cinthia character reacts to game states with breathing animation.
    - Floating "Combo!" text appears on matches.
    - Modern aesthetics (gradients, gems) are visible.
