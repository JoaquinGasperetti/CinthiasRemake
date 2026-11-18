package com.hkemtrentainment.cinthiasquestr.data

import androidx.compose.ui.graphics.Color
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.GamePiecePowerUp
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.GamePieceType

/**
 * Define las distintas siluetas y geometrías de tablero (8x8) para dar variedad visual y táctica
 * a los niveles de Cinthia's Quest. Cada máscara usa '#' para casillas jugables y '.' para huecos.
 */
enum class BoardShape(
    val displayName: String,
    val subtitle: String,
    val mask: List<String>
) {
    CLASSIC_SQUARE(
        displayName = "Cuadrado Real",
        subtitle = "Tablero clásico completo de 64 casillas",
        mask = listOf(
            "########",
            "########",
            "########",
            "########",
            "########",
            "########",
            "########",
            "########"
        )
    ),
    OCTAGON_CITADEL(
        displayName = "Octágono Real",
        subtitle = "Esquinas biseladas en diagonal",
        mask = listOf(
            "..####..",
            ".######.",
            "########",
            "########",
            "########",
            "########",
            ".######.",
            "..####.."
        )
    ),
    ROYAL_CROSS(
        displayName = "Cruz Templaria",
        subtitle = "Brazos tácticos con esquinas recortadas",
        mask = listOf(
            "..####..",
            "..####..",
            "########",
            "########",
            "########",
            "########",
            "..####..",
            "..####.."
        )
    ),
    DIAMOND_GEM(
        displayName = "Diamante Arcano",
        subtitle = "Silueta de gema escalonada a 45°",
        mask = listOf(
            "...##...",
            "..####..",
            ".######.",
            "########",
            "########",
            ".######.",
            "..####..",
            "...##..."
        )
    ),
    HEART_CHARM(
        displayName = "Corazón del Bosque",
        subtitle = "Doble arco superior y punta inferior",
        mask = listOf(
            ".##..##.",
            "########",
            "########",
            "########",
            "########",
            ".######.",
            "..####..",
            "...##..."
        )
    ),
    HOURGLASS_TIME(
        displayName = "Reloj de Arena",
        subtitle = "Cuello estrecho en el centro del tablero",
        mask = listOf(
            "########",
            "########",
            ".######.",
            "..####..",
            "..####..",
            ".######.",
            "########",
            "########"
        )
    ),
    DONUT_RING(
        displayName = "Anillo del Dragón",
        subtitle = "Foso abisal de 2×2 en el corazón del tablero",
        mask = listOf(
            ".######.",
            "########",
            "########",
            "###..###",
            "###..###",
            "########",
            "########",
            ".######."
        )
    ),
    TWIN_BRIDGES(
        displayName = "Puentes Gemelos",
        subtitle = "Dos pasos laterales sobre el abismo central",
        mask = listOf(
            "########",
            "########",
            "###..###",
            "###..###",
            "###..###",
            "###..###",
            "########",
            "########"
        )
    ),
    CASTLE_FORTRESS(
        displayName = "Fortaleza Real",
        subtitle = "Almenas de piedra y bastión escalonado",
        mask = listOf(
            "##.##.##",
            "########",
            "########",
            "########",
            "########",
            "########",
            ".######.",
            "..####.."
        )
    ),
    BUTTERFLY_WINGS(
        displayName = "Alas de Hada",
        subtitle = "Dos alas simétricas unidas por un puente",
        mask = listOf(
            "###..###",
            "###..###",
            "########",
            "########",
            "########",
            "########",
            "###..###",
            "###..###"
        )
    ),
    CHALICE_GRAIL(
        displayName = "Cáliz Dorado",
        subtitle = "Copa ancha superior y pedestal real",
        mask = listOf(
            "########",
            "########",
            ".######.",
            "..####..",
            "..####..",
            "...##...",
            ".######.",
            "########"
        )
    ),
    STEPPED_PYRAMID(
        displayName = "Pirámide del Sol",
        subtitle = "Terrazas escalonadas desde la cúspide",
        mask = listOf(
            "...##...",
            "..####..",
            "..####..",
            ".######.",
            ".######.",
            "########",
            "########",
            "########"
        )
    ),
    EMERALD_SHIELD(
        displayName = "Escudo de Cinthia",
        subtitle = "Emblema defensivo con punta heráldica",
        mask = listOf(
            ".######.",
            "########",
            "########",
            "########",
            "########",
            ".######.",
            "..####..",
            "...##..."
        )
    ),
    ARCHWAY_PORTAL(
        displayName = "Arco del Portal",
        subtitle = "Bóveda superior sostenida por dos columnas",
        mask = listOf(
            ".######.",
            "########",
            "########",
            "########",
            "###..###",
            "###..###",
            "###..###",
            "###..###"
        )
    ),
    STAR_COMPASS(
        displayName = "Estrella del Norte",
        subtitle = "Puntas cardinales con núcleo expandido",
        mask = listOf(
            "...##...",
            ".######.",
            ".######.",
            "########",
            "########",
            ".######.",
            ".######.",
            "...##..."
        )
    ),
    CROWN_OF_FIRE(
        displayName = "Corona Suprema",
        subtitle = "Tres picos reales sobre el trono volcánico",
        mask = listOf(
            "##.##.##",
            "##.##.##",
            "########",
            "########",
            "########",
            ".######.",
            "########",
            ".######."
        )
    );

    fun isCellActive(row: Int, col: Int): Boolean {
        if (row !in 0 until 8 || col !in 0 until 8) return false
        return mask[row][col] == '#'
    }

    val activeCellCount: Int
        get() = mask.sumOf { rowStr -> rowStr.count { it == '#' } }
}

