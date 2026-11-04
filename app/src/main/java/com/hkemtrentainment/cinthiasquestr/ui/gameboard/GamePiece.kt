package com.hkemtrentainment.cinthiasquestr.ui.gameboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hkemtrentainment.cinthiasquestr.R
import java.util.UUID
import kotlin.math.sin

enum class GamePieceType(val drawableResId: Int, val particleColorHex: Long) {
    RED(R.drawable.gem_red, 0xFFFF5252),
    BLUE(R.drawable.gem_blue, 0xFF40C4FF),
    GREEN(R.drawable.gem_green, 0xFF69F0AE),
    YELLOW(R.drawable.gem_yellow, 0xFFFFD740),
    PURPLE(R.drawable.gem_purple, 0xFFE040FB),
    MAGENTA(R.drawable.gem_magenta, 0xFFFF4081)
}

enum class GamePiecePowerUp {
    NONE,
    LINE_BLASTER,
    EXPLOSIVE_BOMB,
    COLOR_BOMB
}

enum class BoosterType(val displayName: String, val priceCoins: Int) {
    HAMMER("Martillo", 100),
    DYNAMITE("Dinamita", 150),
    SHUFFLE("Barajar", 120)
}

enum class CinthiaState {
    IDLE,
    CHEERING,
    WORRIED,
    SAD
}

data class GamePiece(
    val type: GamePieceType,
    val powerUp: GamePiecePowerUp = GamePiecePowerUp.NONE,
    val id: String = UUID.randomUUID().toString(),
    val isEmpty: Boolean = false
)

@Composable
fun GamePieceItem(
    piece: GamePiece,
    isMatched: Boolean,
    isSelected: Boolean = false,
    isHinted: Boolean = false,
    isInvalidShake: Boolean = false,
    isHitFlash: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (piece.isEmpty) return

    // Squash & Stretch de entrada cuando se genera una nueva ficha
    val spawnSquash = remember(piece.id) { Animatable(0.72f) }
    LaunchedEffect(piece.id) {
        spawnSquash.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    // Escala al hacer match (se infla ligeramente en el Hit-Flash y luego colapsa con rebote)
    val matchScale by animateFloatAsState(
        targetValue = when {
            isHitFlash -> 1.24f
            isMatched -> 0f
            isSelected -> 1.12f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "matchScale"
    )

    // Sacudida rápida horizontal cuando un intercambio es inválido
    val invalidShakeProgress = remember { Animatable(0f) }
    LaunchedEffect(isInvalidShake) {
        if (isInvalidShake) {
            invalidShakeProgress.snapTo(0f)
            invalidShakeProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 240, easing = LinearEasing)
            )
            invalidShakeProgress.snapTo(0f)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pieceJuiceTransition")

    // Pulso de Squash & Stretch para ficha seleccionada o sugerida (Idle Hint)
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsePhase"
    )

    val squashScaleX = when {
        isSelected -> 1f + 0.08f * pulsePhase
        isHinted -> 1f + 0.07f * pulsePhase
        else -> 1f + (1f - spawnSquash.value) * 0.35f
    }

    val squashScaleY = when {
        isSelected -> 1f + 0.08f * (1f - pulsePhase)
        isHinted -> 1f + 0.07f * (1f - pulsePhase)
        else -> spawnSquash.value
    }

    val hintOffsetY = if (isHinted) (-5f * pulsePhase) else 0f
    val shakeOffsetX = if (isInvalidShake) {
        sin(invalidShakeProgress.value * Math.PI.toFloat() * 4f) * 14f
    } else {
        0f
    }

    // Matriz de brillo blanco durante el Hit-Stop / Hit-Flash
    val hitFlashColorFilter = remember(isHitFlash, isHinted, pulsePhase) {
        if (isHitFlash) {
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        1.8f, 0f, 0f, 0f, 90f,
                        0f, 1.8f, 0f, 0f, 90f,
                        0f, 0f, 1.8f, 0f, 90f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        } else if (isHinted && pulsePhase > 0.5f) {
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        1.25f, 0f, 0f, 0f, 25f,
                        0f, 1.25f, 0f, 0f, 25f,
                        0f, 0f, 1.25f, 0f, 25f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        } else {
            null
        }
    }

    val isColorBomb = piece.powerUp == GamePiecePowerUp.COLOR_BOMB
    val hasPowerUp = piece.powerUp != GamePiecePowerUp.NONE

    Box(
        modifier = modifier
            .padding(2.dp)
            .graphicsLayer {
                scaleX = matchScale * squashScaleX
                scaleY = matchScale * squashScaleY
                translationX = shakeOffsetX
                translationY = hintOffsetY
                rotationZ = if (isSelected) (pulsePhase - 0.5f) * 8f else 0f
            }
            .then(
                when {
                    isSelected -> Modifier.border(2.5.dp, Color(0xFFFFD700), RoundedCornerShape(10.dp))
                    isHinted -> Modifier.border(
                        width = 2.dp,
                        color = Color(0xFF80D8FF).copy(alpha = 0.5f + 0.5f * pulsePhase),
                        shape = RoundedCornerShape(10.dp)
                    )
                    else -> Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isColorBomb) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFFE53935),
                                Color(0xFFFDD835),
                                Color(0xFF43A047),
                                Color(0xFF1E88E5),
                                Color(0xFF8E24AA),
                                Color(0xFFE53935)
                            )
                        )
                    )
                    .border(2.dp, Color(0xFFFFD700), CircleShape)
            )
        } else {
            if (hasPowerUp || isSelected || isHinted) {
                PowerUpGlow(
                    glowColor = when {
                        isSelected -> Color(0xFFFFEA00)
                        isHinted -> Color(0xFF80D8FF)
                        else -> Color(0xFFFFD700)
                    }
                )
            }
            Image(
                painter = painterResource(id = piece.type.drawableResId),
                contentDescription = piece.type.name,
                contentScale = ContentScale.Fit,
                colorFilter = hitFlashColorFilter,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (hasPowerUp) {
            val icon = when (piece.powerUp) {
                GamePiecePowerUp.LINE_BLASTER -> Icons.Default.FlashOn
                GamePiecePowerUp.EXPLOSIVE_BOMB -> Icons.Default.Star
                GamePiecePowerUp.COLOR_BOMB -> Icons.Default.AutoAwesome
                else -> null
            }
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = piece.powerUp.name,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/** Resplandor circular y pulsante detrás de las gemas con power-up, seleccionadas o sugeridas. */
@Composable
private fun PowerUpGlow(glowColor: Color = Color(0xFFFFD700)) {
    val transition = rememberInfiniteTransition(label = "powerUpGlow")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val radius = size.minDimension / 1.85f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = glowAlpha), Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius
                )
            }
    )
}
