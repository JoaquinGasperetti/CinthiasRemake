package com.hkemtrentainment.cinthiasquestr.ui.mainmenu

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkemtrentainment.cinthiasquestr.CinthiasQuestApp
import com.hkemtrentainment.cinthiasquestr.audio.GameSound
import com.hkemtrentainment.cinthiasquestr.data.DailyQuest

@Composable
fun MainMenuScreen(
    onPlayClick: () -> Unit,
    viewModel: MainMenuViewModel = viewModel(
        factory = MainMenuViewModel.Factory(
            (LocalContext.current.applicationContext as CinthiasQuestApp).playerRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val soundManager = remember(context) {
        (context.applicationContext as CinthiasQuestApp).soundManager
    }
    val haptic = LocalHapticFeedback.current

    val animatedCoins by animateIntAsState(
        targetValue = uiState.playerData.coins,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "menuCoins"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "menuPulse")
    val playButtonPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playButtonPulse"
    )

    if (uiState.isShopOpen) {
        ShopDialog(
            playerData = uiState.playerData,
            shopMessage = uiState.shopMessage,
            onBuyBooster = {
                soundManager.play(GameSound.MATCH_SUCCESS_GEM)
                if (uiState.playerData.hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                viewModel.buyBooster(it)
            },
            onClaimBonusCoins = {
                soundManager.play(GameSound.AWARD_GOLD)
                if (uiState.playerData.hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                viewModel.claimFreeBonusCoins()
            },
            onDismiss = {
                soundManager.play(GameSound.TILE_DESELECT)
                viewModel.closeShop()
            }
        )
    }

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = Color(0xFF3E2723)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Espacio Publicitario (AdMob)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8D6E63)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF3E2723), Color(0xFF1B1210))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Cinthia's Quest",
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD4AF37)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Chips de Estado: Estrellas, Monedas y Racha de Victorias
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF2C1A14),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFD700))
                    ) {
                        Text(
                            text = "⭐ ${uiState.playerData.levelStars.values.sum()}",
                            color = Color(0xFFF5E6CE),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFF2C1A14),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFB300))
                    ) {
                        Text(
                            text = "🪙 $animatedCoins",
                            color = Color(0xFFF5E6CE),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFF2C1A14),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF7043))
                    ) {
                        Text(
                            text = "🔥 Racha x${uiState.playerData.winStreak}",
                            color = Color(0xFFFFCC80),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        soundManager.play(GameSound.TILE_SELECT)
                        if (uiState.playerData.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onPlayClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(52.dp)
                        .scale(playButtonPulse),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4AF37),
                        contentColor = Color(0xFF3E2723)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Text(
                        text = "COMENZAR AVENTURA",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        soundManager.play(GameSound.TILE_SELECT)
                        if (uiState.playerData.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        viewModel.openShop()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(48.dp),
                    border = BorderStroke(2.dp, Color(0xFF8D6E63)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF5E6CE)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "TIENDA & COFRE DIARIO (DÍA ${uiState.playerData.dailyLoginStreak})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Panel de Misiones Diarias (Motor de Retención D1/D7/D30 y Frecuencia de Sesión)
                DailyQuestsPanel(
                    quests = uiState.playerData.getActiveDailyQuests(),
                    bannerMessage = uiState.questBannerMessage,
                    onClaimQuest = { quest ->
                        soundManager.play(GameSound.MATCH_SUCCESS_GEM, rate = 1.2f)
                        if (uiState.playerData.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        viewModel.claimDailyQuest(quest)
                    }
                )
            }
        }
    }
}

@Composable
private fun DailyQuestsPanel(
    quests: List<DailyQuest>,
    bannerMessage: String?,
    onClaimQuest: (DailyQuest) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF281813),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, Color(0xFF8D6E63))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📜 MISIONES DIARIAS DE CINTHIA",
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
                Text(
                    text = "${quests.count { it.isClaimed }}/${quests.size} Completadas",
                    color = Color(0xFFD7CCC8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (bannerMessage != null) {
                Text(
                    text = bannerMessage,
                    color = Color(0xFF81C784),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            quests.forEach { quest ->
                Surface(
                    color = Color(0xFF3E2723),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (quest.isCompleted && !quest.isClaimed) Color(0xFFFFD700) else Color(0xFF5D4037)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = quest.title,
                                color = Color(0xFFF5E6CE),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${quest.description} (${minOf(quest.currentProgress, quest.targetProgress)}/${quest.targetProgress})",
                                color = Color(0xFFD7CCC8),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1B1210))
                                    .border(0.5.dp, Color(0xFF8D6E63), CircleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(quest.progressFraction)
                                        .clip(CircleShape)
                                        .background(
                                            if (quest.isCompleted) Color(0xFF66BB6A) else Color(0xFFFFB300)
                                        )
                                )
                            }
                        }

                        Button(
                            onClick = { onClaimQuest(quest) },
                            enabled = quest.isCompleted && !quest.isClaimed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (quest.isClaimed) Color(0xFF5D4037) else Color(0xFFFFB300),
                                contentColor = Color(0xFF3E2723)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when {
                                    quest.isClaimed -> "✓ Lista"
                                    quest.isCompleted -> "+${quest.rewardCoins} 🪙"
                                    else -> "${quest.rewardCoins} 🪙"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}
