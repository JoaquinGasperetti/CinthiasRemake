package com.hkemtrentainment.cinthiasquestr.ui.gameboard.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun VictoryDialog(
    score: Int,
    stars: Int = 1,
    earnedCoins: Int = 75,
    bonusMovesScore: Int = 0,
    winStreak: Int = 1,
    doubleCoinsClaimed: Boolean = false,
    hasNextLevel: Boolean = true,
    onWatchAdClick: () -> Unit,
    onNextLevelClick: () -> Unit,
    onWorldMapClick: () -> Unit
) {
    // Aparición secuencial con rebote elástico para cada una de las 3 estrellas
    val starScales = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(stars) {
        starScales.forEachIndexed { idx, anim ->
            delay(idx * 190L)
            anim.animateTo(
                targetValue = if (idx < stars) 1f else 0.85f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "victoryScore"
    )
    val totalDisplayedCoins = if (doubleCoinsClaimed) earnedCoins * 2 else earnedCoins
    val animatedCoins by animateIntAsState(
        targetValue = totalDisplayedCoins,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "victoryCoins"
    )

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFFF5E6CE),
            border = BorderStroke(4.dp, Color(0xFF3E2723)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (hasNextLevel) "¡Nivel Completado!" else "¡Aventura Completada!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3E2723)
                    ),
                    textAlign = TextAlign.Center
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isStarEarned = index < stars
                        val size = if (index == 1) 52.dp else 40.dp
                        Icon(
                            imageVector = if (isStarEarned) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = if (isStarEarned) Color(0xFFFFD700) else Color(0xFFB0BEC5),
                            modifier = Modifier
                                .size(size)
                                .scale(starScales[index].value)
                        )
                    }
                }

                if (winStreak >= 1) {
                    Surface(
                        color = Color(0xFF3E2723),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFFFD700))
                    ) {
                        val nextPowerUps = winStreak.coerceIn(1, 3)
                        Text(
                            text = "🔥 Racha de Victorias x$winStreak · +$nextPowerUps Power-Up${if (nextPowerUps > 1) "s" else ""} en el próximo nivel",
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Text(
                    text = "Puntuación Final: $animatedScore",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        color = Color(0xFF5D4037),
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                if (bonusMovesScore > 0) {
                    Text(
                        text = "(Incluye +$bonusMovesScore pts de Bonus por Movimientos)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6D4C41),
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = "Recompensa: +$animatedCoins 🪙",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Serif,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                if (!doubleCoinsClaimed) {
                    Button(
                        onClick = onWatchAdClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8D6E63),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Ver Anuncio (Doble Monedas +$earnedCoins 🪙)",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (hasNextLevel) {
                    Button(
                        onClick = onNextLevelClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB300),
                            contentColor = Color(0xFF3E2723)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "SIGUIENTE NIVEL ➔",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onWorldMapClick,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(2.dp, Color(0xFF8D6E63)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF8D6E63)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "MAPA DE MUNDOS",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
