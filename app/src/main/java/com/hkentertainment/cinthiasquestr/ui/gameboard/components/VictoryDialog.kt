package com.hkemtrentainment.cinthiasquestr.ui.gameboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
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
            color = Color(0xFFF5E6CE), // Parchment
            border = BorderStroke(4.dp, Color(0xFF3E2723)), // Dark Brown Border
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "¡Nivel Completado!",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3E2723)
                    ),
                    textAlign = TextAlign.Center
                )

                // Earned Stars Display
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

                Spacer(modifier = Modifier.height(4.dp))

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
                        "Ver Anuncio (Doble Monedas 🪙)",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                }

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
                        "SIGUIENTE NIVEL ➔",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
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
                        "MAPA DE MUNDOS",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
