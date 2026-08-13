package com.hkentertainment.cinthiasquestr.ui.gameboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.tooling.preview.Preview
import com.hkentertainment.cinthiasquestr.ui.theme.CinthiasQuestTheme

@Composable
fun OutOfMovesDialog(
    onWatchAdClick: () -> Unit,
    onQuitClick: () -> Unit
) {
    Dialog(onDismissRequest = { /* Don't dismiss by clicking outside */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5E6CE),
            ),
            border = BorderStroke(2.dp, Color(0xFF3E2723)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Out of Moves!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "You've run out of moves! Don't give up now. Watch a short video to get 5 extra moves and keep your quest going!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF3E2723),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        // TODO: Trigger AdMob Rewarded Ad
                        onWatchAdClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3E2723),
                        contentColor = Color(0xFFF5E6CE)
                    ),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Watch Ad for +5 Moves")
                }

                TextButton(
                    onClick = onQuitClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Quit",
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
            onWatchAdClick = {},
            onQuitClick = {}
        )
    }
}
