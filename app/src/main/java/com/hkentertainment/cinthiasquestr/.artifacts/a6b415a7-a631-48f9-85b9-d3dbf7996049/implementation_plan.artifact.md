# Implementation Plan - GameBoard Logic Overhaul

Rewrite the core logic of "Cinthia's Quest" to fix stability and race conditions using a robust state machine and clear grid separation.

## Proposed Changes

### [Game Logic]

#### [MODIFY] [GamePiece.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/gameboard/GamePiece.kt)
- Update `GamePiecePowerUp` to simplify to `LINE_BLASTER`, `COLOR_BOMB`, and `EXPLOSIVE_BOMB`.

#### [MODIFY] [GameViewModel.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/gameboard/GameViewModel.kt)
- **Total Rewrite**:
    - Implement `GameState` enum: `IDLE`, `SWAPPING`, `RESOLVING`, `DROPPING`, `SHUFFLING`.
    - Maintain a private `MutableList<MutableList<GamePiece>>` for the logical board.
    - Expose a `StateFlow<List<List<GamePiece>>>` for the UI.
    - Implement `onSwipe` with strict state checks.
    - Match detection logic for lines and intersections (L/T shapes).
    - Special piece spawning at swap origin.
    - Recursive cascade/gravity logic with synchronized `delay()` calls.
    - Deadlock detection and shuffle logic.

#### [MODIFY] [GameBoardScreen.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/gameboard/GameBoardScreen.kt)
- Update to observe the new `BoardState` and pieces.
- Ensure animations match the `delay()` timings in the ViewModel.
- Maintain existing "Juice" (haptics, shake) during the cascade.

## Verification Plan

### Automated Tests
- I will verify the build is successful after the overhaul.
- I will run `./gradlew :app:assembleDebug` to ensure no syntax errors.

### Manual Verification
- Visual inspection of the code to ensure all logic requirements (L/T shapes, special pieces, state machine) are met.
