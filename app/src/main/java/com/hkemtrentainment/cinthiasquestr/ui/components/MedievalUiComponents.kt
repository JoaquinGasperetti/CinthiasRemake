package com.hkemtrentainment.cinthiasquestr.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkemtrentainment.cinthiasquestr.R
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.BoosterType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Paleta cromática diegética extraída de los assets reales de Cinthia's Quest:
 * - Piedra y pizarra de mazmorra (`bg_game.png` / `ui_top_bar.png`)
 * - Pergamino envejecido (`board_frame.png`)
 * - Bronce antiguo, oro real y contornos de tinta negra (`cinthia_idle_01.png`)
 */
object MedievalPalette {
    val InkOutline = Color(0xFF140D0B)
    val DungeonAbyss = Color(0xFF161115)
    val IronDark = Color(0xFF231B20)
    val IronSlate = Color(0xFF352A31)
    val IronHighlight = Color(0xFF4F414A)

    val BrassFrame = Color(0xFF8C734B)
    val BrassDark = Color(0xFF59462B)
    val GoldBright = Color(0xFFFFD54F)
    val GoldMid = Color(0xFFF59E0B)
    val GoldDeep = Color(0xFFB45309)

    val ParchmentLight = Color(0xFFF6EAD4)
    val ParchmentMid = Color(0xFFE6D2B2)
    val ParchmentDark = Color(0xFFCBB089)
    val ParchmentTextDark = Color(0xFF2B1B14)
    val ParchmentTextMuted = Color(0xFF5C4033)

    val EmeraldTop = Color(0xFF34C759)
    val EmeraldBottom = Color(0xFF1E8E3E)
    val EmeraldLip = Color(0xFF0F4D20)

    val CrimsonTop = Color(0xFFEF5350)
    val CrimsonBottom = Color(0xFFC62828)
    val CrimsonLip = Color(0xFF6E1111)

    val CobaltTop = Color(0xFF29B6F6)
    val CobaltBottom = Color(0xFF0277BD)
    val CobaltLip = Color(0xFF013A5E)
}

enum class ArcadeButtonStyle(
    val topColor: Color,
    val bottomColor: Color,
    val lipColor: Color,
    val contentColor: Color,
    val borderColor: Color = MedievalPalette.InkOutline
) {
    EMERALD_PLAY(
        topColor = Color(0xFF43D165),
        bottomColor = Color(0xFF1E8E3E),
        lipColor = Color(0xFF0D4A1D),
        contentColor = Color.White
    ),
    ROYAL_GOLD(
        topColor = Color(0xFFFFD54F),
        bottomColor = Color(0xFFF59E0B),
        lipColor = Color(0xFF8C4A00),
        contentColor = Color(0xFF2B180A)
    ),
    CRIMSON_ALERT(
        topColor = Color(0xFFFF6659),
        bottomColor = Color(0xFFC62828),
        lipColor = Color(0xFF630E0E),
        contentColor = Color.White
    ),
    IRON_SLATE(
        topColor = Color(0xFF52444D),
        bottomColor = Color(0xFF332930),
        lipColor = Color(0xFF171115),
        contentColor = Color(0xFFF6EAD4)
    ),
    COBALT_MAGIC(
        topColor = Color(0xFF4FC3F7),
        bottomColor = Color(0xFF0277BD),
        lipColor = Color(0xFF003B63),
        contentColor = Color.White
    )
}

/**
 * Botón táctil 3D con bisel físico inferior, brillo especular superior y compresión real al presionar.
 * Sustituye los botones planos de Material 3 para lograr un acabado de juego móvil de alta retención.
 */
