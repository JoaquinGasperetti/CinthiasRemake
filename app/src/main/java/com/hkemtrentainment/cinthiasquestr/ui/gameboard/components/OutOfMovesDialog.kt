package com.hkemtrentainment.cinthiasquestr.ui.gameboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hkemtrentainment.cinthiasquestr.ui.theme.CinthiasQuestTheme

@Composable
fun OutOfMovesDialog(
    onWatchAdClick: () -> Unit,
    onQuitClick: () -> Unit
) {
    Dialog(onDismissRequest = { /* No cerrar tocando fuera */ }) {
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
                    text = "¡Sin Movimientos!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "¡Te quedaste sin movimientos! No te rindas ahora: mira un video corto para obtener +5 movimientos extra y continuar tu misión.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF3E2723),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onWatchAdClick,
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
                    Text(text = "Ver Anuncio (+5 Movimientos)")
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
            onWatchAdClick = {},
            onQuitClick = {}
        )
    }
}