data class PrePlacedRelic(
    val row: Int,
    val col: Int,
    val powerUp: GamePiecePowerUp
)

data class LevelConfig(
    val levelNumber: Int,
    val worldId: Int,
    val levelTitle: String,
    val boardShape: BoardShape,
    val targetScore: Int,
    val moves: Int,
    val activePieceTypes: List<GamePieceType> = GamePieceType.entries,
    val prePlacedRelics: List<PrePlacedRelic> = emptyList()
) {
    fun isCellActive(row: Int, col: Int): Boolean = boardShape.isCellActive(row, col)
}

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

    // Paleta de 5 colores solo para la introducción (Niveles 1 y 2), evitando cascadas infinitas de 4 colores
    private val FIVE_COLOR_PALETTE = listOf(
        GamePieceType.RED,
        GamePieceType.BLUE,
        GamePieceType.GREEN,
        GamePieceType.YELLOW,
        GamePieceType.PURPLE
    )

    // Paleta completa de 6 colores desde el Nivel 3 en adelante para exigir planificación táctica real
    private val ALL_SIX_COLORS = GamePieceType.entries

    private data class LevelBlueprint(
        val title: String,
        val shape: BoardShape,
        val targetScore: Int,
        val moves: Int,
        val colors: List<GamePieceType>,
        val prePlacedRelics: List<PrePlacedRelic> = emptyList()
    )

    /**
     * Curva de dificultad táctica y exigente:
     * - Límites de movimientos ajustados (de 17 en el tutorial a 12 en el jefe final).
     * - Metas de puntos exigentes (2,200 a 9,500) calibradas por percentil para cada silueta de tablero.
     * - 6 colores activos a partir del Nivel 3 para reducir cascadas fortuitas.
     * - Sin reliquias regaladas en niveles normales/difíciles ni bombas de color precolocadas.
     */
    private val LEVEL_BLUEPRINTS: Map<Int, LevelBlueprint> = mapOf(
        // Mundo 1: Bosque Encantado (Niveles 1-5)
        1 to LevelBlueprint(
            title = "Claro del Bosque",
            shape = BoardShape.CLASSIC_SQUARE,
            targetScore = 2200,
            moves = 17,
            colors = FIVE_COLOR_PALETTE,
            prePlacedRelics = listOf(PrePlacedRelic(3, 3, GamePiecePowerUp.LINE_BLASTER))
        ),
        2 to LevelBlueprint(
            title = "Cruce de los Druidas",
            shape = BoardShape.ROYAL_CROSS,
            targetScore = 2550,
            moves = 16,
            colors = FIVE_COLOR_PALETTE
        ),
        3 to LevelBlueprint(
            title = "Escudo de Roble",
            shape = BoardShape.EMERALD_SHIELD,
            targetScore = 2900,
            moves = 15,
            colors = ALL_SIX_COLORS
        ),
        4 to LevelBlueprint(
            title = "Mariposa Silvestre",
            shape = BoardShape.BUTTERFLY_WINGS,
            targetScore = 3150,
            moves = 16,
            colors = ALL_SIX_COLORS
        ),
        5 to LevelBlueprint(
            title = "Corazón del Guardián",
            shape = BoardShape.HEART_CHARM,
            targetScore = 3600,
            moves = 15,
            colors = ALL_SIX_COLORS,
            prePlacedRelics = listOf(PrePlacedRelic(2, 3, GamePiecePowerUp.EXPLOSIVE_BOMB))
        ),

        // Mundo 2: Desierto Dorado (Niveles 6-10)
        6 to LevelBlueprint(
            title = "Peldaños de Ra",
            shape = BoardShape.STEPPED_PYRAMID,
            targetScore = 3900,
            moves = 15,
            colors = ALL_SIX_COLORS
        ),
        7 to LevelBlueprint(
            title = "Arenas del Tiempo",
            shape = BoardShape.HOURGLASS_TIME,
            targetScore = 4150,
            moves = 15,
            colors = ALL_SIX_COLORS
        ),
        8 to LevelBlueprint(
            title = "Oasis Octogonal",
            shape = BoardShape.OCTAGON_CITADEL,
            targetScore = 4450,
            moves = 15,
            colors = ALL_SIX_COLORS
        ),
        9 to LevelBlueprint(
            title = "Cáliz del Faraón",
            shape = BoardShape.CHALICE_GRAIL,
            targetScore = 4750,
            moves = 14,
            colors = ALL_SIX_COLORS
        ),
        10 to LevelBlueprint(
            title = "Joya de las Dunas",
            shape = BoardShape.DIAMOND_GEM,
            targetScore = 5100,
            moves = 14,
            colors = ALL_SIX_COLORS,
            prePlacedRelics = listOf(PrePlacedRelic(3, 3, GamePiecePowerUp.LINE_BLASTER))
        ),

        // Mundo 3: Cueva de Cristal (Niveles 11-15)
        11 to LevelBlueprint(
            title = "Portal Subterráneo",
            shape = BoardShape.ARCHWAY_PORTAL,
            targetScore = 5350,
            moves = 15,
            colors = ALL_SIX_COLORS
        ),
        12 to LevelBlueprint(
            title = "Estrella Polar",
            shape = BoardShape.STAR_COMPASS,
            targetScore = 5650,
            moves = 14,
            colors = ALL_SIX_COLORS
        ),
        13 to LevelBlueprint(
            title = "Puentes de Escarcha",
            shape = BoardShape.TWIN_BRIDGES,
            targetScore = 5900,
            moves = 14,
            colors = ALL_SIX_COLORS
        ),
        14 to LevelBlueprint(
            title = "Anillo de Escarcha",
            shape = BoardShape.DONUT_RING,
            targetScore = 6200,
            moves = 14,
            colors = ALL_SIX_COLORS
        ),
        15 to LevelBlueprint(
            title = "Bastión del Glaciar",
            shape = BoardShape.CASTLE_FORTRESS,
            targetScore = 6600,
            moves = 13,
            colors = ALL_SIX_COLORS,
            prePlacedRelics = listOf(PrePlacedRelic(2, 3, GamePiecePowerUp.EXPLOSIVE_BOMB))
        ),

        // Mundo 4: Reino de Caramelos (Niveles 16-20)
        16 to LevelBlueprint(
            title = "Corazón de Frambuesa",
            shape = BoardShape.HEART_CHARM,
            targetScore = 6850,
            moves = 14,
            colors = ALL_SIX_COLORS
        ),
        17 to LevelBlueprint(
            title = "Cruz de Caramelo",
            shape = BoardShape.ROYAL_CROSS,
            targetScore = 7100,
            moves = 14,
            colors = ALL_SIX_COLORS
        ),
        18 to LevelBlueprint(
            title = "Alas de Azúcar",
            shape = BoardShape.BUTTERFLY_WINGS,
            targetScore = 7400,
            moves = 13,
            colors = ALL_SIX_COLORS
        ),
        19 to LevelBlueprint(
            title = "Copa de Néctar",
            shape = BoardShape.CHALICE_GRAIL,
            targetScore = 7650,
            moves = 13,
            colors = ALL_SIX_COLORS
        ),
        20 to LevelBlueprint(
            title = "Corona de Mazapán",
            shape = BoardShape.CROWN_OF_FIRE,
            targetScore = 8000,
            moves = 13,
            colors = ALL_SIX_COLORS,
            prePlacedRelics = listOf(PrePlacedRelic(3, 3, GamePiecePowerUp.LINE_BLASTER))
        ),

        // Mundo 5: Picos Volcánicos (Niveles 21-25)
        21 to LevelBlueprint(
            title = "Puentes de Magma",
            shape = BoardShape.TWIN_BRIDGES,
            targetScore = 8250,
            moves = 13,
            colors = ALL_SIX_COLORS
        ),
        22 to LevelBlueprint(
            title = "Cráter del Dragón",
            shape = BoardShape.DONUT_RING,
            targetScore = 8500,
            moves = 13,
            colors = ALL_SIX_COLORS
        ),
        23 to LevelBlueprint(
            title = "Reloj de Obsidiana",
            shape = BoardShape.HOURGLASS_TIME,
            targetScore = 8750,
            moves = 13,
            colors = ALL_SIX_COLORS
        ),
        24 to LevelBlueprint(
            title = "Diamante Ígneo",
            shape = BoardShape.DIAMOND_GEM,
            targetScore = 9050,
            moves = 12,
            colors = ALL_SIX_COLORS
        ),
        25 to LevelBlueprint(
            title = "Trono de la Corona Suprema",
            shape = BoardShape.CROWN_OF_FIRE,
            targetScore = 9500,
            moves = 12,
            colors = ALL_SIX_COLORS,
            prePlacedRelics = listOf(PrePlacedRelic(3, 3, GamePiecePowerUp.EXPLOSIVE_BOMB))
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
        val blueprint = LEVEL_BLUEPRINTS[safeLevel] ?: LevelBlueprint(
            title = "Batalla Real $safeLevel",
            shape = BoardShape.entries[safeLevel % BoardShape.entries.size],
            targetScore = 2000 + safeLevel * 300,
            moves = 14,
            colors = ALL_SIX_COLORS
        )
        return LevelConfig(
            levelNumber = safeLevel,
            worldId = world.id,
            levelTitle = blueprint.title,
            boardShape = blueprint.shape,
            targetScore = blueprint.targetScore,
            moves = blueprint.moves,
            activePieceTypes = blueprint.colors,
            prePlacedRelics = blueprint.prePlacedRelics
        )
    }

    const val MAX_GAME_LEVELS = 25
}
