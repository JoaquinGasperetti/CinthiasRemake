package com.hkemtrentainment.cinthiasquestr.ui.tutorial

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hkemtrentainment.cinthiasquestr.R
import com.hkemtrentainment.cinthiasquestr.data.BoardShape
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
import com.hkemtrentainment.cinthiasquestr.ui.worldmap.components.BoardShapePreviewGrid

private data class TutorialStepInfo(
    val stepNumber: Int,
    val badge: String,
    val title: String,
    val cinthiaQuote: String,
    val summary: String
)

private val TUTORIAL_STEPS = listOf(
    TutorialStepInfo(
        stepNumber = 1,
        badge = "PASO 1 DE 6 · FUNDAMENTOS",
        title = "Combina 3 o Más Gemas",
        cinthiaQuote = "¡Bienvenida/o a mi guardia! Desliza una gema o toca dos casillas vecinas para intercambiarlas.",
        summary = "Alinea 3 o más gemas del mismo color en horizontal o vertical para romperlas, ganar puntos y alcanzar la Meta Real antes de quedarte sin movimientos."
    ),
    TutorialStepInfo(
        stepNumber = 2,
        badge = "PASO 2 DE 6 · FORJA ARCANO-REAL",
        title = "Crea Reliquias de Poder",
        cinthiaQuote = "¡Si combinas 4 o 5 gemas iguales de un solo movimiento, forjarás una reliquia explosiva!",
        summary = "Cada alineación grande deja un Power-Up en el tablero que puedes activar al combinarlo o tocarlo con otra pieza."
    ),
    TutorialStepInfo(
        stepNumber = 3,
        badge = "PASO 3 DE 6 · SINERGIAS SUPREMAS",
        title = "Combina 2 Power-Ups Entre Sí",
        cinthiaQuote = "¡El secreto de los maestros! Intercambia dos Power-Ups vecinos aunque sean de distinto color.",
        summary = "Unir dos reliquias desencadena efectos devastadores que limpian gran parte del tablero en un solo movimiento."
    ),
    TutorialStepInfo(
        stepNumber = 4,
        badge = "PASO 4 DE 6 · GEOMETRÍA TÁCTICA",
        title = "Tableros con Formas y Fosos",
        cinthiaQuote = "¡No todos los campos de batalla son cuadrados! Cada nivel cambia la silueta del tablero.",
        summary = "Las gemas caen verticalmente atravesando huecos, puentes y fosos intermedios. Aprovecha las reliquias precolocadas en cada nivel."
    ),
    TutorialStepInfo(
        stepNumber = 5,
        badge = "PASO 5 DE 6 · ARSENAL TÁCTICO",
        title = "Usa tus Boosters Sin Gastar Turno",
        cinthiaQuote = "¡Tus 3 reliquias de la barra inferior no consumen movimientos! Úsalas en momentos clave.",
        summary = "Toca un Booster en la parte inferior de la pantalla y elige la casilla donde quieras desatar su efecto."
    ),
    TutorialStepInfo(
        stepNumber = 6,
        badge = "PASO 6 DE 6 · RACHA Y RECOMPENSAS",
        title = "Racha de Cinthia y 3 Estrellas",
        cinthiaQuote = "¡Gana niveles seguidos para empezar cada batalla con hasta 3 Power-Ups gratis!",
        summary = "Los movimientos que te sobren al alcanzar la meta se convierten en Puntos de Bonus para lograr las 3 Estrellas y abrir el Cofre Real."
    )
)

/**
 * Tutorial interactivo completo paso a paso para jugadores nuevos (y consultable en cualquier momento
 * desde el Menú Principal o el Menú de Pausa).
 */
