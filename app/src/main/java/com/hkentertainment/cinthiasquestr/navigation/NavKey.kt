package com.hkemtrentainment.cinthiasquestr.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen : NavKey {
    @Serializable
    data object MainMenu : Screen()

    @Serializable
    data object WorldMap : Screen()

    @Serializable
    data class GameBoard(val level: Int = 1) : Screen()
}