@Composable
fun ChunkyArcadeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ArcadeButtonStyle = ArcadeButtonStyle.ROYAL_GOLD,
    enabled: Boolean = true,
    cornerRadius: Dp = 16.dp,
    lipHeight: Dp = 5.dp,
    badgeCount: Int = 0,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) lipHeight - 1.dp else 0.dp,
        animationSpec = tween(durationMillis = 70),
        label = "buttonPressOffset"
    )

    val topCol = if (enabled) style.topColor else Color(0xFF6D6369)
    val botCol = if (enabled) style.bottomColor else Color(0xFF494146)
    val lipCol = if (enabled) style.lipColor else Color(0xFF282326)
    val txtCol = if (enabled) style.contentColor else Color(0xFFBDB3B8)

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        // Base 3D inferior (Labio / Sombra sólida)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = lipHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .background(lipCol)
                .border(2.5.dp, style.borderColor, RoundedCornerShape(cornerRadius))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        // Cara frontal que desciende físicamente al tocar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = pressOffset)
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(topCol, botCol)
                    )
                )
                .border(2.5.dp, style.borderColor, RoundedCornerShape(cornerRadius))
        ) {
            // Reflejo especular superior estilo arcade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (enabled) 0.32f else 0.12f))
                    .align(Alignment.TopCenter)
            )

            CompositionLocalProvider(LocalContentColor provides txtCol) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }

        if (badgeCount > 0) {
            NotificationBadgeDot(
                text = "$badgeCount",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
            )
        }
    }
}

/**
 * Contenedor diegético de Pergamino Real con textura de `board_frame.png` y remaches de bronce en las esquinas.
 */
@Composable
fun RoyalParchmentCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderColor: Color = MedievalPalette.InkOutline,
    accentTrimColor: Color = MedievalPalette.BrassFrame,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MedievalPalette.ParchmentLight,
                        MedievalPalette.ParchmentMid,
                        MedievalPalette.ParchmentDark
                    )
                )
            )
            .border(3.dp, borderColor, RoundedCornerShape(cornerRadius))
    ) {
        Image(
            painter = painterResource(id = R.drawable.board_frame),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.35f,
            modifier = Modifier.matchParentSize()
        )

        // Marco interior de bronce con remaches en las 4 esquinas
        Canvas(modifier = Modifier.matchParentSize()) {
            val inset = 6.dp.toPx()
            drawRoundRect(
                color = accentTrimColor.copy(alpha = 0.65f),
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius((cornerRadius - 4.dp).toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )
            val rivetRadius = 3.2.dp.toPx()
            val rInset = 12.dp.toPx()
            val corners = listOf(
                Offset(rInset, rInset),
                Offset(size.width - rInset, rInset),
                Offset(rInset, size.height - rInset),
                Offset(size.width - rInset, size.height - rInset)
            )
            corners.forEach { pt ->
                drawCircle(color = MedievalPalette.InkOutline, radius = rivetRadius + 1.2f, center = pt)
                drawCircle(color = MedievalPalette.GoldBright, radius = rivetRadius, center = pt)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Panel diegético de Hierro Forjado y Bronce (inspirado en `ui_top_bar.png`).
 */
@Composable
fun ForgedIronPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    trimColor: Color = MedievalPalette.BrassFrame,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MedievalPalette.IronSlate,
                        MedievalPalette.IronDark,
                        MedievalPalette.DungeonAbyss
                    )
                )
            )
            .border(2.5.dp, MedievalPalette.InkOutline, RoundedCornerShape(cornerRadius))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(2.5.dp)
                .border(1.5.dp, trimColor.copy(alpha = 0.75f), RoundedCornerShape(cornerRadius - 2.dp))
        )
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Punto de notificación pulsante de alta visibilidad (Live-Ops Attention Hook).
 */
@Composable
fun NotificationBadgeDot(
    text: String = "!",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(22.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFF5252), Color(0xFFB71C1C))
                )
            )
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Botón de acceso rápido lateral (Side Dock) estilo Royal Match / Brawl Stars,
 * con icono diegético, barra o etiqueta de progreso y globo de notificación.
 */
@Composable
fun SideDockActionButton(
    label: String,
    progressText: String,
    hasAlert: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val yOffset by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        animationSpec = tween(60),
        label = "dockPress"
    )

    Box(
        modifier = modifier
            .width(74.dp)
            .offset(y = yOffset)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MedievalPalette.IronHighlight,
                                MedievalPalette.IronDark
                            )
                        )
                    )
                    .border(2.5.dp, MedievalPalette.InkOutline, RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .border(1.5.dp, accentColor.copy(alpha = 0.85f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                    content = iconContent
                )
            }

            // Pastilla inferior con estado/progreso
            Surface(
                modifier = Modifier.offset(y = (-7).dp),
                color = MedievalPalette.IronDark,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.5.dp, accentColor)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label.uppercase(),
                        color = Color(0xFFF6EAD4),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = progressText,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
        }

        if (hasAlert) {
            NotificationBadgeDot(
                text = "!",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = (-4).dp)
            )
        }
    }
}

