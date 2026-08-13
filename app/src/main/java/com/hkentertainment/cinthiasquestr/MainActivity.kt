package com.hkentertainment.cinthiasquestr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.hkentertainment.cinthiasquestr.navigation.Navigator
import com.hkentertainment.cinthiasquestr.navigation.Screen
import com.hkentertainment.cinthiasquestr.navigation.rememberNavigationState
import com.hkentertainment.cinthiasquestr.navigation.toEntries
import com.hkentertainment.cinthiasquestr.ui.gameboard.GameBoardScreen
import com.hkentertainment.cinthiasquestr.ui.mainmenu.MainMenuScreen
import com.hkentertainment.cinthiasquestr.ui.theme.CinthiasQuestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navigationState = rememberNavigationState(
                startRoute = Screen.MainMenu,
                topLevelRoutes = setOf(Screen.MainMenu)
            )
            val navigator = remember { Navigator(navigationState) }

            val entryProvider = entryProvider<NavKey> {
                entry<Screen.MainMenu> {
                    MainMenuScreen(
                        onPlayClick = { navigator.navigate(Screen.GameBoard) }
                    )
                }
                entry<Screen.GameBoard> {
                    GameBoardScreen(
                        onPauseClick = { navigator.goBack() }
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
