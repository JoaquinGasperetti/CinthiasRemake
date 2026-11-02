package com.hkemtrentainment.cinthiasquestr.ui.gameboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.UUID

enum class GamePieceType(val color: Color) {
    RED(Color(0xFFE53935)),
    BLUE(Color(0xFF1E88E5)),
    GREEN(Color(0xFF43A047)),
    YELLOW(Color(0xFFFDD835)),
    PURPLE(Color(0xFF8E24AA)),
    ORANGE(Color(0xFFFB8C00))
}

enum class GamePiecePowerUp {
    NONE,
    LINE_BLASTER,
    EXPLOSIVE_BOMB,
    COLOR_BOMB
}

enum class BoosterType {
    HAMMER,
    DYNAMITE,
    SHUFFLE
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
    val id: String = UUID.randomUUID().toString()
)

@Composable
fun GamePieceItem(
    piece: GamePiece,
    isMatched: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isMatched) 0f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Box(
        modifier = modifier
            .padding(2.dp)
            .scale(scale)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .background(piece.type.color, RoundedCornerShape(8.dp))
            .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (piece.powerUp != GamePiecePowerUp.NONE) {
            val icon = when (piece.powerUp) {
                GamePiecePowerUp.LINE_BLASTER -> Icons.Default.FlashOn
                GamePiecePowerUp.EXPLOSIVE_BOMB -> Icons.Default.Star
                GamePiecePowerUp.COLOR_BOMB -> Icons.Default.Favorite
                else -> null
            }
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
