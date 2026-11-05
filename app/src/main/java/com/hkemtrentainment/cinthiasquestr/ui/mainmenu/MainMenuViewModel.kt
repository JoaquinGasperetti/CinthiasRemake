package com.hkemtrentainment.cinthiasquestr.ui.mainmenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hkemtrentainment.cinthiasquestr.data.DailyQuest
import com.hkemtrentainment.cinthiasquestr.data.PlayerData
import com.hkemtrentainment.cinthiasquestr.data.PlayerRepository
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.BoosterType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ActiveHubModal {
    NONE,
    DAILY_CALENDAR,
    QUESTS_SCROLL,
    STREAK_BLESSING
}

data class MainMenuUiState(
    val playerData: PlayerData = PlayerData(),
    val isShopOpen: Boolean = false,
    val activeModal: ActiveHubModal = ActiveHubModal.NONE,
    val shopMessage: String? = null,
    val questBannerMessage: String? = null,
    val cinthiaDialogueIndex: Int = 0
)

class MainMenuViewModel(private val playerRepository: PlayerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MainMenuUiState())
    val uiState: StateFlow<MainMenuUiState> = _uiState.asStateFlow()

    private var isClaimingDailyChest = false
    private var isClaimingStarChest = false

    init {
        viewModelScope.launch {
            playerRepository.playerDataFlow.collect { data ->
                _uiState.update { it.copy(playerData = data) }
            }
        }
    }

    fun openShop() {
        _uiState.update {
            it.copy(
                isShopOpen = true,
                activeModal = ActiveHubModal.NONE,
                shopMessage = null
            )
        }
    }

    fun closeShop() {
        _uiState.update { it.copy(isShopOpen = false, shopMessage = null) }
    }

    fun openModal(modal: ActiveHubModal) {
        _uiState.update {
            it.copy(
                activeModal = modal,
                isShopOpen = false,
                questBannerMessage = null
            )
        }
    }

    fun closeModal() {
        _uiState.update { it.copy(activeModal = ActiveHubModal.NONE, questBannerMessage = null) }
    }

    fun cycleCinthiaDialogue() {
        _uiState.update {
            it.copy(cinthiaDialogueIndex = (it.cinthiaDialogueIndex + 1) % 4)
        }
    }

    fun buyBooster(type: BoosterType) {
        viewModelScope.launch {
            val success = playerRepository.buyBooster(type, type.priceCoins)
            val msg = if (success) {
                "¡Forjaste +1 ${type.displayName} para tu inventario!"
            } else {
                "Monedas insuficientes (${type.priceCoins} requeridas)."
            }
            _uiState.update { it.copy(shopMessage = msg) }
        }
    }

    fun buyRoyalBundle() {
        viewModelScope.launch {
            val bundleCost = 290
            val success = playerRepository.buyRoyalBoosterBundle(bundleCost)
            val msg = if (success) {
                "¡Lote de Asedio Real adquirido (+1 Martillo, +1 Dinamita, +1 Barajar)!"
            } else {
                "Necesitas $bundleCost monedas de oro para el Lote de Asedio Real."
            }
            _uiState.update { it.copy(shopMessage = msg) }
        }
    }

    fun claimFreeBonusCoins() {
        if (isClaimingDailyChest || !_uiState.value.playerData.canClaimDailyReward) {
            _uiState.update {
                it.copy(
                    shopMessage = "El Cofre Real de hoy ya fue reclamado. ¡Regresa mañana para mantener tu racha!",
                    questBannerMessage = "El Cofre Real de hoy ya fue reclamado. ¡Regresa mañana!"
                )
            }
            return
        }
        isClaimingDailyChest = true
        viewModelScope.launch {
            try {
                val (reward, streakDay) = playerRepository.claimDailyStreakChest()
                val message = if (reward > 0) {
                    "¡Cofre de Racha Diaria (Día $streakDay/7) abierto: +$reward Monedas de Oro!"
                } else {
                    "El Cofre Real de hoy ya fue reclamado. ¡Regresa mañana!"
                }
                _uiState.update {
                    it.copy(
                        shopMessage = message,
                        questBannerMessage = message
                    )
                }
            } finally {
                isClaimingDailyChest = false
            }
        }
    }

    fun claimStarChest() {
        if (isClaimingStarChest || !_uiState.value.playerData.canClaimStarChest) return
        isClaimingStarChest = true
        viewModelScope.launch {
            try {
                val reward = playerRepository.claimStarChestReward()
                if (reward != null) {
                    val (coins, booster) = reward
                    _uiState.update {
                        it.copy(
                            questBannerMessage = "¡Cofre de Estrellas abierto! +$coins Monedas y +1 ${booster.displayName}"
                        )
                    }
                }
            } finally {
                isClaimingStarChest = false
            }
        }
    }

    fun claimDailyQuest(quest: DailyQuest) {
        if (!quest.isCompleted || quest.isClaimed) return
        viewModelScope.launch {
            val success = playerRepository.claimDailyQuestReward(quest.id, quest.rewardCoins)
            if (success) {
                _uiState.update {
                    it.copy(questBannerMessage = "¡Misión '${quest.title}' completada! +${quest.rewardCoins} Monedas")
                }
            }
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
