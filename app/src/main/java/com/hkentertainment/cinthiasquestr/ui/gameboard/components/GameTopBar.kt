package com.hkemtrentainment.cinthiasquestr.ui.gameboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkemtrentainment.cinthiasquestr.ui.theme.CinthiasQuestTheme

@Composable
fun GameTopBar(
    score: Int,
    targetScore: Int,
    movesLeft: Int,
    level: Int,
    coins: Int,
    onPauseClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color(0xFF3E2723),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level & Target Score Info
            Column {
                Text(
                    text = "LEVEL $level",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5E6CE)
                )
                Text(
                    text = "Target: $targetScore",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFD700)
                )
            }

            // Score & Moves Display
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score Box
                Box(
                    modifier = Modifier
                        .background(Color(0xFF2C1A14), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SCORE",
                            fontSize = 10.sp,
                            color = Color(0xFFD7CCC8),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$score",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Moves Box
                Box(
                    modifier = Modifier
                        .background(Color(0xFF2C1A14), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MOVES",
                            fontSize = 10.sp,
                            color = Color(0xFFD7CCC8),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$movesLeft",
                            fontSize = 16.sp,
                            color = if (movesLeft <= 5) Color(0xFFFF5252) else Color(0xFFFFD700),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Pause Button
            IconButton(onClick = onPauseClick) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = Color(0xFFF5E6CE)
                )
            }
        }
    }
}
