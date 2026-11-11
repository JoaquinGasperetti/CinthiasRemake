package com.hkemtrentainment.cinthiasquestr.ui.mainmenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkemtrentainment.cinthiasquestr.CinthiasQuestApp
import com.hkemtrentainment.cinthiasquestr.R
import com.hkemtrentainment.cinthiasquestr.audio.GameSound
import com.hkemtrentainment.cinthiasquestr.data.DailyQuest
import com.hkemtrentainment.cinthiasquestr.data.PlayerData
import com.hkemtrentainment.cinthiasquestr.data.PlayerRepository
import com.hkemtrentainment.cinthiasquestr.data.WorldRepository
import com.hkemtrentainment.cinthiasquestr.ui.components.ArcadeButtonStyle
import com.hkemtrentainment.cinthiasquestr.ui.components.BoosterCanvasIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.ChunkyArcadeButton
import com.hkemtrentainment.cinthiasquestr.ui.components.ForgedIronPanel
import com.hkemtrentainment.cinthiasquestr.ui.components.GoldCoinIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.KnightShieldIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.MedievalPalette
import com.hkemtrentainment.cinthiasquestr.ui.components.QuestScrollIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalCrownIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalParchmentCard
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalStarIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.SideDockActionButton
import com.hkemtrentainment.cinthiasquestr.ui.components.StreakFlameIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.TorchEmbersOverlay
import com.hkemtrentainment.cinthiasquestr.ui.components.TreasureChestIcon
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.BoosterType
import com.hkemtrentainment.cinthiasquestr.ui.tutorial.TutorialDialog
import kotlinx.coroutines.delay