/**
 * Fondo de partículas de antorcha y brasas flotantes renderizadas en Canvas sobre la mazmorra.
 */
@Composable
fun TorchEmbersOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "torchEmbers")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "emberPhase"
    )
    val torchFlicker by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "torchFlicker"
    )

    val seeds = remember {
        List(22) { idx ->
            val x = ((idx * 37) % 100) / 100f
            val yOffset = ((idx * 53) % 100) / 100f
            val speed = 0.6f + (idx % 5) * 0.2f
            val radius = 2.2f + (idx % 4) * 1.3f
            floatArrayOf(x, yOffset, speed, radius)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Resplandor cálido de la antorcha en la pared superior izquierda (coincide con bg_game.png)
        val torchCenter = Offset(size.width * 0.365f, size.height * 0.135f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF9800).copy(alpha = 0.24f * torchFlicker),
                    Color(0xFFFF5722).copy(alpha = 0.08f * torchFlicker),
                    Color.Transparent
                ),
                center = torchCenter,
                radius = size.minDimension * 0.45f * torchFlicker
            ),
            radius = size.minDimension * 0.45f * torchFlicker,
            center = torchCenter
        )

        seeds.forEachIndexed { idx, seed ->
            val rawY = (seed[1] - phase * seed[2]) % 1f
            val normY = if (rawY < 0f) rawY + 1f else rawY
            val sway = sin((phase * 2f * PI + idx).toFloat()) * 14.dp.toPx()
            val px = seed[0] * size.width + sway
            val py = normY * size.height
            val alpha = (sin(normY * PI).toFloat()).coerceIn(0f, 1f) * 0.65f
            val color = if (idx % 3 == 0) Color(0xFFFFD54F) else Color(0xFFFF7043)

            drawCircle(
                color = color.copy(alpha = alpha),
                radius = seed[3].dp.toPx(),
                center = Offset(px, py)
            )
        }
    }
}

// ============================================================================
// ICONOGRAFÍA VECTORIAL / CANVAS DIEGÉTICA (CERO EMOJIS UNICODE)
// ============================================================================

/**
 * Moneda de Oro Real dibujada en Canvas con bisel metálico y corona grabada.
 */
@Composable
fun GoldCoinIcon(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = center
        drawCircle(color = MedievalPalette.InkOutline, radius = r, center = c)
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFF176), Color(0xFFFFB300), Color(0xFFE65100))
            ),
            radius = r * 0.86f,
            center = c
        )
        drawCircle(
            color = Color(0xFFFFF8E1).copy(alpha = 0.7f),
            radius = r * 0.64f,
            center = c,
            style = Stroke(width = r * 0.12f)
        )
        // Rombo central grabado
        val path = Path().apply {
            moveTo(c.x, c.y - r * 0.38f)
            lineTo(c.x + r * 0.30f, c.y)
            lineTo(c.x, c.y + r * 0.38f)
            lineTo(c.x - r * 0.30f, c.y)
            close()
        }
        drawPath(path = path, color = Color(0xFFFFF59D))
    }
}

/**
 * Estrella Real facetada con borde de tinta y centro brillante.
 */
@Composable
fun RoyalStarIcon(
    filled: Boolean = true,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val outerR = this.size.minDimension * 0.48f
        val innerR = outerR * 0.44f
        val starPath = createStarPath(center.x, center.y, outerR, innerR)

        drawPath(
            path = starPath,
            color = MedievalPalette.InkOutline,
            style = Stroke(width = this.size.minDimension * 0.14f, join = StrokeJoin.Round)
        )
        drawPath(
            path = starPath,
            brush = if (filled) {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF59D), Color(0xFFFFCA28), Color(0xFFF57F17))
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF4F434A), Color(0xFF2E252A))
                )
            }
        )
        if (filled) {
            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                radius = outerR * 0.16f,
                center = Offset(center.x - outerR * 0.12f, center.y - outerR * 0.15f)
            )
        }
    }
}

private fun createStarPath(cx: Float, cy: Float, outerR: Float, innerR: Float): Path {
    val path = Path()
    val angleStep = (PI / 5.0).toFloat()
    var angle = (-PI / 2.0).toFloat()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outerR else innerR
        val x = cx + cos(angle) * r
        val y = cy + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        angle += angleStep
    }
    path.close()
    return path
}

