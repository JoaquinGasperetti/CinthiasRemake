# Manual Booster System Implementation Plan

This plan outlines the implementation of a manual booster system (Hammer, Dynamite, Shuffle) for "Cinthia's Quest", including state management, UI components, and interaction logic.

## Proposed Changes

### [GameBoard Backend]

#### [MODIFY] [GameViewModel.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/gameboard/GameViewModel.kt)
- Add `BoosterType` enum: `HAMMER`, `DYNAMITE`, `SHUFFLE`.
- Update `BoardState` to include `WAITING_FOR_BOOSTER_TARGET`.
- Update `GameUIState` to include:
    - `boosterInventory: Map<BoosterType, Int>`
    - `activeBooster: BoosterType?`
- Implement `onBoosterSelected(type: BoosterType)`:
    - If `SHUFFLE`: Consume 1, trigger shuffle, and resolve.
    - If `HAMMER`/`DYNAMITE`:
        - If already active: Cancel (refund, set to `IDLE`).
        - Else: Consume 1, set to `WAITING_FOR_BOOSTER_TARGET`, set `activeBooster`.
- Implement `onPieceTapped(row: Int, col: Int)`:
    - Only active during `WAITING_FOR_BOOSTER_TARGET`.
    - `HAMMER`: Destroy piece at `(row, col)`.
    - `DYNAMITE`: Destroy 3x3 area centered at `(row, col)`.
    - Trigger `resolveMatches` after destruction.
- Update `onSwipe` to ignore inputs when in targeting mode.

### [UI Components]

#### [MODIFY] [GameBoardScreen.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/gameboard/GameBoardScreen.kt)
- Implement `BoosterBar` horizontal component.
- Display booster buttons with icons (Hammer, Dynamite, Shuffle) and badges for counts.
- Add "Ad" icon fallback when count is 0 (logic for ads can be added later as TODO).
- Update `GameBoard` and `AnimatedGamePieceItem` to support tap detection in addition to swipes.
- Add a dimming overlay over the board when `boardState == WAITING_FOR_BOOSTER_TARGET`.

## Verification Plan

### Manual Verification
- Verify `SHUFFLE` booster works immediately and resets board.
- Verify `HAMMER` enters targeting mode, dims board, and destroys exactly one piece on tap.
- Verify `DYNAMITE` enters targeting mode, dims board, and destroys a 3x3 area on tap.
- Verify tapping the same booster twice cancels the action and refunds the count.
- Verify boosters do not decrease the `movesLeft` counter.
- Verify board resolves (gravity, matches) correctly after booster use.
