package com.hkemtrentainment.cinthiasquestr.ui.gameboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hkemtrentainment.cinthiasquestr.ui.components.ArcadeButtonStyle
import com.hkemtrentainment.cinthiasquestr.ui.components.ChunkyArcadeButton
import com.hkemtrentainment.cinthiasquestr.ui.components.ForgedIronPanel
import com.hkemtrentainment.cinthiasquestr.ui.components.MedievalPalette
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalCrownIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalParchmentCard
import com.hkemtrentainment.cinthiasquestr.ui.theme.CinthiasQuestTheme

@Composable
fun PauseDialog(
    soundEnabled: Boolean = true,
    hapticsEnabled: Boolean = true,
    reducedMotion: Boolean = false,
    onToggleSound: (Boolean) -> Unit = {},
    onToggleHaptics: (Boolean) -> Unit = {},
    onToggleReducedMotion: (Boolean) -> Unit = {},
    onOpenTutorialClick: () -> Unit = {},
    onRestartClick: () -> Unit = {},
    onResumeClick: () -> Unit,
    onQuitClick: () -> Unit
) {
    Dialog(onDismissRequest = onResumeClick) {
        RoyalParchmentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            contentPadding = PaddingValues(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoyalCrownIcon(size = 24.dp)
                    Text(
                        text = "GUARDIA EN PAUSA",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = MedievalPalette.ParchmentTextDark
                    )
                }

                // Panel diegético de ajustes de combate y accesibilidad
                ForgedIronPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    trimColor = MedievalPalette.BrassFrame,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SettingSwitchRow(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            label = "Efectos de Sonido",
                            checked = soundEnabled,
                            onCheckedChange = onToggleSound
                        )
                        SettingSwitchRow(
                            icon = Icons.Default.Vibration,
                            label = "Respuesta Háptica",
                            checked = hapticsEnabled,
                            onCheckedChange = onToggleHaptics
                        )
                        SettingSwitchRow(
                            icon = Icons.Default.Shield,
                            label = "Estabilizar Cámara (Sin Shake)",
                            checked = reducedMotion,
                            onCheckedChange = onToggleReducedMotion
                        )
                    }
                }

                ChunkyArcadeButton(
                    onClick = onResumeClick,
                    style = ArcadeButtonStyle.EMERALD_PLAY,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONTINUAR BATALLA",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }

                ChunkyArcadeButton(
                    onClick = {
                        onResumeClick()
                        onOpenTutorialClick()
                    },
                    style = ArcadeButtonStyle.COBALT_MAGIC,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CÓMO JUGAR · TUTORIAL",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                ChunkyArcadeButton(
                    onClick = {
                        onResumeClick()
                        onRestartClick()
                    },
                    style = ArcadeButtonStyle.ROYAL_GOLD,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "REINICIAR NIVEL",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                ChunkyArcadeButton(
                    onClick = onQuitClick,
                    style = ArcadeButtonStyle.IRON_SLATE,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "RETIRARSE AL MAPA DEL REINO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                color = Color(0xFF140E12),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MedievalPalette.BrassFrame),
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MedievalPalette.GoldBright,
                    modifier = Modifier.padding(5.dp)
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF6EAD4)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF2B180A),
                checkedTrackColor = MedievalPalette.GoldBright,
                uncheckedThumbColor = Color(0xFFBDB3B8),
                uncheckedTrackColor = Color(0xFF1B1418)
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
