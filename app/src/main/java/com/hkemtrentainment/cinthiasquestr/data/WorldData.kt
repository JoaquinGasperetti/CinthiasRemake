package com.hkemtrentainment.cinthiasquestr.data

import androidx.compose.ui.graphics.Color

data class LevelConfig(
    val levelNumber: Int,
    val worldId: Int,
    val targetScore: Int,
    val moves: Int
)

data class WorldConfig(
    val id: Int,
    val name: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val gradientColors: List<Color>,
    val startLevel: Int,
    val endLevel: Int
)

object WorldRepository {
    val WORLDS = listOf(
        WorldConfig(
            id = 1,
            name = "Bosque Encantado",
            description = "Un denso bosque mágico lleno de misterios y gemas verdes.",
            primaryColor = Color(0xFF2E7D32),
            secondaryColor = Color(0xFF81C784),
            gradientColors = listOf(Color(0xFF1B5E20), Color(0xFF388E3C), Color(0xFF2E7D32)),
            startLevel = 1,
            endLevel = 5
        ),
        WorldConfig(
            id = 2,
            name = "Desierto Dorado",
            description = "Ruinas antiguas entre dunas brillantes y tesoros perdidos.",
            primaryColor = Color(0xFFE65100),
            secondaryColor = Color(0xFFFFB74D),
            gradientColors = listOf(Color(0xFFBF360C), Color(0xFFE65100), Color(0xFFF57C00)),
            startLevel = 6,
            endLevel = 10
        ),
        WorldConfig(
            id = 3,
            name = "Cueva de Cristal",
            description = "Cavernas subterráneas heladas iluminadas por cristales cósmicos.",
            primaryColor = Color(0xFF0277BD),
            secondaryColor = Color(0xFF4FC3F7),
            gradientColors = listOf(Color(0xFF01579B), Color(0xFF0277BD), Color(0xFF0288D1)),
            startLevel = 11,
            endLevel = 15
        ),
        WorldConfig(
            id = 4,
            name = "Reino de Caramelos",
            description = "Palacios dulces y montañas de golosinas encantadas.",
            primaryColor = Color(0xFFC2185B),
            secondaryColor = Color(0xFFF48FB1),
            gradientColors = listOf(Color(0xFF880E4F), Color(0xFFC2185B), Color(0xFFD81B60)),
            startLevel = 16,
            endLevel = 20
        ),
        WorldConfig(
            id = 5,
            name = "Picos Volcánicos",
            description = "Tierras de fuego y lava ardiente que custodian la gema suprema.",
            primaryColor = Color(0xFFB71C1C),
            secondaryColor = Color(0xFFEF5350),
            gradientColors = listOf(Color(0xFF4A148C), Color(0xFFB71C1C), Color(0xFFD32F2F)),
            startLevel = 21,
            endLevel = 25
        )
    )

    fun getWorldForLevel(level: Int): WorldConfig {
        val safeLevel = level.coerceIn(1, MAX_GAME_LEVELS)
        return WORLDS.find { safeLevel in it.startLevel..it.endLevel }
            ?: WORLDS.last()
    }

    fun getLevelConfig(level: Int): LevelConfig {
        val safeLevel = level.coerceIn(1, MAX_GAME_LEVELS)
        val world = getWorldForLevel(safeLevel)
        val baseTarget = 1200 + (safeLevel * 400)
        val baseMoves = maxOf(12, 25 - (safeLevel / 2))
        return LevelConfig(
            levelNumber = safeLevel,
            worldId = world.id,
            targetScore = baseTarget,
            moves = baseMoves
        )
    }

    const val MAX_GAME_LEVELS = 25
}
