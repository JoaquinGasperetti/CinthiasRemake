package com.hkentertainment.cinthiasquestr.ui.gameboard

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class CinthiaState {
    IDLE, CHEERING, WORRIED, SAD
}

enum class GamePieceType(val color: Color) {
    Type1(Color(0xFFE91E63)), // Pink
    Type2(Color(0xFF2196F3)), // Blue
    Type3(Color(0xFF4CAF50)), // Green
    Type4(Color(0xFFFFEB3B)), // Yellow
    Type5(Color(0xFFFF9800)), // Orange
    Type6(Color(0xFF9C27B0))  // Purple
}

enum class GamePiecePowerUp {
    NONE,
    LINE_BLASTER,
    COLOR_BOMB,
    EXPLOSIVE_BOMB
}

enum class BoosterType {
    HAMMER, DYNAMITE, SHUFFLE
}

data class GamePiece(
    val id: String = UUID.randomUUID().toString(),
    val type: GamePieceType,
    val powerUp: GamePiecePowerUp = GamePiecePowerUp.NONE
)
