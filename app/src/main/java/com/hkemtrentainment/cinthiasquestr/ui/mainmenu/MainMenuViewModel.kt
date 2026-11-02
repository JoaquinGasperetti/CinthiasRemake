package com.hkemtrentainment.cinthiasquestr.ui.mainmenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hkemtrentainment.cinthiasquestr.data.PlayerData
import com.hkemtrentainment.cinthiasquestr.data.PlayerRepository
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.BoosterType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainMenuUiState(
    val playerData: PlayerData = PlayerData(),
    val isShopOpen: Boolean = false,
    val shopMessage: String? = null
)

class MainMenuViewModel(private val playerRepository: PlayerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MainMenuUiState())
    val uiState: StateFlow<MainMenuUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playerRepository.playerDataFlow.collect { data ->
                _uiState.update { it.copy(playerData = data) }
            }
        }
    }

    fun openShop() {
        _uiState.update { it.copy(isShopOpen = true, shopMessage = null) }
    }

    fun closeShop() {
        _uiState.update { it.copy(isShopOpen = false, shopMessage = null) }
    }

    fun buyBooster(type: BoosterType) {
        viewModelScope.launch {
            val success = playerRepository.buyBooster(type, type.priceCoins)
            val msg = if (success) {
                "¡Compraste +1 ${type.displayName}!"
            } else {
                "No tienes suficientes monedas (${type.priceCoins} 🪙 requeridas)."
            }
            _uiState.update { it.copy(shopMessage = msg) }
        }
    }

    fun claimFreeBonusCoins() {
        viewModelScope.launch {
            playerRepository.addCoins(250)
            _uiState.update { it.copy(shopMessage = "¡Recibiste +250 🪙 de bonificación!") }
        }
    }

    class Factory(private val playerRepository: PlayerRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainMenuViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainMenuViewModel(playerRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
