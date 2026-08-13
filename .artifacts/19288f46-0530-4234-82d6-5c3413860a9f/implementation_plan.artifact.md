# Fix Vertical Swap Animation

The user reported that vertical movement is "strange" (pieces move up and down) compared to the smooth horizontal movement. Investigation revealed that the nested loop structure in the `GameBoard` composable causes pieces to be recreated when they move between rows, resetting their animation state.

## Proposed Changes

### [UI] GameBoard Screen

#### [MODIFY] [GameBoardScreen.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/gameboard/GameBoardScreen.kt)
- Flatten the nested `forEachIndexed` loops in the `GameBoard` composable.
- Use a single `forEach` loop over a flattened list of pieces with their positions.
- This ensures that `key(piece.id)` calls are siblings, allowing Compose to track piece state correctly even when they move between different rows (vertical swaps and gravity).

### [Logic] Game ViewModel

#### [MODIFY] [GameViewModel.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/gameboard/GameViewModel.kt)
- Fix the non-unique "EMPTY" ID issue for matched pieces.
- Use unique IDs for pieces marked for removal to prevent key collisions in Compose during the resolution phase.
- Update `applyGravity` to correctly identify these marked pieces.

## Verification Plan

### Manual Verification
- Perform vertical swipes (UP/DOWN) and verify they are as smooth as horizontal swipes.
- Verify that horizontal swipes still work as expected.
- Verify that gravity animations (pieces falling down) are smooth and don't jitter or jump.
- Check for any UI glitches during match resolution and power-up spawning.