@Composable
fun MainMenuScreen(
    onPlayClick: () -> Unit,
    onQuickPlayLevelClick: (Int) -> Unit = { _ -> onPlayClick() },
    viewModel: MainMenuViewModel = viewModel(
        factory = MainMenuViewModel.Factory(
            (LocalContext.current.applicationContext as CinthiasQuestApp).playerRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerData = uiState.playerData
    val context = LocalContext.current
    val soundManager = remember(context) {
        (context.applicationContext as CinthiasQuestApp).soundManager
    }
    val haptic = LocalHapticFeedback.current

    val animatedCoins by animateIntAsState(
        targetValue = playerData.coins,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "menuCoins"
    )
    val animatedStars by animateIntAsState(
        targetValue = playerData.totalStars,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "menuStars"
    )

    val currentWorld = remember(playerData.level) {
        WorldRepository.getWorldForLevel(playerData.level)
    }
    val isBossLevel = playerData.level % 5 == 0
    val isHardLevel = !isBossLevel && (playerData.level % 3 == 0)

    val activeQuests = remember(playerData) { playerData.getActiveDailyQuests() }
    val claimableQuestsCount = activeQuests.count { it.isCompleted && !it.isClaimed }
    val completedQuestsCount = activeQuests.count { it.isCompleted }

    // Diálogo de la Armería Real (Tienda)
    if (uiState.isShopOpen) {
        ShopDialog(
            playerData = playerData,
            shopMessage = uiState.shopMessage,
            onBuyBooster = { booster ->
                val canAfford = playerData.coins >= booster.priceCoins
                soundManager.play(if (canAfford) GameSound.MATCH_SUCCESS_GEM else GameSound.TILE_INVALID)
                if (playerData.hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                viewModel.buyBooster(booster)
            },
            onBuyRoyalBundle = {
                val canAfford = playerData.coins >= 290
                soundManager.play(if (canAfford) GameSound.AWARD_GOLD else GameSound.TILE_INVALID)
                if (playerData.hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                viewModel.buyRoyalBundle()
            },
            onClaimBonusCoins = {
                if (playerData.canClaimDailyReward) {
                    soundManager.play(GameSound.AWARD_GOLD)
                    if (playerData.hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                } else {
                    soundManager.play(GameSound.TILE_INVALID)
                }
                viewModel.claimFreeBonusCoins()
            },
            onDismiss = {
                soundManager.play(GameSound.TILE_DESELECT)
                viewModel.closeShop()
            }
        )
    }

    // Modales Live-Ops dedicados (7-Day Daily Calendar, Pergamino de Misiones, Bendición de Racha)
    when (uiState.activeModal) {
        ActiveHubModal.DAILY_CALENDAR -> {
            DailyStreakCalendarModal(
                playerData = playerData,
                bannerMessage = uiState.questBannerMessage,
                onClaimToday = {
                    if (playerData.canClaimDailyReward) {
                        soundManager.play(GameSound.AWARD_GOLD)
                        if (playerData.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    } else {
                        soundManager.play(GameSound.TILE_INVALID)
                    }
                    viewModel.claimFreeBonusCoins()
                },
                onDismiss = {
                    soundManager.play(GameSound.TILE_DESELECT)
                    viewModel.closeModal()
                }
            )
        }
        ActiveHubModal.QUESTS_SCROLL -> {
            DailyQuestsScrollModal(
                quests = activeQuests,
                playerData = playerData,
                bannerMessage = uiState.questBannerMessage,
                onClaimQuest = { quest ->
                    soundManager.play(GameSound.MATCH_SUCCESS_GEM, rate = 1.18f)
                    if (playerData.hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    viewModel.claimDailyQuest(quest)
                },
                onClaimStarChest = {
                    soundManager.play(GameSound.AWARD_GOLD)
                    if (playerData.hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    viewModel.claimStarChest()
                },
                onDismiss = {
                    soundManager.play(GameSound.TILE_DESELECT)
                    viewModel.closeModal()
                }
            )
        }
        ActiveHubModal.STREAK_BLESSING -> {
            StreakBlessingModal(
                winStreak = playerData.winStreak,
                bestWinStreak = playerData.bestWinStreak,
                onPlayNow = {
                    soundManager.play(GameSound.TILE_SELECT)
                    viewModel.closeModal()
                    onQuickPlayLevelClick(playerData.level)
                },
                onDismiss = {
                    soundManager.play(GameSound.TILE_DESELECT)
                    viewModel.closeModal()
                }
            )
        }
        ActiveHubModal.TUTORIAL_GUIDE -> {
            TutorialDialog(
                onCompleteOrDismiss = {
                    soundManager.play(GameSound.TILE_DESELECT)
                    viewModel.completeAndCloseTutorial()
                }
            )
        }
        ActiveHubModal.NONE -> {}
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Escenario Diegético de Mazmorra Real (`bg_game.png`) + Viñeta Cinemática + Brasas a 60fps
        Image(
            painter = painterResource(id = R.drawable.bg_game),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0E0A0D).copy(alpha = 0.65f),
                            Color.Transparent,
                            Color(0xFF0E0A0D).copy(alpha = 0.45f),
                            Color(0xFF0E0A0D).copy(alpha = 0.90f)
                        )
                    )
                )
        )

        TorchEmbersOverlay()

        // 2. Estructura Principal del Hub de Retención (Sin Scroll Vertical, Todo al Alcance de 1 Toque)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // A. TOP HUD BAR: Rango de Caballera, Estrellas y Monedas con Acceso a la Armería
            HubTopHudBar(
                playerData = playerData,
                animatedCoins = animatedCoins,
                animatedStars = animatedStars,
                onOpenShop = {
                    soundManager.play(GameSound.TILE_SELECT)
                    if (playerData.hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    viewModel.openShop()
                },
                onOpenQuests = {
                    soundManager.play(GameSound.TILE_SELECT)
                    viewModel.openModal(ActiveHubModal.QUESTS_SCROLL)
                }
            )

            // B. ESCENARIO CENTRAL: Emblema Real + Docks Laterales Live-Ops + Cinthia Interactiva
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Emblema Heráldico Superior
                RoyalTitleCrest(
                    currentWorldName = currentWorld.name,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                )

                // Cinthia Animada e Interactiva en el Centro del Salón
                InteractiveHeroShowcase(
                    level = playerData.level,
                    worldName = currentWorld.name,
                    winStreak = playerData.winStreak,
                    dialogueIndex = uiState.cinthiaDialogueIndex,
                    onHeroTap = {
                        soundManager.play(GameSound.KNIGHT_CHEER_COMBO)
                        if (playerData.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        viewModel.cycleCinthiaDialogue()
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 44.dp)
                )

                // DOCK LATERAL IZQUIERDO: Cofre de Estrellas + Racha de la Caballera
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(top = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SideDockActionButton(
                        label = "Cofre Real",
                        progressText = "${playerData.totalStars}/${playerData.nextStarChestTarget}",
                        hasAlert = playerData.canClaimStarChest,
                        accentColor = Color(0xFFFFCA28),
                        onClick = {
                            if (playerData.canClaimStarChest) {
                                soundManager.play(GameSound.AWARD_GOLD)
                                if (playerData.hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                viewModel.claimStarChest()
                            } else {
                                soundManager.play(GameSound.TILE_SELECT)
                                viewModel.openModal(ActiveHubModal.QUESTS_SCROLL)
                            }
                        }
                    ) {
                        TreasureChestIcon(
                            isOpen = playerData.canClaimStarChest,
                            isRoyalGold = true,
                            size = 32.dp
                        )
                    }

                    SideDockActionButton(
                        label = "Racha",
                        progressText = "x${playerData.winStreak} Activa",
                        hasAlert = false,
                        accentColor = if (playerData.winStreak > 0) Color(0xFFFF7043) else Color(0xFF90A4AE),
                        onClick = {
                            soundManager.play(GameSound.TILE_SELECT)
                            viewModel.openModal(ActiveHubModal.STREAK_BLESSING)
                        }
                    ) {
                        StreakFlameIcon(size = 30.dp)
                    }

                    SideDockActionButton(
                        label = "Tutorial",
                        progressText = if (!playerData.hasCompletedTutorial) "¡NUEVO!" else "Guía 6/6",
                        hasAlert = !playerData.hasCompletedTutorial,
                        accentColor = Color(0xFF4FC3F7),
                        onClick = {
                            soundManager.play(GameSound.TILE_SELECT)
                            viewModel.openModal(ActiveHubModal.TUTORIAL_GUIDE)
                        }
                    ) {
                        QuestScrollIcon(size = 28.dp)
                    }
                }

                // DOCK LATERAL DERECHO: Racha Diaria 7D + Pergamino de Misiones
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SideDockActionButton(
                        label = "Regalo 7D",
                        progressText = if (playerData.canClaimDailyReward) "¡LISTO!" else "Día ${playerData.dailyLoginStreak}/7",
                        hasAlert = playerData.canClaimDailyReward,
                        accentColor = if (playerData.canClaimDailyReward) Color(0xFF69F0AE) else Color(0xFFFFD54F),
                        onClick = {
                            soundManager.play(GameSound.TILE_SELECT)
                            viewModel.openModal(ActiveHubModal.DAILY_CALENDAR)
                        }
                    ) {
                        TreasureChestIcon(
                            isOpen = playerData.canClaimDailyReward,
                            isRoyalGold = false,
                            size = 32.dp
                        )
                    }

                    SideDockActionButton(
                        label = "Misiones",
                        progressText = "$completedQuestsCount/${activeQuests.size}",
                        hasAlert = claimableQuestsCount > 0,
                        accentColor = if (claimableQuestsCount > 0) Color(0xFF69F0AE) else Color(0xFF4FC3F7),
                        onClick = {
                            soundManager.play(GameSound.TILE_SELECT)
                            viewModel.openModal(ActiveHubModal.QUESTS_SCROLL)
                        }
                    ) {
                        QuestScrollIcon(size = 30.dp)
                    }
                }
            }

            // C. ZONA INFERIOR DE ACCIÓN: Barra de Cofre de Estrellas + Botón Gigante 1-Tap Play + Dock de Navegación
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Barra de Progreso Rápido de Meta-Progresión (Cofre de Estrellas + Misión Activa)
                StarChestProgressStrip(
                    playerData = playerData,
                    bannerMessage = uiState.questBannerMessage,
                    onClick = {
                        if (playerData.canClaimStarChest) {
                            soundManager.play(GameSound.AWARD_GOLD)
                            viewModel.claimStarChest()
                        } else {
                            soundManager.play(GameSound.TILE_SELECT)
                            viewModel.openModal(ActiveHubModal.QUESTS_SCROLL)
                        }
                    }
                )

                // CTA PRINCIPAL DE ALTA CONVERSIÓN: 1-Tap Play directo al nivel actual + Navegación a Mapa y Armería
                PrimaryBattleActionDeck(
                    currentLevel = playerData.level,
                    worldName = currentWorld.name,
                    isBossLevel = isBossLevel,
                    isHardLevel = isHardLevel,
                    winStreak = playerData.winStreak,
                    canClaimDaily = playerData.canClaimDailyReward,
                    claimableQuestsCount = claimableQuestsCount + (if (playerData.canClaimStarChest) 1 else 0),
                    onQuickPlayClick = {
                        soundManager.play(GameSound.MATCH_SUCCESS_GEM)
                        if (playerData.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onQuickPlayLevelClick(playerData.level)
                    },
                    onWorldMapClick = {
                        soundManager.play(GameSound.TILE_SELECT)
                        if (playerData.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onPlayClick()
                    },
                    onShopClick = {
                        soundManager.play(GameSound.TILE_SELECT)
                        if (playerData.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        viewModel.openShop()
                    },
                    onQuestsClick = {
                        soundManager.play(GameSound.TILE_SELECT)
                        viewModel.openModal(ActiveHubModal.QUESTS_SCROLL)
                    }
                )

                // Espacio reservado discreto e integrado para Banner AdMob
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(Color(0xFF120D10).copy(alpha = 0.85f))
                        .border(
                            1.dp,
                            MedievalPalette.BrassDark.copy(alpha = 0.45f),
                            RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PATROCINIO REAL · ADMOB BANNER",
                        color = Color(0xFF7A685D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HubTopHudBar(
    playerData: PlayerData,
    animatedCoins: Int,
    animatedStars: Int,
    onOpenShop: () -> Unit,
    onOpenQuests: () -> Unit
) {
    val rankInfo = remember(playerData.totalStars) { playerData.getKnightRankInfo() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Insignia de Rango de Caballera (Meta-progresión de cuenta)
        Surface(
            color = MedievalPalette.IronDark.copy(alpha = 0.94f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, MedievalPalette.BrassFrame),
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenQuests)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KnightShieldIcon(rankTier = rankInfo.tier, size = 30.dp)
                Column {
                    Text(
                        text = rankInfo.title.uppercase(),
                        color = MedievalPalette.GoldBright,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF120D0F))
                                .border(0.8.dp, MedievalPalette.BrassFrame, CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(rankInfo.progressFraction)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF4FC3F7), Color(0xFFFFCA28))
                                        )
                                    )
                            )
                        }
                        Text(
                            text = " Rango ${rankInfo.tier}",
                            color = Color(0xFFD7CCC8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Contadores de Estrellas y Monedas de Oro
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Contador de Estrellas
            Surface(
                color = MedievalPalette.IronDark.copy(alpha = 0.94f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, MedievalPalette.BrassFrame),
                modifier = Modifier.shadow(6.dp, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    RoyalStarIcon(filled = true, size = 18.dp)
                    Text(
                        text = "$animatedStars",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Contador de Monedas + Botón "+" a la Armería
            Surface(
                color = MedievalPalette.IronDark.copy(alpha = 0.94f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, MedievalPalette.GoldMid),
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenShop)
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GoldCoinIcon(size = 18.dp)
                    Text(
                        text = "$animatedCoins",
                        color = MedievalPalette.GoldBright,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MedievalPalette.EmeraldTop)
                            .border(1.dp, MedievalPalette.InkOutline, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoyalTitleCrest(
    currentWorldName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ForgedIronPanel(
            cornerRadius = 22.dp,
            trimColor = MedievalPalette.GoldMid,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.gem_red),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CINTHIA'S QUEST",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        fontSize = 21.sp,
                        letterSpacing = 1.2.sp,
                        color = MedievalPalette.GoldBright
                    )
                    Text(
                        text = "CRÓNICAS DE $currentWorldName".uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.6.sp,
                        color = Color(0xFFD7CCC8)
                    )
                }
                Image(
                    painter = painterResource(id = R.drawable.gem_blue),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun InteractiveHeroShowcase(
    level: Int,
    worldName: String,
    winStreak: Int,
    dialogueIndex: Int,
    onHeroTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val frames = remember {
        listOf(
            R.drawable.cinthia_idle_01, R.drawable.cinthia_idle_02,
            R.drawable.cinthia_idle_03, R.drawable.cinthia_idle_04,
            R.drawable.cinthia_idle_05, R.drawable.cinthia_idle_06,
            R.drawable.cinthia_idle_07, R.drawable.cinthia_idle_08,
            R.drawable.cinthia_idle_09, R.drawable.cinthia_idle_10,
            R.drawable.cinthia_idle_11, R.drawable.cinthia_idle_12
        )
    }

    var isCheeringTap by remember { mutableStateOf(false) }
    LaunchedEffect(dialogueIndex) {
        if (dialogueIndex > 0) {
            isCheeringTap = true
            delay(650)
            isCheeringTap = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hubHeroAnim")
    val frameIndex by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = 12,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(12 * 115, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "heroFrame"
    )
    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isCheeringTap) 240 else 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroBreath"
    )

    val dialogues = remember(level, worldName, winStreak) {
        listOf(
            if (winStreak > 0) "¡Racha x$winStreak activa! Empezaremos el Nivel $level con bombas extra."
            else "¡Las reliquias de $worldName nos esperan en el Nivel $level!",
            "¡Combina 4 o 5 gemas para forjar Rayos y Esferas Arcoíris!",
            "¡Reclama tus Misiones y el Cofre de Estrellas para abastecer la Armería!",
            "¡Toca el botón verde para entrar directo al Nivel $level!"
        )
    }

    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onHeroTap
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Globo de diálogo diegético en pergamino
        Surface(
            color = MedievalPalette.ParchmentLight,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.5.dp, MedievalPalette.InkOutline),
            modifier = Modifier
                .width(210.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp))
        ) {
            Text(
                text = dialogues[dialogueIndex % dialogues.size],
                color = MedievalPalette.ParchmentTextDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Pedestal de Luz y Sprite Animado de Cinthia
        Box(contentAlignment = Alignment.BottomCenter) {
            // Aura circular en el suelo bajo los pies de Cinthia
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MedievalPalette.GoldBright.copy(alpha = 0.42f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Image(
                painter = painterResource(id = frames[frameIndex.coerceIn(0, frames.lastIndex)]),
                contentDescription = "Cinthia, Caballera Real",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(205.dp)
                    .offset(y = (if (isCheeringTap) -14f * breathPhase else -4f * breathPhase).dp)
                    .graphicsLayer {
                        scaleX = 1f + 0.03f * (1f - breathPhase)
                        scaleY = 1f + 0.03f * breathPhase
                    }
            )
        }
    }
}

@Composable
private fun StarChestProgressStrip(
    playerData: PlayerData,
    bannerMessage: String?,
    onClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = playerData.starChestProgressFraction,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "starChestBar"
    )

    ForgedIronPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 16.dp,
        trimColor = if (playerData.canClaimStarChest) MedievalPalette.GoldBright else MedievalPalette.BrassFrame,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TreasureChestIcon(
                        isOpen = playerData.canClaimStarChest,
                        isRoyalGold = true,
                        size = 24.dp
                    )
                    Column {
                        Text(
                            text = if (playerData.canClaimStarChest) {
                                "¡COFRE DE ESTRELLAS LISTO PARA ABRIR!"
                            } else {
                                "COFRE REAL DE ESTRELLAS"
                            },
                            color = if (playerData.canClaimStarChest) Color(0xFF69F0AE) else MedievalPalette.GoldBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = bannerMessage
                                ?: "Reúne estrellas en los niveles para ganar +220 Monedas y Boosters",
                            color = Color(0xFFD7CCC8),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = if (playerData.canClaimStarChest) MedievalPalette.EmeraldTop else Color(0xFF140E12),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MedievalPalette.GoldMid)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RoyalStarIcon(filled = true, size = 13.dp)
                        Text(
                            text = if (playerData.canClaimStarChest) "ABRIR" else "${playerData.totalStars}/${playerData.nextStarChestTarget}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF120C0F))
                    .border(1.dp, MedievalPalette.BrassFrame, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFFCA28), Color(0xFFFF8F00), Color(0xFF69F0AE))
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun PrimaryBattleActionDeck(
    currentLevel: Int,
    worldName: String,
    isBossLevel: Boolean,
    isHardLevel: Boolean,
    winStreak: Int,
    canClaimDaily: Boolean,
    claimableQuestsCount: Int,
    onQuickPlayClick: () -> Unit,
    onWorldMapClick: () -> Unit,
    onShopClick: () -> Unit,
    onQuestsClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ctaPulse")
    val ctaPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(820, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ctaScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Botón Principal 3D (1-Tap Play directo al Nivel Actual)
        ChunkyArcadeButton(
            onClick = onQuickPlayClick,
            style = if (isBossLevel) ArcadeButtonStyle.CRIMSON_ALERT else ArcadeButtonStyle.EMERALD_PLAY,
            cornerRadius = 20.dp,
            lipHeight = 6.dp,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .scale(ctaPulse)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.24f))
                            .border(1.5.dp, MedievalPalette.GoldBright, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBossLevel) {
                            RoyalCrownIcon(size = 24.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MedievalPalette.GoldBright,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "JUGAR NIVEL $currentLevel",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp
                            )
                            if (isBossLevel || isHardLevel) {
                                Surface(
                                    color = Color(0xFF2B1111),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, MedievalPalette.GoldBright)
                                ) {
                                    Text(
                                        text = if (isBossLevel) "JEFE" else "DIFÍCIL",
                                        color = MedievalPalette.GoldBright,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "$worldName · Toque rápido para combatir",
                            color = Color.White.copy(alpha = 0.90f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Insignia de Racha adherida al botón de Jugar
                if (winStreak > 0) {
                    Surface(
                        color = Color(0xFF23130E),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, MedievalPalette.GoldBright)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StreakFlameIcon(size = 16.dp)
                            Text(
                                text = "x$winStreak",
                                color = MedievalPalette.GoldBright,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // Barra de Navegación Inferior de 3 Pestañas Táctiles (Armería | Mapa del Reino | Misiones)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChunkyArcadeButton(
                onClick = onShopClick,
                style = ArcadeButtonStyle.IRON_SLATE,
                cornerRadius = 14.dp,
                lipHeight = 4.dp,
                badgeCount = if (canClaimDaily) 1 else 0,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f)
            ) {
                BoosterCanvasIcon(type = BoosterType.HAMMER, size = 18.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ARMERÍA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }

            ChunkyArcadeButton(
                onClick = onWorldMapClick,
                style = ArcadeButtonStyle.ROYAL_GOLD,
                cornerRadius = 14.dp,
                lipHeight = 4.dp,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier.weight(1.15f)
            ) {
                RoyalCrownIcon(size = 18.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MAPA MUNDOS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }

            ChunkyArcadeButton(
                onClick = onQuestsClick,
                style = ArcadeButtonStyle.IRON_SLATE,
                cornerRadius = 14.dp,
                lipHeight = 4.dp,
                badgeCount = claimableQuestsCount,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f)
            ) {
                QuestScrollIcon(size = 18.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MISIONES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
        }
    }
}

// ============================================================================
// MODALES DE RETENCIÓN: CALENDARIO 7 DÍAS, PERGAMINO DE MISIONES Y RACHA REAL
// ============================================================================

@Composable
private fun DailyStreakCalendarModal(
    playerData: PlayerData,
    bannerMessage: String?,
    onClaimToday: () -> Unit,
    onDismiss: () -> Unit
) {
    val activeDay = playerData.dailyLoginStreak.coerceIn(1, 7)
    val todayReward = PlayerRepository.computeDailyStreakReward(activeDay)

    Dialog(onDismissRequest = onDismiss) {
        RoyalParchmentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            contentPadding = PaddingValues(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TreasureChestIcon(isOpen = playerData.canClaimDailyReward, isRoyalGold = true, size = 28.dp)
                    Column {
                        Text(
                            text = "TRIBUTO REAL DE 7 DÍAS",
                            color = MedievalPalette.ParchmentTextDark,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Entra cada día para multiplicar tu tesoro",
                            color = MedievalPalette.ParchmentTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MedievalPalette.ParchmentTextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Días 1 a 6 en cuadrícula 3x2
            for (rowIdx in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (colIdx in 1..3) {
                        val dayNum = rowIdx * 3 + colIdx
                        val reward = PlayerRepository.computeDailyStreakReward(dayNum)
                        val isPastClaimed = dayNum < activeDay || (dayNum == activeDay && !playerData.canClaimDailyReward)
                        val isCurrentClaimable = dayNum == activeDay && playerData.canClaimDailyReward

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = when {
                                isCurrentClaimable -> Color(0xFFFFF3C4)
                                isPastClaimed -> Color(0xFFD7C4A5)
                                else -> Color(0xFFEADBC2)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isCurrentClaimable) 2.5.dp else 1.5.dp,
                                color = when {
                                    isCurrentClaimable -> Color(0xFFE65100)
                                    isPastClaimed -> Color(0xFF2E7D32)
                                    else -> MedievalPalette.BrassFrame
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "DÍA $dayNum",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MedievalPalette.ParchmentTextDark
                                )
                                GoldCoinIcon(size = 20.dp)
                                Text(
                                    text = if (isPastClaimed) "RECLAMADO" else "+$reward",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isPastClaimed) Color(0xFF2E7D32) else Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Gran Cofre Real del Día 7
            val day7Reward = PlayerRepository.computeDailyStreakReward(7)
            val isDay7Active = activeDay == 7 && playerData.canClaimDailyReward
            ForgedIronPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                trimColor = MedievalPalette.GoldBright,
                contentPadding = PaddingValues(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TreasureChestIcon(isOpen = isDay7Active, isRoyalGold = true, size = 36.dp)
                        Column {
                            Text(
                                text = "DÍA 7 · GRAN ARCA DE LA CORONA",
                                color = MedievalPalette.GoldBright,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Recompensa máxima semanal: +$day7Reward Monedas de Oro",
                                color = Color(0xFFF6EAD4),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            if (bannerMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = bannerMessage,
                    color = Color(0xFF1B5E20),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            ChunkyArcadeButton(
                onClick = onClaimToday,
                enabled = playerData.canClaimDailyReward,
                style = if (playerData.canClaimDailyReward) ArcadeButtonStyle.EMERALD_PLAY else ArcadeButtonStyle.IRON_SLATE,
                modifier = Modifier.fillMaxWidth()
            ) {
                GoldCoinIcon(size = 20.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (playerData.canClaimDailyReward) {
                        "RECLAMAR DÍA $activeDay (+$todayReward MONEDAS)"
                    } else {
                        "COFRE DÍA $activeDay RECLAMADO · VUELVE MAÑANA"
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun DailyQuestsScrollModal(
    quests: List<DailyQuest>,
    playerData: PlayerData,
    bannerMessage: String?,
    onClaimQuest: (DailyQuest) -> Unit,
    onClaimStarChest: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        RoyalParchmentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            contentPadding = PaddingValues(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuestScrollIcon(size = 28.dp)
                    Column {
                        Text(
                            text = "ENCARGOS DE LA CORONA",
                            color = MedievalPalette.ParchmentTextDark,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${quests.count { it.isClaimed }}/${quests.size} completados hoy",
                            color = MedievalPalette.ParchmentTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MedievalPalette.ParchmentTextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (bannerMessage != null) {
                Surface(
                    color = Color(0xFF1B5E20),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = bannerMessage,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                quests.forEach { quest ->
                    Surface(
                        color = Color(0xFFFDF7EC),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = if (quest.isCompleted && !quest.isClaimed) 2.dp else 1.5.dp,
                            color = if (quest.isCompleted && !quest.isClaimed) Color(0xFFE65100) else MedievalPalette.BrassFrame
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quest.title,
                                    color = MedievalPalette.ParchmentTextDark,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${quest.description} (${minOf(quest.currentProgress, quest.targetProgress)}/${quest.targetProgress})",
                                    color = MedievalPalette.ParchmentTextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD7C4A5))
                                        .border(1.dp, MedievalPalette.InkOutline, CircleShape)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(quest.progressFraction)
                                            .clip(CircleShape)
                                            .background(
                                                if (quest.isCompleted) Color(0xFF2E7D32) else Color(0xFFF59E0B)
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            ChunkyArcadeButton(
                                onClick = { onClaimQuest(quest) },
                                enabled = quest.isCompleted && !quest.isClaimed,
                                style = when {
                                    quest.isClaimed -> ArcadeButtonStyle.IRON_SLATE
                                    quest.isCompleted -> ArcadeButtonStyle.EMERALD_PLAY
                                    else -> ArcadeButtonStyle.ROYAL_GOLD
                                },
                                cornerRadius = 12.dp,
                                lipHeight = 3.dp,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                                modifier = Modifier.width(96.dp)
                            ) {
                                if (!quest.isClaimed) {
                                    GoldCoinIcon(size = 14.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = when {
                                        quest.isClaimed -> "HECHO"
                                        quest.isCompleted -> "COBRAR"
                                        else -> "+${quest.rewardCoins}"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sección de Cofre de Estrellas dentro del modal
            ForgedIronPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                trimColor = MedievalPalette.GoldMid,
                contentPadding = PaddingValues(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        TreasureChestIcon(isOpen = playerData.canClaimStarChest, isRoyalGold = true, size = 30.dp)
                        Column {
                            Text(
                                text = "COFRE DE ESTRELLAS (${playerData.totalStars}/${playerData.nextStarChestTarget})",
                                color = MedievalPalette.GoldBright,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Contiene +220 Monedas y +1 Booster",
                                color = Color(0xFFD7CCC8),
                                fontSize = 10.sp
                            )
                        }
                    }
                    ChunkyArcadeButton(
                        onClick = onClaimStarChest,
                        enabled = playerData.canClaimStarChest,
                        style = ArcadeButtonStyle.EMERALD_PLAY,
                        cornerRadius = 10.dp,
                        lipHeight = 3.dp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.width(88.dp)
                    ) {
                        Text(
                            text = "ABRIR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakBlessingModal(
    winStreak: Int,
    bestWinStreak: Int,
    onPlayNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val activeTier = winStreak.coerceIn(0, 3)

    Dialog(onDismissRequest = onDismiss) {
        RoyalParchmentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            contentPadding = PaddingValues(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StreakFlameIcon(size = 28.dp)
                    Column {
                        Text(
                            text = "GUARDIA REAL DE CINTHIA",
                            color = MedievalPalette.ParchmentTextDark,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Racha actual: x$winStreak · Récord: x$bestWinStreak",
                            color = MedievalPalette.ParchmentTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MedievalPalette.ParchmentTextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Gana niveles consecutivos sin rendirte para empezar cada partida con reliquias explosivas colocadas gratis en el tablero:",
                color = MedievalPalette.ParchmentTextDark,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            val tiers = listOf(
                Triple(1, "Racha x1 · Rayo de Línea", "Despliega 1 Rayo Lineal al inicio del nivel"),
                Triple(2, "Racha x2 · Bomba Real", "Despliega Rayo Lineal + Bomba Explosiva 3x3"),
                Triple(3, "Racha x3 · Bendición Suprema", "Despliega Rayo + Bomba + Esfera Arcoíris")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tiers.forEach { (tierNum, title, desc) ->
                    val unlocked = activeTier >= tierNum
                    Surface(
                        color = if (unlocked) Color(0xFFFFF3C4) else Color(0xFFEADBC2),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (unlocked) 2.dp else 1.dp,
                            color = if (unlocked) Color(0xFFE65100) else MedievalPalette.BrassFrame
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StreakFlameIcon(size = 22.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    color = MedievalPalette.ParchmentTextDark,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = desc,
                                    color = MedievalPalette.ParchmentTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Surface(
                                color = if (unlocked) Color(0xFF2E7D32) else Color(0xFF5D4037),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (unlocked) "ACTIVA" else "x$tierNum",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            ChunkyArcadeButton(
                onClick = onPlayNow,
                style = ArcadeButtonStyle.EMERALD_PLAY,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (winStreak > 0) "DEFENDER MI RACHA x$winStreak" else "COMENZAR RACHA AHORA",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}
