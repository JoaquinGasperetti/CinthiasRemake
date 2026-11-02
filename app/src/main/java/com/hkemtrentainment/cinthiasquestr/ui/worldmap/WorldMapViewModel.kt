package com.hkemtrentainment.cinthiasquestr.ui.worldmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hkemtrentainment.cinthiasquestr.data.PlayerRepository
import com.hkemtrentainment.cinthiasquestr.data.WorldConfig
import com.hkemtrentainment.cinthiasquestr.data.WorldRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorldMapUiState(
    val coins: Int = 0,
    val maxUnlockedLevel: Int = 1,
    val levelStars: Map<Int, Int> = emptyMap(),
    val totalStars: Int = 0,
    val selectedWorld: WorldConfig = WorldRepository.WORLDS.first(),
    val activeDetailLevel: Int? = null
)

class WorldMapViewModel(private val playerRepository: PlayerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(WorldMapUiState())
    val uiState: StateFlow<WorldMapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playerRepository.playerDataFlow.collect { playerData ->
                val totalStars = playerData.levelStars.values.sum()
                val currentWorld = WorldRepository.getWorldForLevel(playerData.level)
                _uiState.update {
                    it.copy(
                        coins = playerData.coins,
                        maxUnlockedLevel = playerData.level,
                        levelStars = playerData.levelStars,
                        totalStars = totalStars,
                        selectedWorld = currentWorld
                    )
                }
            }
        }
    }

    fun onLevelClicked(levelNumber: Int) {
        if (levelNumber <= _uiState.value.maxUnlockedLevel) {
            _uiState.update { it.copy(activeDetailLevel = levelNumber) }
        }
    }

    fun dismissDetailDialog() {
        _uiState.update { it.copy(activeDetailLevel = null) }
    }

    fun selectWorld(world: WorldConfig) {
        _uiState.update { it.copy(selectedWorld = world) }
    }

    class Factory(private val playerRepository: PlayerRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WorldMapViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WorldMapViewModel(playerRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
