package com.hkemtrentainment.cinthiasquestr.ui.gameboard.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hkemtrentainment.cinthiasquestr.R
import com.hkemtrentainment.cinthiasquestr.ui.components.ArcadeButtonStyle
import com.hkemtrentainment.cinthiasquestr.ui.components.ChunkyArcadeButton
import com.hkemtrentainment.cinthiasquestr.ui.components.ForgedIronPanel
import com.hkemtrentainment.cinthiasquestr.ui.components.GoldCoinIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.MedievalPalette
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalCrownIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalParchmentCard
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalStarIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.StreakFlameIcon
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
    val starScales = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(stars) {
        starScales.forEachIndexed { idx, anim ->
            delay(idx * 170L)
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
        RoyalParchmentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            accentTrimColor = MedievalPalette.GoldMid,
            contentPadding = PaddingValues(0.dp)
        ) {
            // Estandarte Superior de Victoria Real
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF1B5E20))
                        )
                    )
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoyalCrownIcon(size = 24.dp)
                        Text(
                            text = if (hasNextLevel) "¡VICTORIA REAL!" else "¡REINO LIBERADO!",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = MedievalPalette.GoldBright
                        )
                        RoyalCrownIcon(size = 24.dp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Trío de Estrellas Facetadas con Rebote Secuencial
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { index ->
                            val isStarEarned = index < stars
                            val starSize = if (index == 1) 52.dp else 40.dp
                            RoyalStarIcon(
                                filled = isStarEarned,
                                size = starSize,
                                modifier = Modifier.scale(starScales[index].value)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tarjeta de Puntuación y Botín con Retrato de Cinthia
                ForgedIronPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    trimColor = MedievalPalette.GoldMid,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = Color(0xFF161014),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, MedievalPalette.GoldBright),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.cinthia_idle_01),
                                contentDescription = "Cinthia Victoriosa",
                                modifier = Modifier.padding(4.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "PUNTUACIÓN: $animatedScore",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            if (bonusMovesScore > 0) {
                                Text(
                                    text = "Bonus de movimientos: +$bonusMovesScore pts",
                                    color = Color(0xFF81C784),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                GoldCoinIcon(size = 18.dp)
                                Text(
                                    text = "Botín: +$animatedCoins Monedas",
                                    color = MedievalPalette.GoldBright,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Insignia de Progreso de Racha de la Caballera
                if (winStreak >= 1) {
                    val nextPowerUps = winStreak.coerceIn(1, 3)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFDF7EC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFE65100))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StreakFlameIcon(size = 20.dp)
                            Text(
                                text = "Racha x$winStreak: Empezarás el próximo nivel con +$nextPowerUps Reliquia${if (nextPowerUps > 1) "s" else ""} en el tablero",
                                color = MedievalPalette.ParchmentTextDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Botón Principal: Siguiente Nivel (1-Tap Loop)
                if (hasNextLevel) {
                    ChunkyArcadeButton(
                        onClick = onNextLevelClick,
                        style = ArcadeButtonStyle.EMERALD_PLAY,
                        cornerRadius = 16.dp,
                        lipHeight = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SIGUIENTE NIVEL",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }

                // Botón de Duplicar Monedas (Rewarded Video Hook)
                if (!doubleCoinsClaimed) {
                    ChunkyArcadeButton(
                        onClick = onWatchAdClick,
                        style = ArcadeButtonStyle.ROYAL_GOLD,
                        cornerRadius = 14.dp,
                        lipHeight = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GoldCoinIcon(size = 18.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DUPLICAR BOTÍN (+$earnedCoins ORO EXTRA)",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }

                ChunkyArcadeButton(
                    onClick = onWorldMapClick,
                    style = ArcadeButtonStyle.IRON_SLATE,
                    cornerRadius = 14.dp,
                    lipHeight = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "VOLVER AL MAPA DEL REINO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
