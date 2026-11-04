package com.hkemtrentainment.cinthiasquestr.ui.gameboard.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
        targetValue = (percentReached / 100f).coerceIn(0.05f, 0.99f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "nearMissProgress"
    )
    val canAffordContinue = coins >= continueCoinCost

    Dialog(onDismissRequest = { /* No cerrar tocando fuera */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5E6CE),
            ),
            border = BorderStroke(3.dp, Color(0xFF3E2723)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (percentReached >= 75) "¡Estuviste Tan Cerca!" else "¡Sin Movimientos!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF3E2723),
                    textAlign = TextAlign.Center
                )

                // Tarjeta de psicología Near-Miss ("Casi Ganas")
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFEDE0D4),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFD7CCC8))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Completaste el $percentReached% del nivel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF3E2723)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3E2723).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF5D4037), CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedProgress)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFFFB300), Color(0xFFE65100))
                                        )
                                    )
                            )
                        }
                        Text(
                            text = "Solo te faltaron $pointsMissing pts para la victoria",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB71C1C)
                        )
                    }
                }

                // Alerta de Loss Aversion si el jugador tiene una Racha de Victorias activa
                if (winStreak > 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF3E2723),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFFFB300))
                    ) {
                        Text(
                            text = "🔥 ¡No pierdas tu Racha x$winStreak! Si continúas conservarás tus Power-Ups iniciales.",
                            color = Color(0xFFFFD700),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Button(
                    onClick = onWatchAdClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ver Video (+5 Movimientos Gratis)",
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onContinueWithCoinsClick,
                    enabled = canAffordContinue,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB300),
                        contentColor = Color(0xFF3E2723)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Text(
                        text = "Continuar (+5 Movs por $continueCoinCost 🪙)",
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                OutlinedButton(
                    onClick = onRetryClick,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.5.dp, Color(0xFF3E2723)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF3E2723)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reintentar Nivel al Instante",
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = onQuitClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Salir al Mapa de Mundos",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF3E2723).copy(alpha = 0.7f)
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
