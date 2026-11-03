package com.hkemtrentainment.cinthiasquestr.ui.gameboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import java.util.UUID

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.hkemtrentainment.cinthiasquestr.R

enum class GamePieceType(val drawableResId: Int) {
    RED(R.drawable.gem_red),
    BLUE(R.drawable.gem_blue),
    GREEN(R.drawable.gem_green),
    YELLOW(R.drawable.gem_yellow),
    PURPLE(R.drawable.gem_purple),
    MAGENTA(R.drawable.gem_magenta)
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
    modifier: Modifier = Modifier
) {
    if (piece.isEmpty) return

    val scale by animateFloatAsState(
        targetValue = if (isMatched) 0f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val isColorBomb = piece.powerUp == GamePiecePowerUp.COLOR_BOMB
    val hasPowerUp = piece.powerUp != GamePiecePowerUp.NONE

    Box(
        modifier = modifier
            .padding(2.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        if (isColorBomb) {
            // TODO: reemplazar por sprite propio de la Bomba de color cuando exista el arte.
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
            if (hasPowerUp) {
                PowerUpGlow()
            }
            Image(
                painter = painterResource(id = piece.type.drawableResId),
                contentDescription = piece.type.name,
                contentScale = ContentScale.Fit,
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

/** Resplandor dorado circular y pulsante detrás de las gemas con power-up (no rectangular). */
@Composable
private fun PowerUpGlow() {
    val transition = rememberInfiniteTransition(label = "powerUpGlow")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val radius = size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFD700).copy(alpha = glowAlpha), Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius
                )
            }
    )
}
