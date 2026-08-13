# Cinthia's Quest Initialization Plan

This plan initializes the "Cinthia's Quest" project with the correct package name, Navigation 3, and Compose Material Adaptive.

## User Review Required

> [!IMPORTANT]
> The package name will be corrected from `com.hkemtrentainment.cinthiasquestr` to `com.hkentertainment.cinthiasquestr`.

## Proposed Changes

### Build Configuration

#### [MODIFY] [settings.gradle.kts](file:///H:/CinthiasRemake/settings.gradle.kts)
- Update root project name to "Cinthia's Quest".

#### [MODIFY] [build.gradle.kts (app)](file:///H:/CinthiasRemake/app/build.gradle.kts)
- Update `namespace` and `applicationId` to `com.hkentertainment.cinthiasquestr`.

### Core UI & Navigation

#### [MODIFY] [MainActivity.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/MainActivity.kt)
- Move to new package.
- Implement Navigation 3 setup.

#### [NEW] [NavKey.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/navigation/NavKey.kt)
- Define navigation routes using `@Serializable`.

### Screens & ViewModels

#### [NEW] [MainMenuScreen.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/mainmenu/MainMenuScreen.kt)
- Implement Main Menu with Play/Shop buttons and AdMob placeholder.

#### [NEW] [MainMenuViewModel.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/mainmenu/MainMenuViewModel.kt)
- ViewModel for Main Menu state.

#### [NEW] [GameBoardScreen.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/gameboard/GameBoardScreen.kt)
- Implement Game Board with `GameTopBar`.

#### [NEW] [GameViewModel.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/gameboard/GameViewModel.kt)
- ViewModel for Game state (Score, Moves).

#### [NEW] [GameTopBar.kt](file:///H:/CinthiasRemake/app/src/main/java/com/hkentertainment/cinthiasquestr/ui/gameboard/components/GameTopBar.kt)
- Reusable component for the Game HUD.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure compilation.

### Manual Verification
- Verify Navigation 3 transitions in Compose Previews.
