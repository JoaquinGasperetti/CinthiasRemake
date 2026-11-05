package com.hkemtrentainment.cinthiasquestr.ui.gameboard.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hkemtrentainment.cinthiasquestr.R
import com.hkemtrentainment.cinthiasquestr.ui.components.ArcadeButtonStyle
import com.hkemtrentainment.cinthiasquestr.ui.components.ChunkyArcadeButton
import com.hkemtrentainment.cinthiasquestr.ui.components.ForgedIronPanel
import com.hkemtrentainment.cinthiasquestr.ui.components.GoldCoinIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.MedievalPalette
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalParchmentCard
import com.hkemtrentainment.cinthiasquestr.ui.components.StreakFlameIcon
import com.hkemtrentainment.cinthiasquestr.ui.theme.CinthiasQuestTheme

@Composable
fun OutOfMovesDialog(
    score: Int = 1400,
    targetScore: Int = 1600,
    coins: Int = 100,
    winStreak: Int = 0,
    continueCoinCost: Int = 80,
    onWatchAdClick: () -> Unit,
    onContinueWithCoinsClick: () -> Unit = {},
    onRetryClick: () -> Unit = {},
    onQuitClick: () -> Unit
) {
    val pointsMissing = maxOf(0, targetScore - score)
    val percentReached = if (targetScore > 0) ((score * 100) / targetScore).coerceIn(0, 99) else 0
    val animatedProgress by animateFloatAsState(
        targetValue = (percentReached / 100f).coerceIn(0.06f, 0.99f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "nearMissProgress"
    )
    val canAffordContinue = coins >= continueCoinCost

    Dialog(onDismissRequest = { /* Bloqueado para evitar cierres accidentales */ }) {
        RoyalParchmentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            accentTrimColor = Color(0xFFC62828),
            contentPadding = PaddingValues(0.dp)
        ) {
            // Cabecera Dramática Near-Miss
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF4A1212), Color(0xFFB71C1C), Color(0xFF4A1212))
                        )
                    )
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (percentReached >= 75) "¡A UN PASO DE LA VICTORIA!" else "¡SIN MOVIMIENTOS!",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = MedievalPalette.GoldBright,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Saldo disponible: $coins Monedas de Oro",
                        color = Color(0xFFF6EAD4),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Panel Near-Miss con Retrato de Cinthia y Barra de Progreso
                ForgedIronPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    trimColor = MedievalPalette.BrassFrame,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = Color(0xFF140E12),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, Color(0xFFEF5350)),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.cinthia_idle_01),
                                    contentDescription = "Cinthia",
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Completaste el $percentReached% del objetivo",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Solo faltan $pointsMissing pts para ganar el nivel",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedievalPalette.GoldBright
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(11.dp)
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
                                            listOf(Color(0xFFFFCA28), Color(0xFFFF5722))
                                        )
                                    )
                            )
                        }
                    }
                }

                // Aversión a la Pérdida (Loss Aversion): Alerta de Racha en Riesgo
                if (winStreak > 0) {
                    val lostRelics = winStreak.coerceIn(1, 3)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF2B1212),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, Color(0xFFFF7043))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StreakFlameIcon(size = 24.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "¡RACHA x$winStreak EN PELIGRO!",
                                    color = MedievalPalette.GoldBright,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Si te rindes perderás tus +$lostRelics Reliquias Iniciales gratis.",
                                    color = Color(0xFFFFCCBC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // CTA 1: Seguir con +5 Movimientos Gratis (Video Recompensado)
                ChunkyArcadeButton(
                    onClick = onWatchAdClick,
                    style = ArcadeButtonStyle.EMERALD_PLAY,
                    cornerRadius = 16.dp,
                    lipHeight = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+5 MOVIMIENTOS GRATIS (VER VIDEO)",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                // CTA 2: Seguir con Monedas de Oro
                ChunkyArcadeButton(
                    onClick = onContinueWithCoinsClick,
                    enabled = canAffordContinue,
                    style = ArcadeButtonStyle.ROYAL_GOLD,
                    cornerRadius = 16.dp,
                    lipHeight = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GoldCoinIcon(size = 18.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONTINUAR (+5 MOVS POR $continueCoinCost ORO)",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                // CTA 3: Reintentar Nivel al Instante
                ChunkyArcadeButton(
                    onClick = onRetryClick,
                    style = ArcadeButtonStyle.IRON_SLATE,
                    cornerRadius = 14.dp,
                    lipHeight = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "REINTENTAR NIVEL DESDE CERO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                TextButton(
                    onClick = onQuitClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Volver al Mapa del Reino",
                        color = MedievalPalette.ParchmentTextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OutOfMovesDialogPreview() {
    CinthiasQuestTheme {
        OutOfMovesDialog(
            score = 1480,
            targetScore = 1600,
            coins = 200,
            winStreak = 2,
            onWatchAdClick = {},
            onQuitClick = {}
        )
    }
}
