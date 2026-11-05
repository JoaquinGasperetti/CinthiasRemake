package com.hkemtrentainment.cinthiasquestr.ui.worldmap.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hkemtrentainment.cinthiasquestr.R
import com.hkemtrentainment.cinthiasquestr.data.PlayerRepository
import com.hkemtrentainment.cinthiasquestr.data.WorldRepository
import com.hkemtrentainment.cinthiasquestr.ui.components.ArcadeButtonStyle
import com.hkemtrentainment.cinthiasquestr.ui.components.BoosterCanvasIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.ChunkyArcadeButton
import com.hkemtrentainment.cinthiasquestr.ui.components.ForgedIronPanel
import com.hkemtrentainment.cinthiasquestr.ui.components.MedievalPalette
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalCrownIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalParchmentCard
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalStarIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.StreakFlameIcon
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.BoosterType

@Composable
fun LevelDetailDialog(
    levelNumber: Int,
    starsEarned: Int,
    winStreak: Int = 0,
    hammerCount: Int = 1,
    dynamiteCount: Int = 1,
    shuffleCount: Int = 1,
    onPlayClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val levelConfig = WorldRepository.getLevelConfig(levelNumber)
    val world = WorldRepository.getWorldForLevel(levelNumber)
    val streakStarterCount = PlayerRepository.computeStreakStarterPowerUpsCount(winStreak)
    val isBossLevel = levelNumber % 5 == 0
    val isHardLevel = !isBossLevel && (levelNumber % 3 == 0)

    Dialog(onDismissRequest = onDismiss) {
        RoyalParchmentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            borderColor = MedievalPalette.InkOutline,
            accentTrimColor = world.primaryColor,
            contentPadding = PaddingValues(0.dp)
        ) {
            // Cabecera Heráldica del Nivel con Gradiente del Mundo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(world.gradientColors))
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        .size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isBossLevel) {
                            RoyalCrownIcon(size = 18.dp)
                        }
                        Surface(
                            color = Color.Black.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MedievalPalette.GoldBright)
                        ) {
                            Text(
                                text = when {
                                    isBossLevel -> "${world.name.uppercase()} · NIVEL JEFE"
                                    isHardLevel -> "${world.name.uppercase()} · NIVEL DIFÍCIL"
                                    else -> world.name.uppercase()
                                },
                                color = MedievalPalette.GoldBright,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "NIVEL $levelNumber",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Serif
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Estrellas del Nivel con Emblemas Facetados
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { index ->
                            val isEarned = index < starsEarned
                            val starSize = if (index == 1) 42.dp else 32.dp
                            RoyalStarIcon(
                                filled = isEarned,
                                size = starSize
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Objetivo de Puntos y Límite de Movimientos en Placas de Hierro Forjado
                ForgedIronPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    trimColor = MedievalPalette.BrassFrame,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "META REAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFD7CCC8)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.gem_red),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${levelConfig.targetScore} pts",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MedievalPalette.GoldBright
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .height(34.dp)
                                .background(MedievalPalette.BrassFrame.copy(alpha = 0.6f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "MOVIMIENTOS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFD7CCC8)
                            )
                            Text(
                                text = "${levelConfig.moves}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                // Vista Previa Visual de la Bendición de Racha (Win Streak Starter Power-Ups)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFDF7EC),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, MedievalPalette.BrassFrame)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StreakFlameIcon(size = 16.dp)
                            Text(
                                text = if (streakStarterCount > 0) {
                                    "RACHA x$winStreak ACTIVA: +$streakStarterCount RELIQUIA${if (streakStarterCount > 1) "S" else ""} AL INICIAR"
                                } else {
                                    "GANA PARA ACTIVAR LA RACHA DE CINTHIA"
                                },
                                color = MedievalPalette.ParchmentTextDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val bonusLabels = listOf(
                                1 to "Rayo (x1)",
                                2 to "Bomba (x2)",
                                3 to "Arcoíris (x3)"
                            )
                            bonusLabels.forEach { (reqStreak, label) ->
                                val active = streakStarterCount >= reqStreak
                                Surface(
                                    color = if (active) Color(0xFF2E7D32) else Color(0xFFE0D0B8),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (active) MedievalPalette.GoldBright else MedievalPalette.BrassFrame
                                    )
                                ) {
                                    Text(
                                        text = if (active) "✓ $label" else label,
                                        color = if (active) Color.White else MedievalPalette.ParchmentTextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Arsenal de Boosters Disponibles para la Partida
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PreLevelBoosterBadge(type = BoosterType.HAMMER, count = hammerCount)
                    PreLevelBoosterBadge(type = BoosterType.DYNAMITE, count = dynamiteCount)
                    PreLevelBoosterBadge(type = BoosterType.SHUFFLE, count = shuffleCount)
                }

                // Botón Táctil 3D para Iniciar la Batalla
                ChunkyArcadeButton(
                    onClick = {
                        onDismiss()
                        onPlayClick(levelNumber)
                    },
                    style = if (isBossLevel) ArcadeButtonStyle.CRIMSON_ALERT else ArcadeButtonStyle.EMERALD_PLAY,
                    cornerRadius = 18.dp,
                    lipHeight = 6.dp,
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBossLevel) "¡DESAFIAR AL JEFE!" else "¡A LA BATALLA!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif
                    )
                }
            }
        }
    }
}

@Composable
private fun PreLevelBoosterBadge(
    type: BoosterType,
    count: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Surface(
            color = MedievalPalette.IronDark,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, MedievalPalette.GoldMid),
            modifier = Modifier.size(44.dp)
        ) {
            BoosterCanvasIcon(
                type = type,
                size = 24.dp,
                modifier = Modifier.padding(8.dp)
            )
        }
        Text(
            text = "${type.displayName} x$count",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MedievalPalette.ParchmentTextDark
        )
    }
}
