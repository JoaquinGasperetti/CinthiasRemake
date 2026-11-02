package com.hkemtrentainment.cinthiasquestr.navigation

import androidx.navigation3.runtime.NavKey

class Navigator(val state: NavigationState) {
    fun navigate(key: NavKey) {
        if (key in state.backStacks.keys) {
            state.topLevelRoute = key
        } else {
            state.backStacks[state.topLevelRoute]?.add(key)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
        if (currentStack != null && currentStack.size > 1) {
            currentStack.removeAt(currentStack.lastIndex)
        } else if (state.topLevelRoute != state.startRoute) {
            state.topLevelRoute = state.startRoute
        }
    }
}
