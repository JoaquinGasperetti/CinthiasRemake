package com.hkemtrentainment.cinthiasquestr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.hkemtrentainment.cinthiasquestr.navigation.Navigator
import com.hkemtrentainment.cinthiasquestr.navigation.Screen
import com.hkemtrentainment.cinthiasquestr.navigation.rememberNavigationState
import com.hkemtrentainment.cinthiasquestr.navigation.toEntries
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.GameBoardScreen
import com.hkemtrentainment.cinthiasquestr.ui.mainmenu.MainMenuScreen
import com.hkemtrentainment.cinthiasquestr.ui.theme.CinthiasQuestTheme
import com.hkemtrentainment.cinthiasquestr.ui.worldmap.WorldMapScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            val navigationState = rememberNavigationState(
                startRoute = Screen.MainMenu,
                topLevelRoutes = setOf(Screen.MainMenu)
            )
            val navigator = remember { Navigator(navigationState) }

            val entryProvider = entryProvider<NavKey> {
                entry<Screen.MainMenu> {
                    MainMenuScreen(
                        onPlayClick = { navigator.navigate(Screen.WorldMap) },
                        onQuickPlayLevelClick = { levelNumber ->
                            navigator.navigate(Screen.GameBoard(level = levelNumber))
                        }
                    )
                }
                entry<Screen.WorldMap> {
                    WorldMapScreen(
                        onBackClick = { navigator.goBack() },
                        onLevelSelect = { levelNumber ->
                            navigator.navigate(Screen.GameBoard(level = levelNumber))
                        }
                    )
                }
                entry<Screen.GameBoard> { key ->
                    GameBoardScreen(
                        selectedLevel = key.level,
                        onPauseClick = { navigator.goBack() },
                        onNextLevelClick = { nextLevel ->
                            navigator.goBack()
                            navigator.navigate(Screen.GameBoard(level = nextLevel))
                        },
                        onWorldMapClick = {
                            navigator.goBack()
                        }
                    )
                }
            }

            CinthiasQuestTheme {
                NavDisplay(
                    entries = navigationState.toEntries(entryProvider),
                    onBack = { navigator.goBack() }
                )
            }
        }
    }
}