/**
 * Llama de Racha de Victorias (Win Streak Flame) dibujada en Canvas.
 */
@Composable
fun StreakFlameIcon(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val outerFlame = Path().apply {
            moveTo(w * 0.5f, h * 0.06f)
            cubicTo(w * 0.88f, h * 0.32f, w * 0.92f, h * 0.72f, w * 0.5f, h * 0.94f)
            cubicTo(w * 0.08f, h * 0.72f, w * 0.12f, h * 0.32f, w * 0.5f, h * 0.06f)
            close()
        }
        drawPath(
            path = outerFlame,
            color = MedievalPalette.InkOutline,
            style = Stroke(width = w * 0.12f, join = StrokeJoin.Round)
        )
        drawPath(
            path = outerFlame,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFCA28), Color(0xFFFF5722), Color(0xFFD32F2F))
            )
        )
        val innerFlame = Path().apply {
            moveTo(w * 0.5f, h * 0.34f)
            cubicTo(w * 0.72f, h * 0.52f, w * 0.70f, h * 0.78f, w * 0.5f, h * 0.88f)
            cubicTo(w * 0.30f, h * 0.78f, w * 0.28f, h * 0.52f, w * 0.5f, h * 0.34f)
            close()
        }
        drawPath(path = innerFlame, color = Color(0xFFFFF59D))
    }
}

/**
 * Cofre del Tesoro Medieval dibujado en Canvas (para Racha Diaria y Cofre de Estrellas).
 */
@Composable
fun TreasureChestIcon(
    isOpen: Boolean = false,
    isRoyalGold: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val bodyColor = if (isRoyalGold) Color(0xFFC62828) else Color(0xFF6D4C41)
        val trimColor = Color(0xFFFFCA28)

        if (isOpen) {
            // Resplandor de tesoro abierto
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF59D).copy(alpha = 0.8f), Color.Transparent)
                ),
                radius = w * 0.55f,
                center = Offset(w * 0.5f, h * 0.45f)
            )
        }

        // Base del cofre
        drawRoundRect(
            color = MedievalPalette.InkOutline,
            topLeft = Offset(w * 0.10f, h * 0.42f),
            size = Size(w * 0.80f, h * 0.48f),
            cornerRadius = CornerRadius(w * 0.10f)
        )
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(w * 0.14f, h * 0.46f),
            size = Size(w * 0.72f, h * 0.40f),
            cornerRadius = CornerRadius(w * 0.07f)
        )

        // Tapa del cofre
        val lidTop = if (isOpen) h * 0.14f else h * 0.22f
        drawRoundRect(
            color = MedievalPalette.InkOutline,
            topLeft = Offset(w * 0.08f, lidTop),
            size = Size(w * 0.84f, h * 0.28f),
            cornerRadius = CornerRadius(w * 0.12f)
        )
        drawRoundRect(
            color = bodyColor.copy(red = (bodyColor.red * 1.12f).coerceAtMost(1f)),
            topLeft = Offset(w * 0.12f, lidTop + h * 0.04f),
            size = Size(w * 0.76f, h * 0.20f),
            cornerRadius = CornerRadius(w * 0.08f)
        )

        // Bandas metálicas doradas y cerradura
        drawRect(
            color = trimColor,
            topLeft = Offset(w * 0.22f, lidTop + h * 0.04f),
            size = Size(w * 0.09f, h * 0.62f)
        )
        drawRect(
            color = trimColor,
            topLeft = Offset(w * 0.69f, lidTop + h * 0.04f),
            size = Size(w * 0.09f, h * 0.62f)
        )
        drawRoundRect(
            color = trimColor,
            topLeft = Offset(w * 0.41f, h * 0.38f),
            size = Size(w * 0.18f, h * 0.22f),
            cornerRadius = CornerRadius(w * 0.04f)
        )
        drawCircle(
            color = MedievalPalette.InkOutline,
            radius = w * 0.035f,
            center = Offset(w * 0.5f, h * 0.49f)
        )
    }
}

