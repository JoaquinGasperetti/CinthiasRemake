# Fix Victory Dialog appearing immediately

The Victory Dialog appears immediately because the `GameViewModel` is being reused across navigation sessions, and its state (specifically `isGameWon`) is not reset when the user starts a new game. Additionally, the `GameBoard` was defined as a top-level route in a `rememberNavigationState` call, which means its backstack (and associated ViewModels) persists as long as the activity is alive.

## Proposed Changes

### Navigation

#### [MODIFY] [NavigationState.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/navigation/NavigationState.kt)
- Add `rememberViewModelStoreNavEntryDecorator` to the navigation decorators list. This ensures each `NavEntry` has its own `ViewModelStoreOwner`, allowing ViewModels to be cleared when their entry is removed from the backstack.

#### [MODIFY] [MainActivity.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/MainActivity.kt)
- Remove `Screen.GameBoard` from the `topLevelRoutes` set in `rememberNavigationState`. This ensures that navigating to `GameBoard` adds a new entry to the `MainMenu` backstack, which will be popped (and its ViewModel cleared) when the user goes back to the menu.

### Game Board

#### [MODIFY] [GameViewModel.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/gameboard/GameViewModel.kt)
- Ensure all game state is reset in the constructor or via an explicit reset method if needed. (Though with scoped ViewModels, a new instance will be created anyway).

## Verification Plan

### Automated Tests
- Build the project using `./gradlew :app:assembleDebug` to ensure no compilation errors.

### Manual Verification
- Verify that clicking "Play" from the main menu always starts a fresh game.
- Verify that winning a game, going back to the menu, and clicking "Play" again starts a fresh game instead of showing the victory dialog immediately.
