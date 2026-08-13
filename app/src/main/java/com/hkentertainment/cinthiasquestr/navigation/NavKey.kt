package com.hkentertainment.cinthiasquestr.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen : NavKey {
    @Serializable
    data object MainMenu : Screen()

    @Serializable
    data object GameBoard : Screen()
}