/**
 * Iconografía personalizada en Canvas para los 3 Boosters del juego:
 * - HAMMER: Martillo de Guerra Forjado (reemplaza Icons.Default.Build)
 * - DYNAMITE: Bomba Real con mecha encendida (reemplaza Icons.Default.Clear)
 * - SHUFFLE: Remolino Arcano de Gemas (reemplaza Icons.Default.Refresh)
 */
@Composable
fun BoosterCanvasIcon(
    type: BoosterType,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    Canvas(modifier = modifier.size(size)) {
        when (type) {
            BoosterType.HAMMER -> drawWarHammer()
            BoosterType.DYNAMITE -> drawRoyalBomb()
            BoosterType.SHUFFLE -> drawArcaneShuffle()
        }
    }
}

private fun DrawScope.drawWarHammer() {
    val w = size.width
    val h = size.height
    // Mango de madera diagonal
    drawLine(
        color = MedievalPalette.InkOutline,
        start = Offset(w * 0.22f, h * 0.82f),
        end = Offset(w * 0.68f, h * 0.34f),
        strokeWidth = w * 0.20f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF8D5524),
        start = Offset(w * 0.22f, h * 0.82f),
        end = Offset(w * 0.68f, h * 0.34f),
        strokeWidth = w * 0.12f,
        cap = StrokeCap.Round
    )
    // Cabeza del martillo de acero y oro
    val headPath = Path().apply {
        moveTo(w * 0.42f, h * 0.16f)
        lineTo(w * 0.86f, h * 0.48f)
        lineTo(w * 0.72f, h * 0.64f)
        lineTo(w * 0.28f, h * 0.32f)
        close()
    }
    drawPath(
        path = headPath,
        color = MedievalPalette.InkOutline,
        style = Stroke(width = w * 0.10f, join = StrokeJoin.Round)
    )
    drawPath(
        path = headPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFECEFF1), Color(0xFF90A4AE), Color(0xFF546E7A))
        )
    )
    // Anillo dorado central
    drawCircle(
        color = Color(0xFFFFCA28),
        radius = w * 0.09f,
        center = Offset(w * 0.57f, h * 0.40f)
    )
}

private fun DrawScope.drawRoyalBomb() {
    val w = size.width
    val h = size.height
    val bombCenter = Offset(w * 0.46f, h * 0.58f)
    val bombRadius = w * 0.32f

    // Mecha curva
    val fusePath = Path().apply {
        moveTo(w * 0.58f, h * 0.32f)
        quadraticTo(w * 0.72f, h * 0.14f, w * 0.84f, h * 0.22f)
    }
    drawPath(
        path = fusePath,
        color = Color(0xFFD7CCC8),
        style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
    )
    // Chispa brillante
    drawCircle(
        color = Color(0xFFFF5722),
        radius = w * 0.12f,
        center = Offset(w * 0.84f, h * 0.22f)
    )
    drawCircle(
        color = Color(0xFFFFF59D),
        radius = w * 0.06f,
        center = Offset(w * 0.84f, h * 0.22f)
    )

    // Esfera de bomba
    drawCircle(
        color = MedievalPalette.InkOutline,
        radius = bombRadius + w * 0.04f,
        center = bombCenter
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF546E7A), Color(0xFF263238), Color(0xFF11171A)),
            center = Offset(bombCenter.x - bombRadius * 0.3f, bombCenter.y - bombRadius * 0.3f),
            radius = bombRadius * 1.4f
        ),
        radius = bombRadius,
        center = bombCenter
    )
    // Banda dorada real
    drawCircle(
        color = Color(0xFFFFCA28),
        radius = bombRadius * 0.45f,
        center = bombCenter,
        style = Stroke(width = w * 0.06f)
    )
}

private fun DrawScope.drawArcaneShuffle() {
    val w = size.width
    val c = center
    val r = w * 0.32f

    drawArc(
        color = Color(0xFF4FC3F7),
        startAngle = 25f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(c.x - r, c.y - r),
        size = Size(r * 2, r * 2),
        style = Stroke(width = w * 0.12f, cap = StrokeCap.Round)
    )
    drawArc(
        color = Color(0xFFFFCA28),
        startAngle = 205f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(c.x - r, c.y - r),
        size = Size(r * 2, r * 2),
        style = Stroke(width = w * 0.12f, cap = StrokeCap.Round)
    )
    drawCircle(
        color = Color.White,
        radius = w * 0.11f,
        center = c
    )
}

