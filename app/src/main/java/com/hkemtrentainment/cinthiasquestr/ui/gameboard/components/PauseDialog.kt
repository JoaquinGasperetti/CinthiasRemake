package com.hkemtrentainment.cinthiasquestr.ui.gameboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hkemtrentainment.cinthiasquestr.ui.theme.CinthiasQuestTheme

@Composable
fun PauseDialog(
    onResumeClick: () -> Unit,
    onQuitClick: () -> Unit
) {
    Dialog(onDismissRequest = onResumeClick) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5E6CE),
            ),
            border = BorderStroke(2.dp, Color(0xFF3E2723))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Juego en Pausa",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )

                Button(
                    onClick = onResumeClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3E2723),
                        contentColor = Color(0xFFF5E6CE)
                    )
                ) {
                    Text(text = "Continuar")
                }

                OutlinedButton(
                    onClick = onQuitClick,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFF3E2723)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF3E2723)
                    )
                ) {
                    Text(text = "Salir al Mapa de Mundos")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PauseDialogPreview() {
    CinthiasQuestTheme {
        PauseDialog(onResumeClick = {}, onQuitClick = {})
    }
}