@Composable
fun TutorialDialog(
    onCompleteOrDismiss: () -> Unit
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = TUTORIAL_STEPS[currentStepIndex]
    val isLastStep = currentStepIndex == TUTORIAL_STEPS.lastIndex

    Dialog(
        onDismissRequest = onCompleteOrDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        RoyalParchmentCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 8.dp),
            cornerRadius = 22.dp,
            borderColor = MedievalPalette.InkOutline,
            accentTrimColor = MedievalPalette.GoldMid,
            contentPadding = PaddingValues(0.dp)
        ) {
            // Cabecera Real del Tutorial
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF1B5E20),
                                Color(0xFF2E7D32),
                                Color(0xFF1B5E20)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IconButton(
                    onClick = onCompleteOrDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        .size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar tutorial",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MedievalPalette.GoldBright)
                    ) {
                        Text(
                            text = step.badge,
                            color = MedievalPalette.GoldBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = step.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Banda de Cinthia Instructora con retrato grande y globo de consejo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFDF7EC))
                        .border(1.5.dp, MedievalPalette.BrassFrame, RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MedievalPalette.IronDark,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, MedievalPalette.GoldMid),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.cinthia_idle_01),
                            contentDescription = "Cinthia Instructora",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.padding(2.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CONSEJO DE CINTHIA",
                            color = MedievalPalette.GoldDeep,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = step.cinthiaQuote,
                            color = MedievalPalette.ParchmentTextDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Panel Visual Interactivo de cada Etapa
                AnimatedContent(
                    targetState = currentStepIndex,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tutorialStepVisual"
                ) { stepIdx ->
                    ForgedIronPanel(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        trimColor = MedievalPalette.GoldMid,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        when (stepIdx) {
                            0 -> Step1MatchBasicsVisual()
                            1 -> Step2PowerUpsVisual()
                            2 -> Step3SynergiesVisual()
                            3 -> Step4BoardShapesVisual()
                            4 -> Step5BoostersVisual()
                            else -> Step6StreakAndStarsVisual()
                        }
                    }
                }

                // Explicación resumida clara
                Text(
                    text = step.summary,
                    color = MedievalPalette.ParchmentTextDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Indicadores de página (Puntos cliqueables)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TUTORIAL_STEPS.forEachIndexed { idx, _ ->
                        val active = idx == currentStepIndex
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (active) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) MedievalPalette.EmeraldTop else MedievalPalette.BrassFrame.copy(alpha = 0.45f)
                                )
                                .clickable { currentStepIndex = idx }
                        )
                    }
                }

                // Botonera de Navegación ("Anterior" / "Siguiente" / "¡A JUGAR!")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStepIndex > 0) {
                        ChunkyArcadeButton(
                            onClick = { currentStepIndex-- },
                            style = ArcadeButtonStyle.IRON_SLATE,
                            cornerRadius = 14.dp,
                            lipHeight = 4.dp,
                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp),
                            modifier = Modifier.weight(0.42f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ANTERIOR",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    ChunkyArcadeButton(
                        onClick = {
                            if (isLastStep) {
                                onCompleteOrDismiss()
                            } else {
                                currentStepIndex++
                            }
                        },
                        style = if (isLastStep) ArcadeButtonStyle.EMERALD_PLAY else ArcadeButtonStyle.ROYAL_GOLD,
                        cornerRadius = 14.dp,
                        lipHeight = 5.dp,
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 16.dp),
                        modifier = Modifier.weight(if (currentStepIndex > 0) 0.58f else 1f)
                    ) {
                        Text(
                            text = if (isLastStep) "¡ENTENDIDO, A JUGAR!" else "SIGUIENTE PASO",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isLastStep) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (!isLastStep) {
                    TextButton(
                        onClick = onCompleteOrDismiss,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            text = "Saltar tutorial e ir al tablero",
                            color = MedievalPalette.ParchmentTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step1MatchBasicsVisual() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "INTERCAMBIA Y ALINEA 3 GEMAS IGUALES",
            color = MedievalPalette.GoldBright,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TutorialGemCell(resId = R.drawable.gem_red, isHighlighted = true)
            TutorialGemCell(resId = R.drawable.gem_red, isHighlighted = true)
            TutorialGemCell(resId = R.drawable.gem_blue, isSelected = true)
            Text(
                text = "⇄",
                color = MedievalPalette.GoldBright,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            TutorialGemCell(resId = R.drawable.gem_red, isSelected = true)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TutorialMiniTipChip(label = "Desliza (Swipe)")
            TutorialMiniTipChip(label = "O toca 2 gemas")
            TutorialMiniTipChip(label = "Pista tras 4.5s")
        }
    }
}

@Composable
private fun Step2PowerUpsVisual() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PowerUpRuleRow(
            iconRes = R.drawable.line_blaster_blue,
            badge = "4 EN LÍNEA",
            title = "Rayo de Línea",
            desc = "Destruye toda la fila y la columna en cruz."
        )
        PowerUpRuleRow(
            iconRes = R.drawable.bomb_red,
            badge = "5 EN L o T",
            title = "Bomba Real",
            desc = "Detona un área explosiva de 3×3 casillas."
        )
        PowerUpRuleRow(
            iconRes = R.drawable.color_bomb,
            badge = "5 EN RECTA",
            title = "Esfera Arcoíris",
            desc = "Únela con cualquier gema para borrar todo su color."
        )
    }
}

