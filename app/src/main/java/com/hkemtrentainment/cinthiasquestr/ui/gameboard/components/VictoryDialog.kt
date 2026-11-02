package com.hkemtrentainment.cinthiasquestr.ui.gameboard.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun VictoryDialog(
    score: Int,
    stars: Int = 1,
    earnedCoins: Int = 75,
    doubleCoinsClaimed: Boolean = false,
    hasNextLevel: Boolean = true,
    onWatchAdClick: () -> Unit,
    onNextLevelClick: () -> Unit,
    onWorldMapClick: () -> Unit
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF5E6CE),
            border = BorderStroke(4.dp, Color(0xFF3E2723)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        val size = if (index == 1) 48.dp else 38.dp
                        Icon(
                            imageVector = if (isStarEarned) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = if (isStarEarned) Color(0xFFFFD700) else Color(0xFFB0BEC5),
                            modifier = Modifier.size(size)
                        )
                    }
                }

                Text(
                    text = "Puntuación Final: $score",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        color = Color(0xFF5D4037),
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                val totalDisplayedCoins = if (doubleCoinsClaimed) earnedCoins * 2 else earnedCoins
                Text(
                    text = "Recompensa: +$totalDisplayedCoins 🪙",
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
                            fontWeight = FontWeight.Bold
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