/**
 * Escudo Real de Rango / Maestría dibujado en Canvas.
 */
@Composable
fun KnightShieldIcon(
    rankTier: Int = 1,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.06f)
            lineTo(w * 0.88f, h * 0.20f)
            lineTo(w * 0.84f, h * 0.58f)
            quadraticTo(w * 0.76f, h * 0.84f, w * 0.5f, h * 0.95f)
            quadraticTo(w * 0.24f, h * 0.84f, w * 0.16f, h * 0.58f)
            lineTo(w * 0.12f, h * 0.20f)
            close()
        }
        val shieldColors = when (rankTier) {
            1 -> listOf(Color(0xFF8D6E63), Color(0xFF4E342E))
            2 -> listOf(Color(0xFF42A5F5), Color(0xFF1565C0))
            3 -> listOf(Color(0xFFAB47BC), Color(0xFF6A1B9A))
            4 -> listOf(Color(0xFFFFCA28), Color(0xFFE65100))
            else -> listOf(Color(0xFFFF5252), Color(0xFFB71C1C))
        }
        drawPath(
            path = shield,
            color = MedievalPalette.InkOutline,
            style = Stroke(width = w * 0.12f, join = StrokeJoin.Round)
        )
        drawPath(
            path = shield,
            brush = Brush.verticalGradient(shieldColors)
        )
        drawPath(
            path = shield,
            color = Color(0xFFFFD54F),
            style = Stroke(width = w * 0.06f, join = StrokeJoin.Round)
        )
        // Estrella central del escudo
        val star = createStarPath(w * 0.5f, h * 0.48f, w * 0.22f, w * 0.10f)
        drawPath(path = star, color = Color(0xFFFFF59D))
    }
}

/**
 * Pergamino de Misiones dibujado en Canvas.
 */
@Composable
fun QuestScrollIcon(modifier: Modifier = Modifier, size: Dp = 26.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(
            color = MedievalPalette.InkOutline,
            topLeft = Offset(w * 0.16f, h * 0.10f),
            size = Size(w * 0.68f, h * 0.80f),
            cornerRadius = CornerRadius(w * 0.10f)
        )
        drawRoundRect(
            color = Color(0xFFF6EAD4),
            topLeft = Offset(w * 0.20f, h * 0.14f),
            size = Size(w * 0.60f, h * 0.72f),
            cornerRadius = CornerRadius(w * 0.08f)
        )
        // Rodillos superior e inferior de madera y oro
        drawRoundRect(
            color = Color(0xFFFFB300),
            topLeft = Offset(w * 0.10f, h * 0.10f),
            size = Size(w * 0.80f, h * 0.13f),
            cornerRadius = CornerRadius(w * 0.06f)
        )
        drawRoundRect(
            color = Color(0xFFFFB300),
            topLeft = Offset(w * 0.10f, h * 0.77f),
            size = Size(w * 0.80f, h * 0.13f),
            cornerRadius = CornerRadius(w * 0.06f)
        )
        // Líneas de escritura
        for (i in 0..2) {
            val y = h * (0.36f + i * 0.13f)
            drawLine(
                color = Color(0xFF5D4037),
                start = Offset(w * 0.30f, y),
                end = Offset(w * 0.70f, y),
                strokeWidth = w * 0.06f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Corona de Jefe / Victoria dibujada en Canvas.
 */
@Composable
fun RoyalCrownIcon(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val crown = Path().apply {
            moveTo(w * 0.12f, h * 0.78f)
            lineTo(w * 0.08f, h * 0.30f)
            lineTo(w * 0.32f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.18f)
            lineTo(w * 0.68f, h * 0.52f)
            lineTo(w * 0.92f, h * 0.30f)
            lineTo(w * 0.88f, h * 0.78f)
            close()
        }
        drawPath(
            path = crown,
            color = MedievalPalette.InkOutline,
            style = Stroke(width = w * 0.12f, join = StrokeJoin.Round)
        )
        drawPath(
            path = crown,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFF176), Color(0xFFFFB300), Color(0xFFE65100))
            )
        )
        drawCircle(color = Color(0xFFE53935), radius = w * 0.07f, center = Offset(w * 0.50f, h * 0.60f))
    }
}