@Composable
private fun Step3SynergiesVisual() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        SynergyComboRow(
            leftRes = R.drawable.line_blaster_blue,
            rightRes = R.drawable.bomb_red,
            title = "Rayo + Bomba = Súper Cruz 3×3",
            desc = "Arrasa 3 filas y 3 columnas completas del tablero."
        )
        SynergyComboRow(
            leftRes = R.drawable.color_bomb,
            rightRes = R.drawable.line_blaster_green,
            title = "Arcoíris + Rayo / Bomba",
            desc = "Transforma todas las gemas de ese color en reliquias y las detona."
        )
        SynergyComboRow(
            leftRes = R.drawable.color_bomb,
            rightRes = R.drawable.color_bomb,
            title = "Doble Arcoíris = Limpieza Total",
            desc = "¡Destruye absolutamente todas las casillas del tablero!"
        )
    }
}

@Composable
private fun Step4BoardShapesVisual() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "16 SILUETAS DISTINTAS CON GRAVEDAD ADAPTATIVA",
            color = MedievalPalette.GoldBright,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShapeShowcaseItem(shape = BoardShape.ROYAL_CROSS, color = Color(0xFF81C784), label = "Cruz")
            ShapeShowcaseItem(shape = BoardShape.DIAMOND_GEM, color = Color(0xFFFFB74D), label = "Diamante")
            ShapeShowcaseItem(shape = BoardShape.HEART_CHARM, color = Color(0xFFF48FB1), label = "Corazón")
            ShapeShowcaseItem(shape = BoardShape.DONUT_RING, color = Color(0xFF4FC3F7), label = "Anillo")
        }
        Text(
            text = "Las gemas atraviesan los huecos al caer y los niveles avanzados incluyen reliquias precolocadas en el tablero.",
            color = Color(0xFFE0D0B8),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Step5BoostersVisual() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BoosterRuleRow(
            type = BoosterType.HAMMER,
            title = "Martillo Real",
            desc = "Rompe cualquier gema individual o activa un Power-Up sin gastar movimiento."
        )
        BoosterRuleRow(
            type = BoosterType.DYNAMITE,
            title = "Dinamita de Asedio",
            desc = "Hace volar un bloque de 3×3 casillas alrededor del punto que toques."
        )
        BoosterRuleRow(
            type = BoosterType.SHUFFLE,
            title = "Barajar Arcano",
            desc = "Mezcla todas las gemas del tablero al instante para abrir nuevos combos."
        )
    }
}

@Composable
private fun Step6StreakAndStarsVisual() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StreakFlameIcon(size = 24.dp)
            Text(
                text = "BENDICIÓN DE RACHA DE VICTORIAS",
                color = MedievalPalette.GoldBright,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TutorialMiniTipChip(label = "Racha x1: +1 Rayo")
            TutorialMiniTipChip(label = "Racha x2: +Bomba")
            TutorialMiniTipChip(label = "Racha x3: +Arcoíris")
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RoyalStarIcon(filled = true, size = 20.dp)
            RoyalStarIcon(filled = true, size = 24.dp)
            RoyalStarIcon(filled = true, size = 20.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Cada movimiento sobrante suma +60 pts de Bonus Final",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TutorialGemCell(
    resId: Int,
    isHighlighted: Boolean = false,
    isSelected: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF3B2621))
            .border(
                width = if (isSelected || isHighlighted) 2.dp else 1.dp,
                color = when {
                    isSelected -> Color(0xFFFFD700)
                    isHighlighted -> Color(0xFF69F0AE)
                    else -> MedievalPalette.BrassFrame
                },
                shape = RoundedCornerShape(10.dp)
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun TutorialMiniTipChip(label: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MedievalPalette.BrassFrame)
    ) {
        Text(
            text = label,
            color = Color(0xFFF6EAD4),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PowerUpRuleRow(
    iconRes: Int,
    badge: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1215))
                .border(1.5.dp, MedievalPalette.GoldMid, RoundedCornerShape(10.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(32.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                Surface(
                    color = MedievalPalette.EmeraldBottom,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = desc,
                color = Color(0xFFD7CCC8),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun SynergyComboRow(
    leftRes: Int,
    rightRes: Int,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Image(
                painter = painterResource(id = leftRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "+",
                color = MedievalPalette.GoldBright,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
            Image(
                painter = painterResource(id = rightRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MedievalPalette.GoldBright,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = desc,
                color = Color(0xFFD7CCC8),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ShapeShowcaseItem(
    shape: BoardShape,
    color: Color,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        BoardShapePreviewGrid(
            shape = shape,
            primaryColor = color,
            size = 44.dp
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BoosterRuleRow(
    type: BoosterType,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1215))
                .border(1.5.dp, MedievalPalette.GoldMid, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            BoosterCanvasIcon(type = type, size = 24.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MedievalPalette.GoldBright,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = desc,
                color = Color(0xFFD7CCC8),
                fontSize = 10.sp
            )
        }
    }
}
