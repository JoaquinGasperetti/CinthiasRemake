package com.hkemtrentainment.cinthiasquestr.ui.gameboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hkemtrentainment.cinthiasquestr.ui.theme.CinthiasQuestTheme

@Composable
fun PauseDialog(
    soundEnabled: Boolean = true,
    hapticsEnabled: Boolean = true,
    reducedMotion: Boolean = false,
    onToggleSound: (Boolean) -> Unit = {},
    onToggleHaptics: (Boolean) -> Unit = {},
    onToggleReducedMotion: (Boolean) -> Unit = {},
    onRestartClick: () -> Unit = {},
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
                    .padding(22.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Juego en Pausa",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )

                // Controles de Game Feel & Accesibilidad (Sonido, Háptica y Reducción de Sacudida)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFEDE0D4),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFD7CCC8))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SettingSwitchRow(
                            label = "🔊 Efectos de Sonido",
                            checked = soundEnabled,
                            onCheckedChange = onToggleSound
                        )
                        SettingSwitchRow(
                            label = "📳 Vibración Háptica",
                            checked = hapticsEnabled,
                            onCheckedChange = onToggleHaptics
                        )
                        SettingSwitchRow(
                            label = "🛡️ Reducir Sacudida (Screen Shake)",
                            checked = reducedMotion,
                            onCheckedChange = onToggleReducedMotion
                        )
                    }
                }

                Button(
                    onClick = onResumeClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3E2723),
                        contentColor = Color(0xFFF5E6CE)
                    )
                ) {
                    Text(text = "Continuar Partida", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onResumeClick()
                        onRestartClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.5.dp, Color(0xFF5D4037)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF3E2723)
                    )
                ) {
                    Text(text = "Reiniciar Nivel", fontWeight = FontWeight.SemiBold)
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

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF3E2723)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF3E2723),
                checkedTrackColor = Color(0xFFFFB300)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PauseDialogPreview() {
    CinthiasQuestTheme {
        PauseDialog(onResumeClick = {}, onQuitClick = {})
    }
}
