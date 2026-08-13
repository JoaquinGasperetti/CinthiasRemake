package com.hkentertainment.cinthiasquestr.ui.gameboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.hkentertainment.cinthiasquestr.ui.theme.CinthiasQuestTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTopBar(
    score: Int,
    targetScore: Int,
    movesLeft: Int,
    level: Int,
    coins: Int,
    onPauseClick: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HUDItem(label = "Level", value = level.toString())
                HUDItem(label = "Score", value = "$score / $targetScore")
                HUDItem(label = "Moves", value = movesLeft.toString())
                HUDItem(label = "Coins", value = coins.toString())
            }
        },
        actions = {
            IconButton(
                onClick = onPauseClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = Color(0xFF8D6E63)
                )
            }
        }
    )
}

@Composable
fun HUDItem(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3E2723)),
        border = BorderStroke(2.dp, Color(0xFF8D6E63)),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8D6E63)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameTopBarPreview() {
    CinthiasQuestTheme {
        GameTopBar(score = 120, targetScore = 2000, movesLeft = 15, level = 1, coins = 100, onPauseClick = {})
    }
}
