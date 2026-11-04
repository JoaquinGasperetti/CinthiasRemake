package com.hkemtrentainment.cinthiasquestr.ui.gameboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hkemtrentainment.cinthiasquestr.audio.GameSound
import com.hkemtrentainment.cinthiasquestr.audio.SoundManager
import com.hkemtrentainment.cinthiasquestr.data.PlayerRepository
import com.hkemtrentainment.cinthiasquestr.data.WorldRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.abs

enum class BoardState {
    IDLE, SWAPPING, RESOLVING, DROPPING, SHUFFLING, WAITING_FOR_BOOSTER_TARGET, BONUS_FINALE
}

/**
 * Texto flotante de puntuación y elogios ("+120", "¡INCREÍBLE! x3") que emerge sobre el tablero.
 */
data class FloatingScorePopup(
    val id: String = UUID.randomUUID().toString(),
    val row: Float,
    val col: Float,
    val pointsText: String,
    val calloutText: String? = null,
    val colorHex: Long = 0xFFFFD700,
    val scaleMultiplier: Float = 1.0f
)

/**
 * Evento de explosión de partículas, rayos de Line Blaster y ondas expansivas en el Canvas del tablero.
 */
data class ParticleBurstEvent(
    val id: String = UUID.randomUUID().toString(),
    val cellColors: Map<Pair<Int, Int>, Long>,
    val shockwaveCenters: List<Pair<Int, Int>> = emptyList(),
    val lineBlasterRows: Set<Int> = emptySet(),
    val lineBlasterCols: Set<Int> = emptySet(),
    val isColorBombMega: Boolean = false
)

data class GameUIState(
    val grid: List<List<GamePiece>> = emptyList(),
    val score: Int = 0,
    val movesLeft: Int = 20,
    val boardState: BoardState = BoardState.IDLE,
    val isGameOver: Boolean = false,
    val isGameWon: Boolean = false,
    val targetScore: Int = 2000,
    val cinthiaState: CinthiaState = CinthiaState.IDLE,
    val cinthiaSpeech: String? = null,
    val lastMatchCombo: Int = 0,
    val matchParticles: Set<Pair<Int, Int>> = emptySet(),
    val hitFlashCells: Set<Pair<Int, Int>> = emptySet(),
    val selectedCell: Pair<Int, Int>? = null,
    val hintCells: Set<Pair<Int, Int>> = emptySet(),
    val invalidSwapCells: Set<Pair<Int, Int>> = emptySet(),
    val floatingPopups: List<FloatingScorePopup> = emptyList(),
    val boosterInventory: Map<BoosterType, Int> = emptyMap(),
    val activeBooster: BoosterType? = null,
    val coins: Int = 0,
    val level: Int = 1,
    val earnedStars: Int = 0,
    val earnedCoinsReward: Int = 0,
    val bonusMovesScore: Int = 0,
    val doubleCoinsClaimed: Boolean = false,
    val winStreak: Int = 0,
    val streakStarterCount: Int = 0,
    val streakBannerText: String? = null,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reducedMotion: Boolean = false
) {
    val progressToFirstStar: Float
        get() = if (targetScore > 0) (score.toFloat() / targetScore).coerceIn(0f, 1f) else 0f

    val progressToThreeStars: Float
        get() {
            val maxStarScore = (targetScore * 1.8f).coerceAtLeast(1f)
            return (score.toFloat() / maxStarScore).coerceIn(0f, 1f)
        }

    val currentLiveStars: Int
        get() = when {
            score >= (targetScore * 1.8).toInt() -> 3
            score >= (targetScore * 1.4).toInt() -> 2
            score >= targetScore -> 1
            else -> 0
        }

    val pointsNeededToWin: Int
        get() = maxOf(0, targetScore - score)

    val nearMissPercent: Int
        get() = if (targetScore > 0) ((score * 100) / targetScore).coerceIn(0, 99) else 0
}

sealed class GameEffect {
    data object SwipeHaptic : GameEffect()
    data object MatchHaptic : GameEffect()
    data object InvalidMoveHaptic : GameEffect()
    data class ComboHaptic(val intensity: Int = 2) : GameEffect()
    data class ScreenShake(val intensityPx: Float = 10f, val cycles: Int = 5) : GameEffect()
    data class SpawnParticles(val event: ParticleBurstEvent) : GameEffect()
    data class PlaySound(
        val sound: GameSound,
        val delayMs: Long = 0L,
        val rate: Float = 1.0f,
        val volume: Float = 1.0f
    ) : GameEffect()
}

enum class SwipeDirection {
    UP, DOWN, LEFT, RIGHT
}

class GameViewModel(
    private val playerRepository: PlayerRepository,
    val selectedLevel: Int = 1
) : ViewModel() {
    private val safeLevel = selectedLevel.coerceIn(1, WorldRepository.MAX_GAME_LEVELS)

    private val _uiState = MutableStateFlow(GameUIState())
    val uiState: StateFlow<GameUIState> = _uiState.asStateFlow()

    val gameState: StateFlow<GameUIState> = uiState

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 48)
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

    private val logicalGrid = MutableList(8) { MutableList(8) { GamePiece(type = randomType()) } }
    private var idleHintJob: Job? = null
    private var hasAppliedStreakBonusForSession = false
    private var streakAtRiskOnLoss: Int = 0

    init {
        val levelConfig = WorldRepository.getLevelConfig(safeLevel)
        _uiState.update {
            it.copy(
                level = safeLevel,
                targetScore = levelConfig.targetScore,
                movesLeft = levelConfig.moves
            )
        }

        initializeBoard()

        viewModelScope.launch {
            playerRepository.playerDataFlow.collect { playerData ->
                _uiState.update {
                    it.copy(
                        boosterInventory = mapOf(
                            BoosterType.HAMMER to playerData.hammer,
                            BoosterType.DYNAMITE to playerData.dynamite,
                            BoosterType.SHUFFLE to playerData.shuffle
                        ),
                        coins = playerData.coins,
                        winStreak = if (it.isGameOver && !it.isGameWon && streakAtRiskOnLoss > 0) {
                            streakAtRiskOnLoss
                        } else {
                            playerData.winStreak
                        },
                        soundEnabled = playerData.soundEnabled,
                        hapticsEnabled = playerData.hapticsEnabled,
                        reducedMotion = playerData.reducedMotion
                    )
                }

                if (!hasAppliedStreakBonusForSession) {
                    hasAppliedStreakBonusForSession = true
                    if (playerData.winStreak > 0 && _uiState.value.boardState == BoardState.IDLE && _uiState.value.score == 0) {
                        applyStreakStarterPowerUps(playerData.winStreak)
                    }
                }
            }
        }

        scheduleIdleHintTimer()
    }

    private fun initializeBoard() {
        var attempts = 0
        do {
            attempts++
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    logicalGrid[r][c] = GamePiece(type = randomType())
                }
            }
        } while ((findMatches().isNotEmpty() || !hasPossibleMoves()) && attempts < 40)

        syncUI()
    }

    /**
     * Recompensa la Racha de Victorias (Win Streak) colocando de 1 a 3 Power-Ups gratis en el tablero
     * al iniciar la partida, reforzando la retención mediante aversión a la pérdida (Loss Aversion).
     */
    private fun applyStreakStarterPowerUps(winStreak: Int) {
        val count = PlayerRepository.computeStreakStarterPowerUpsCount(winStreak)
        if (count <= 0) return

        val candidateCoords = mutableListOf<Pair<Int, Int>>()
        for (r in 1..6) {
            for (c in 1..6) {
                if (logicalGrid[r][c].powerUp == GamePiecePowerUp.NONE) {
                    candidateCoords.add(r to c)
                }
            }
        }
        candidateCoords.shuffle()

        val powerUpProgression = listOf(
            GamePiecePowerUp.LINE_BLASTER,
            GamePiecePowerUp.EXPLOSIVE_BOMB,
            GamePiecePowerUp.COLOR_BOMB
        )

        val spawnedCoords = mutableMapOf<Pair<Int, Int>, Long>()
        for (i in 0 until minOf(count, candidateCoords.size)) {
            val (r, c) = candidateCoords[i]
            val powerUpType = powerUpProgression[i % powerUpProgression.size]
            val basePiece = logicalGrid[r][c]
            logicalGrid[r][c] = basePiece.copy(
                powerUp = powerUpType,
                id = UUID.randomUUID().toString()
            )
            spawnedCoords[r to c] = 0xFFFFD700L
        }

        syncUI()

        viewModelScope.launch {
            _effects.emit(GameEffect.PlaySound(GameSound.BOMB_POWER_RISE, rate = 1.1f))
            _effects.emit(
                GameEffect.SpawnParticles(
                    ParticleBurstEvent(
                        cellColors = spawnedCoords,
                        shockwaveCenters = spawnedCoords.keys.toList()
                    )
                )
            )
            _uiState.update {
                it.copy(
                    streakStarterCount = count,
                    cinthiaState = CinthiaState.CHEERING,
                    cinthiaSpeech = "🔥 ¡Racha x$winStreak! +$count Power-Up${if (count > 1) "s" else ""}",
                    streakBannerText = "🔥 RACHA DE VICTORIAS x$winStreak · +$count POWER-UP${if (count > 1) "S" else ""} INICIAL${if (count > 1) "ES" else ""}"
                )
            }
            delay(2600)
            _uiState.update {
                it.copy(
                    streakBannerText = null,
                    cinthiaSpeech = if (it.cinthiaSpeech?.startsWith("🔥 ¡Racha") == true) null else it.cinthiaSpeech,
                    cinthiaState = if (it.isGameWon) CinthiaState.CHEERING
                    else if (it.isGameOver) CinthiaState.SAD
                    else if (it.movesLeft <= 5) CinthiaState.WORRIED
                    else CinthiaState.IDLE
                )
            }
        }
    }

    private fun scheduleIdleHintTimer() {
        idleHintJob?.cancel()
        idleHintJob = viewModelScope.launch {
            delay(IDLE_HINT_DELAY_MS)
            val state = _uiState.value
            if (state.boardState == BoardState.IDLE && !state.isGameOver && !state.isGameWon) {
                val hintPair = findBestHintMove()
                if (hintPair != null) {
                    _uiState.update {
                        it.copy(
                            hintCells = setOf(hintPair.first, hintPair.second),
                            cinthiaSpeech = "💡 ¡Prueba combinar aquí!"
                        )
                    }
                }
            }
        }
    }

    private fun clearInteractionOverlays() {
        idleHintJob?.cancel()
        if (_uiState.value.hintCells.isNotEmpty() || _uiState.value.selectedCell != null) {
            _uiState.update {
                it.copy(
                    hintCells = emptySet(),
                    selectedCell = null
                )
            }
        }
    }

    private fun randomType() = GamePieceType.entries.random()

    private fun syncUI() {
        _uiState.update { it.copy(grid = logicalGrid.map { row -> row.toList() }) }
    }

    fun onSwipe(row: Int, col: Int, direction: SwipeDirection) {
        val state = _uiState.value
        if (state.boardState != BoardState.IDLE || state.isGameOver || state.isGameWon) return
        if (row !in 0..7 || col !in 0..7) return

        val targetRow = when (direction) {
            SwipeDirection.UP -> row - 1
            SwipeDirection.DOWN -> row + 1
            else -> row
        }
        val targetCol = when (direction) {
            SwipeDirection.LEFT -> col - 1
            SwipeDirection.RIGHT -> col + 1
            else -> col
        }

        if (targetRow !in 0..7 || targetCol !in 0..7) return

        clearInteractionOverlays()
        viewModelScope.launch {
            processMove(row, col, targetRow, targetCol)
        }
    }

    private suspend fun processMove(r1: Int, c1: Int, r2: Int, c2: Int) {
        if (logicalGrid[r1][c1].isEmpty || logicalGrid[r2][c2].isEmpty) {
            _uiState.update { it.copy(boardState = BoardState.IDLE) }
            return
        }

        _uiState.update {
            it.copy(
                boardState = BoardState.SWAPPING,
                selectedCell = null,
                hintCells = emptySet()
            )
        }
        _effects.emit(GameEffect.SwipeHaptic)

        // Intercambio lógico
        val temp = logicalGrid[r1][c1]
        logicalGrid[r1][c1] = logicalGrid[r2][c2]
        logicalGrid[r2][c2] = temp
        syncUI()

        delay(210) // Tiempo ágil de animación de swap

        val movedPiece = logicalGrid[r2][c2]
        val otherPiece = logicalGrid[r1][c1]

        // 1. Verificar si es un intercambio especial de COLOR_BOMB o combinación de 2 Power-Ups
        val specialSwapCoords = evaluateSpecialSwap(r1, c1, otherPiece, r2, c2, movedPiece)
        if (specialSwapCoords.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    movesLeft = maxOf(0, it.movesLeft - 1),
                    boardState = BoardState.RESOLVING
                )
            }
            resolveMatches(
                initialMatches = listOf(specialSwapCoords),
                swapOrigin = r2 to c2,
                isSpecialSwap = true,
                preProcessedSpecialOrigins = setOf(r1 to c1, r2 to c2)
            )
            return
        }

        // 2. Verificar matches normales de 3 o más piezas
        val matches = findMatches()
        if (matches.isEmpty()) {
            // Sonido y vibración de movimiento inválido + Shake en ambas fichas
            _effects.emit(GameEffect.PlaySound(GameSound.TILE_INVALID))
            _effects.emit(GameEffect.InvalidMoveHaptic)
            _uiState.update { it.copy(invalidSwapCells = setOf(r1 to c1, r2 to c2)) }

            // Deshacer swap si no hay combinación válida
            val tempBack = logicalGrid[r1][c1]
            logicalGrid[r1][c1] = logicalGrid[r2][c2]
            logicalGrid[r2][c2] = tempBack
            syncUI()
            delay(240)
            _uiState.update {
                it.copy(
                    boardState = BoardState.IDLE,
                    invalidSwapCells = emptySet()
                )
            }
            scheduleIdleHintTimer()
        } else {
            _uiState.update {
                it.copy(
                    movesLeft = maxOf(0, it.movesLeft - 1),
                    boardState = BoardState.RESOLVING
                )
            }
            resolveMatches(matches, swapOrigin = r2 to c2)
        }
    }

    /**
     * Evalúa si el jugador intercambió un COLOR_BOMB con cualquier ficha o dos Power-Ups entre sí.
     */
    private fun evaluateSpecialSwap(
        r1: Int,
        c1: Int,
        p1: GamePiece,
        r2: Int,
        c2: Int,
        p2: GamePiece
    ): Set<Pair<Int, Int>> {
        // Doble Color Bomb: limpia todo el tablero 8x8
        if (p1.powerUp == GamePiecePowerUp.COLOR_BOMB && p2.powerUp == GamePiecePowerUp.COLOR_BOMB) {
            val allCoords = mutableSetOf<Pair<Int, Int>>()
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    allCoords.add(r to c)
                }
            }
            return allCoords
        }

        // Un Color Bomb intercambiado con cualquier pieza: destruye todas las piezas del color de la otra ficha
        if (p1.powerUp == GamePiecePowerUp.COLOR_BOMB || p2.powerUp == GamePiecePowerUp.COLOR_BOMB) {
            val other = if (p1.powerUp == GamePiecePowerUp.COLOR_BOMB) p2 else p1
            val targetType = other.type
            val affected = mutableSetOf(r1 to c1, r2 to c2)
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    val cellPiece = logicalGrid[r][c]
                    if (!cellPiece.isEmpty && cellPiece.powerUp != GamePiecePowerUp.COLOR_BOMB && cellPiece.type == targetType) {
                        affected.add(r to c)
                        // Si la otra ficha también era un Power-Up (Bomb o Line Blaster), propaga su efecto en cada gema de ese color
                        if (other.powerUp == GamePiecePowerUp.LINE_BLASTER) {
                            for (i in 0 until 8) {
                                affected.add(r to i)
                                affected.add(i to c)
                            }
                        } else if (other.powerUp == GamePiecePowerUp.EXPLOSIVE_BOMB) {
                            for (dr in -1..1) {
                                for (dc in -1..1) {
                                    if (r + dr in 0..7 && c + dc in 0..7) {
                                        affected.add((r + dr) to (c + dc))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return affected
        }

        // Dos Explosive Bombs intercambiadas: Mega Explosión de 5x5
        if (p1.powerUp == GamePiecePowerUp.EXPLOSIVE_BOMB && p2.powerUp == GamePiecePowerUp.EXPLOSIVE_BOMB) {
            val affected = mutableSetOf<Pair<Int, Int>>()
            for (dr in -2..2) {
                for (dc in -2..2) {
                    val nr = r2 + dr
                    val nc = c2 + dc
                    if (nr in 0..7 && nc in 0..7) affected.add(nr to nc)
                }
            }
            return affected
        }

        // Line Blaster + Explosive Bomb: Super Cruz de 3 filas y 3 columnas
        if ((p1.powerUp == GamePiecePowerUp.LINE_BLASTER && p2.powerUp == GamePiecePowerUp.EXPLOSIVE_BOMB) ||
            (p1.powerUp == GamePiecePowerUp.EXPLOSIVE_BOMB && p2.powerUp == GamePiecePowerUp.LINE_BLASTER)
        ) {
            val affected = mutableSetOf<Pair<Int, Int>>()
            for (d in -1..1) {
                val targetR = (r2 + d).coerceIn(0, 7)
                val targetC = (c2 + d).coerceIn(0, 7)
                for (i in 0 until 8) {
                    affected.add(targetR to i)
                    affected.add(i to targetC)
                }
            }
            return affected
        }

        // Dos Line Blasters: Cruz completa en (r1, c1) y (r2, c2)
        if (p1.powerUp != GamePiecePowerUp.NONE && p2.powerUp != GamePiecePowerUp.NONE) {
            return setOf(r1 to c1, r2 to c2)
        }

        return emptySet()
    }

    private data class ExpandedDestruction(
        val finalCoords: Set<Pair<Int, Int>>,
        val triggeredPowerUps: Set<GamePiecePowerUp>,
        val shockwaveCenters: List<Pair<Int, Int>>,
        val lineBlasterRows: Set<Int>,
        val lineBlasterCols: Set<Int>
    )

    private fun mostCommonBoardType(): GamePieceType {
        val counts = mutableMapOf<GamePieceType, Int>()
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                val p = logicalGrid[r][c]
                if (!p.isEmpty && p.powerUp != GamePiecePowerUp.COLOR_BOMB) {
                    counts[p.type] = (counts[p.type] ?: 0) + 1
                }
            }
        }
        return counts.maxByOrNull { it.value }?.key ?: randomType()
    }

    private fun expandWithPowerUps(
        seedCoords: Set<Pair<Int, Int>>,
        preProcessedCoords: Set<Pair<Int, Int>> = emptySet()
    ): ExpandedDestruction {
        val finalDestructionSet = seedCoords.toMutableSet()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.addAll(seedCoords)
        val processedPowerUps = preProcessedCoords.toMutableSet()
        val triggeredPowerUps = mutableSetOf<GamePiecePowerUp>()
        val shockwaveCenters = mutableListOf<Pair<Int, Int>>()
        val blasterRows = mutableSetOf<Int>()
        val blasterCols = mutableSetOf<Int>()

        // Registrar los power-ups que ya venían pre-procesados por un Special Swap
        for (coord in preProcessedCoords) {
            val p = logicalGrid[coord.first][coord.second]
            if (!p.isEmpty && p.powerUp != GamePiecePowerUp.NONE) {
                triggeredPowerUps.add(p.powerUp)
                shockwaveCenters.add(coord)
                if (p.powerUp == GamePiecePowerUp.LINE_BLASTER) {
                    blasterRows.add(coord.first)
                    blasterCols.add(coord.second)
                }
            }
        }

        while (queue.isNotEmpty()) {
            val coord = queue.poll() ?: continue
            val piece = logicalGrid[coord.first][coord.second]
            if (!piece.isEmpty && piece.powerUp != GamePiecePowerUp.NONE && coord !in processedPowerUps) {
                processedPowerUps.add(coord)
                triggeredPowerUps.add(piece.powerUp)
                shockwaveCenters.add(coord)

                when (piece.powerUp) {
                    GamePiecePowerUp.LINE_BLASTER -> {
                        blasterRows.add(coord.first)
                        blasterCols.add(coord.second)
                        for (i in 0 until 8) {
                            if (finalDestructionSet.add(coord.first to i)) queue.add(coord.first to i)
                            if (finalDestructionSet.add(i to coord.second)) queue.add(i to coord.second)
                        }
                    }
                    GamePiecePowerUp.EXPLOSIVE_BOMB -> {
                        for (dr in -1..1) {
                            for (dc in -1..1) {
                                val nr = coord.first + dr
                                val nc = coord.second + dc
                                if (nr in 0..7 && nc in 0..7) {
                                    if (finalDestructionSet.add(nr to nc)) queue.add(nr to nc)
                                }
                            }
                        }
                    }
                    GamePiecePowerUp.COLOR_BOMB -> {
                        val targetType = mostCommonBoardType()
                        for (r in 0 until 8) {
                            for (c in 0 until 8) {
                                val cellPiece = logicalGrid[r][c]
                                if (!cellPiece.isEmpty && cellPiece.powerUp != GamePiecePowerUp.COLOR_BOMB && cellPiece.type == targetType) {
                                    if (finalDestructionSet.add(r to c)) queue.add(r to c)
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
        return ExpandedDestruction(
            finalCoords = finalDestructionSet,
            triggeredPowerUps = triggeredPowerUps,
            shockwaveCenters = shockwaveCenters,
            lineBlasterRows = blasterRows,
            lineBlasterCols = blasterCols
        )
    }

    private suspend fun resolveMatches(
        initialMatches: List<Set<Pair<Int, Int>>>,
        swapOrigin: Pair<Int, Int>? = null,
        isSpecialSwap: Boolean = false,
        preProcessedSpecialOrigins: Set<Pair<Int, Int>> = emptySet()
    ) {
        var matches = initialMatches
        var combo = 0
        var totalMatchesThisTurn = 0
        var totalPowerUpsThisTurn = 0

        while (matches.isNotEmpty()) {
            combo++
            totalMatchesThisTurn += matches.size

            val allMatchedCoords = matches.flatten().toSet()
            val expansion = expandWithPowerUps(
                seedCoords = allMatchedCoords,
                preProcessedCoords = if (combo == 1 && isSpecialSwap) preProcessedSpecialOrigins else emptySet()
            )
            val finalDestructionSet = expansion.finalCoords
            val triggeredPowerUps = expansion.triggeredPowerUps
            val powerUpTriggered = triggeredPowerUps.isNotEmpty() || (isSpecialSwap && combo == 1)
            if (powerUpTriggered) {
                totalPowerUpsThisTurn += maxOf(1, triggeredPowerUps.size)
            }

            // Generar nuevos power-ups únicamente a partir de alineaciones normales de 4, 5 o L/T
            val powerUpsToSpawn = mutableListOf<Triple<Int, Int, GamePiecePowerUp>>()
            if (!(isSpecialSwap && combo == 1)) {
                val activeSwapOrigin = if (combo == 1) swapOrigin else null
                for (matchSet in matches) {
                    val powerUp = determinePowerUp(matchSet)
                    if (powerUp != GamePiecePowerUp.NONE) {
                        val spawnPos = if (activeSwapOrigin != null && activeSwapOrigin in matchSet) {
                            activeSwapOrigin
                        } else {
                            matchSet.first()
                        }
                        powerUpsToSpawn.add(Triple(spawnPos.first, spawnPos.second, powerUp))
                    }
                }
            }

            val hasBigMatch = powerUpsToSpawn.isNotEmpty() || matches.any { it.size >= 4 } || (isSpecialSwap && combo == 1)
            val comboPitch = SoundManager.computeComboPitch(combo)

            // HIT-STOP (Micro-congelamiento de impacto de 45-70ms con destello blanco en impactos fuertes)
            if (powerUpTriggered || hasBigMatch || combo >= 2) {
                _uiState.update {
                    it.copy(
                        boardState = BoardState.RESOLVING,
                        hitFlashCells = finalDestructionSet
                    )
                }
                delay(if (powerUpTriggered || combo >= 3) 70L else 45L)
                _uiState.update { it.copy(hitFlashCells = emptySet()) }
            }

            // 1. Sonido de combinación con Escalada de Tono (Pitch Escalation)
            when {
                combo >= 2 -> _effects.emit(GameEffect.PlaySound(GameSound.MATCH_COMBO, rate = comboPitch))
                hasBigMatch -> _effects.emit(GameEffect.PlaySound(GameSound.MATCH_SUCCESS_GEM, rate = comboPitch))
                else -> _effects.emit(GameEffect.PlaySound(GameSound.MATCH_TRIPLE, rate = comboPitch))
            }

            // 2. Sonido de explosión de Bomba / Power-Up activado
            if (GamePiecePowerUp.COLOR_BOMB in triggeredPowerUps || (isSpecialSwap && combo == 1)) {
                _effects.emit(GameEffect.PlaySound(GameSound.BOMB_EXPLODE_RED))
            } else if (triggeredPowerUps.isNotEmpty()) {
                _effects.emit(GameEffect.PlaySound(GameSound.BOMB_EXPLODE))
            }

            // 3. Sonido de creación/carga de nuevo Power-Up
            if (powerUpsToSpawn.isNotEmpty()) {
                val spawnedTypes = powerUpsToSpawn.map { it.third }.toSet()
                if (GamePiecePowerUp.COLOR_BOMB in spawnedTypes || GamePiecePowerUp.EXPLOSIVE_BOMB in spawnedTypes) {
                    _effects.emit(GameEffect.PlaySound(GameSound.BOMB_POWER_FULL, delayMs = 70L))
                } else if (GamePiecePowerUp.LINE_BLASTER in spawnedTypes) {
                    _effects.emit(GameEffect.PlaySound(GameSound.BOMB_POWER_RISE, delayMs = 70L))
                }
            }

            // 4. Voz de celebración de Cinthia y burbuja reactiva
            val calloutLabel = computeComboCallout(combo, finalDestructionSet.size, powerUpTriggered)
            if (combo == 2 || (combo >= 4 && combo % 2 == 0) || (combo == 1 && powerUpTriggered)) {
                _effects.emit(GameEffect.PlaySound(GameSound.KNIGHT_CHEER_COMBO, delayMs = 110L))
            }

            // 5. Haptics y Screen Shake escalados según la magnitud del impacto
            if (powerUpTriggered || combo > 1) {
                _effects.emit(GameEffect.ComboHaptic(intensity = minOf(4, combo + (if (powerUpTriggered) 1 else 0))))
            } else {
                _effects.emit(GameEffect.MatchHaptic)
            }

            val shakeIntensity = when {
                GamePiecePowerUp.COLOR_BOMB in triggeredPowerUps || finalDestructionSet.size >= 12 -> 18f
                powerUpTriggered || combo >= 3 -> 12f
                combo == 2 || hasBigMatch -> 8f
                else -> 4.5f
            }
            val shakeCycles = if (powerUpTriggered || combo >= 3) 6 else 4
            _effects.emit(GameEffect.ScreenShake(intensityPx = shakeIntensity, cycles = shakeCycles))

            // 6. Emitir explosión de partículas con los colores reales de cada gema y ondas de choque
            val cellColorMap = finalDestructionSet.associateWith { (r, c) ->
                val piece = logicalGrid[r][c]
                if (piece.powerUp == GamePiecePowerUp.COLOR_BOMB) 0xFFFFD700L else piece.type.particleColorHex
            }
            _effects.emit(
                GameEffect.SpawnParticles(
                    ParticleBurstEvent(
                        cellColors = cellColorMap,
                        shockwaveCenters = expansion.shockwaveCenters,
                        lineBlasterRows = expansion.lineBlasterRows,
                        lineBlasterCols = expansion.lineBlasterCols,
                        isColorBombMega = GamePiecePowerUp.COLOR_BOMB in triggeredPowerUps || (isSpecialSwap && combo == 1)
                    )
                )
            )

            // 7. Calcular puntos y lanzar Popup Flotante en el centroide del match
            val points = finalDestructionSet.size * 10 * combo
            val avgRow = finalDestructionSet.map { it.first }.average().toFloat()
            val avgCol = finalDestructionSet.map { it.second }.average().toFloat()
            val dominantColor = cellColorMap.values.firstOrNull() ?: 0xFFFFD700L

            val popup = FloatingScorePopup(
                row = avgRow.coerceIn(0.8f, 6.5f),
                col = avgCol.coerceIn(1.0f, 6.0f),
                pointsText = "+$points",
                calloutText = calloutLabel,
                colorHex = if (combo >= 2 || powerUpTriggered) 0xFFFFD700L else dominantColor,
                scaleMultiplier = (1.0f + (combo - 1) * 0.14f + (if (powerUpTriggered) 0.2f else 0f)).coerceAtMost(1.65f)
            )

            val powerUpPositions = powerUpsToSpawn.map { it.first to it.second }.toSet()
            val shrinkingParticles = finalDestructionSet - powerUpPositions

            _uiState.update { state ->
                state.copy(
                    boardState = BoardState.RESOLVING,
                    score = state.score + points,
                    lastMatchCombo = combo,
                    matchParticles = shrinkingParticles,
                    floatingPopups = (state.floatingPopups + popup).takeLast(5),
                    cinthiaState = if (combo >= 2 || powerUpTriggered) CinthiaState.CHEERING else state.cinthiaState,
                    cinthiaSpeech = calloutLabel ?: state.cinthiaSpeech
                )
            }

            viewModelScope.launch {
                delay(950)
                _uiState.update { state ->
                    state.copy(floatingPopups = state.floatingPopups.filterNot { it.id == popup.id })
                }
            }

            if (points >= 100) {
                viewModelScope.launch {
                    playerRepository.addCoins(points / 100)
                }
            }

            delay(180)
            _uiState.update { it.copy(matchParticles = emptySet()) }

            for (coord in finalDestructionSet) {
                if (coord !in powerUpPositions) {
                    logicalGrid[coord.first][coord.second] = GamePiece(
                        type = randomType(),
                        id = "EMPTY_${UUID.randomUUID()}",
                        isEmpty = true
                    )
                }
            }

            for (p in powerUpsToSpawn) {
                val basePiece = logicalGrid[p.first][p.second]
                logicalGrid[p.first][p.second] = basePiece.copy(
                    powerUp = p.third,
                    id = UUID.randomUUID().toString(),
                    isEmpty = false
                )
            }

            _uiState.update { it.copy(boardState = BoardState.DROPPING) }
            applyGravity()
            syncUI()
            // Sonido de caída / aterrizaje de gemas
            _effects.emit(GameEffect.PlaySound(GameSound.GEM_LAND, delayMs = 120L))
            delay(270)

            matches = findMatches()
        }

        // Registrar telemetría de misiones diarias
        viewModelScope.launch {
            playerRepository.recordMatchTelemetry(
                matchesMade = totalMatchesThisTurn,
                powerUpsTriggered = totalPowerUpsThisTurn
            )
        }

        if (!hasPossibleMoves()) {
            shuffleBoard()
        }

        checkGameOver()

        // Garantizar SIEMPRE que boardState regrese a IDLE al terminar la resolución,
        // incluso si el jugador perdió, para que al revivir pueda mover fichas de inmediato.
        _uiState.update { state ->
            val nextCinthiaState = when {
                state.isGameWon -> CinthiaState.CHEERING
                state.isGameOver -> CinthiaState.SAD
                state.movesLeft <= 5 -> CinthiaState.WORRIED
                else -> CinthiaState.IDLE
            }
            val nextSpeech = when {
                state.isGameWon || state.isGameOver -> state.cinthiaSpeech
                state.movesLeft == 1 -> "⚠️ ¡Último movimiento!"
                state.movesLeft in 2..3 && state.pointsNeededToWin <= 300 -> "🔥 ¡Faltan solo ${state.pointsNeededToWin} pts!"
                else -> null
            }
            state.copy(
                boardState = BoardState.IDLE,
                lastMatchCombo = 0,
                cinthiaState = nextCinthiaState,
                cinthiaSpeech = nextSpeech
            )
        }

        if (!_uiState.value.isGameOver && !_uiState.value.isGameWon) {
            scheduleIdleHintTimer()
        }
    }

    internal fun computeComboCallout(combo: Int, destroyedCount: Int, powerUpTriggered: Boolean): String? {
        return when {
            combo >= 5 || destroyedCount >= 16 -> "¡DIVINO! x$combo"
            combo == 4 || destroyedCount >= 12 -> "¡ÉPICO! x$combo"
            combo == 3 || (powerUpTriggered && destroyedCount >= 8) -> "¡INCREÍBLE!${if (combo >= 2) " x$combo" else ""}"
            combo == 2 -> "¡COMBO x2!"
            destroyedCount >= 5 -> "¡MAGNÍFICO!"
            destroyedCount == 4 -> "¡GENIAL!"
            else -> null
        }
    }

    /**
     * Verifica longitudes reales contiguas horizontales y verticales en `matchSet` para determinar
     * qué Power-Up debe generarse (evitando falsos positivos por coordenadas no contiguas).
     */
    internal fun determinePowerUp(matchSet: Set<Pair<Int, Int>>): GamePiecePowerUp {
        var maxHRun = 0
        val byRow = matchSet.groupBy { it.first }
        for ((_, cells) in byRow) {
            val sortedCols = cells.map { it.second }.sorted()
            var currentRun = 1
            for (i in 1 until sortedCols.size) {
                if (sortedCols[i] == sortedCols[i - 1] + 1) {
                    currentRun++
                    if (currentRun > maxHRun) maxHRun = currentRun
                } else {
                    currentRun = 1
                }
            }
            if (sortedCols.isNotEmpty() && maxHRun < 1) maxHRun = 1
        }

        var maxVRun = 0
        val byCol = matchSet.groupBy { it.second }
        for ((_, cells) in byCol) {
            val sortedRows = cells.map { it.first }.sorted()
            var currentRun = 1
            for (i in 1 until sortedRows.size) {
                if (sortedRows[i] == sortedRows[i - 1] + 1) {
                    currentRun++
                    if (currentRun > maxVRun) maxVRun = currentRun
                } else {
                    currentRun = 1
                }
            }
            if (sortedRows.isNotEmpty() && maxVRun < 1) maxVRun = 1
        }

        if (maxHRun >= 5 || maxVRun >= 5) return GamePiecePowerUp.COLOR_BOMB
        if (maxHRun >= 3 && maxVRun >= 3) return GamePiecePowerUp.EXPLOSIVE_BOMB
        if (maxHRun >= 4 || maxVRun >= 4) return GamePiecePowerUp.LINE_BLASTER

        return GamePiecePowerUp.NONE
    }

    private fun applyGravity() {
        for (c in 0 until 8) {
            val column = mutableListOf<GamePiece>()
            for (r in 7 downTo 0) {
                val piece = logicalGrid[r][c]
                if (!piece.isEmpty && !piece.id.startsWith("EMPTY")) {
                    column.add(piece)
                }
            }
            while (column.size < 8) {
                column.add(GamePiece(type = randomType()))
            }
            for (r in 0 until 8) {
                logicalGrid[7 - r][c] = column[r]
            }
        }
    }

    internal fun findMatches(): List<Set<Pair<Int, Int>>> {
        val horizontalMatches = mutableListOf<Set<Pair<Int, Int>>>()
        for (r in 0 until 8) {
            var c = 0
            while (c < 8) {
                val startPiece = logicalGrid[r][c]
                if (startPiece.isEmpty || startPiece.id.startsWith("EMPTY") || startPiece.powerUp == GamePiecePowerUp.COLOR_BOMB) {
                    c++
                    continue
                }
                var count = 1
                while (c + count < 8) {
                    val nextPiece = logicalGrid[r][c + count]
                    if (!nextPiece.isEmpty &&
                        !nextPiece.id.startsWith("EMPTY") &&
                        nextPiece.powerUp != GamePiecePowerUp.COLOR_BOMB &&
                        nextPiece.type == startPiece.type
                    ) {
                        count++
                    } else {
                        break
                    }
                }
                if (count >= 3) {
                    horizontalMatches.add((0 until count).map { r to (c + it) }.toSet())
                    c += count
                } else {
                    c++
                }
            }
        }

        val verticalMatches = mutableListOf<Set<Pair<Int, Int>>>()
        for (c in 0 until 8) {
            var r = 0
            while (r < 8) {
                val startPiece = logicalGrid[r][c]
                if (startPiece.isEmpty || startPiece.id.startsWith("EMPTY") || startPiece.powerUp == GamePiecePowerUp.COLOR_BOMB) {
                    r++
                    continue
                }
                var count = 1
                while (r + count < 8) {
                    val nextPiece = logicalGrid[r + count][c]
                    if (!nextPiece.isEmpty &&
                        !nextPiece.id.startsWith("EMPTY") &&
                        nextPiece.powerUp != GamePiecePowerUp.COLOR_BOMB &&
                        nextPiece.type == startPiece.type
                    ) {
                        count++
                    } else {
                        break
                    }
                }
                if (count >= 3) {
                    verticalMatches.add((0 until count).map { (r + it) to c }.toSet())
                    r += count
                } else {
                    r++
                }
            }
        }

        val combined = mutableListOf<MutableSet<Pair<Int, Int>>>()
        val allLines = horizontalMatches + verticalMatches

        for (line in allLines) {
            var found = false
            for (set in combined) {
                if (line.any { it in set }) {
                    set.addAll(line)
                    found = true
                    break
                }
            }
            if (!found) combined.add(line.toMutableSet())
        }

        return combined
    }

    private fun hasPossibleMoves(): Boolean {
        return findBestHintMove() != null
    }

    /**
     * Busca la mejor jugada disponible en el tablero para el sistema de Pistas Automáticas (Idle Hints),
     * priorizando combinaciones de 4+, combinaciones de Power-Ups o uso de Color Bomb.
     */
    internal fun findBestHintMove(): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
        var bestMove: Pair<Pair<Int, Int>, Pair<Int, Int>>? = null
        var bestWeight = 0

        for (r in 0 until 8) {
            for (c in 0 until 8) {
                val neighbors = listOfNotNull(
                    if (c + 1 < 8) r to (c + 1) else null,
                    if (r + 1 < 8) (r + 1) to c else null
                )
                for ((nr, nc) in neighbors) {
                    val weight = evaluateSwapWeight(r, c, nr, nc)
                    if (weight > bestWeight) {
                        bestWeight = weight
                        bestMove = (r to c) to (nr to nc)
                    }
                }
            }
        }
        return bestMove
    }

    private fun evaluateSwapWeight(r1: Int, c1: Int, r2: Int, c2: Int): Int {
        val p1 = logicalGrid[r1][c1]
        val p2 = logicalGrid[r2][c2]
        if (p1.isEmpty || p2.isEmpty) return 0

        if (p1.powerUp == GamePiecePowerUp.COLOR_BOMB || p2.powerUp == GamePiecePowerUp.COLOR_BOMB) {
            return 100
        }
        if (p1.powerUp != GamePiecePowerUp.NONE && p2.powerUp != GamePiecePowerUp.NONE) {
            return 90
        }

        logicalGrid[r1][c1] = p2
        logicalGrid[r2][c2] = p1

        val matches = findMatches()
        val totalMatched = matches.sumOf { it.size }

        logicalGrid[r1][c1] = p1
        logicalGrid[r2][c2] = p2

        return totalMatched
    }

    private suspend fun shuffleBoard() {
        _uiState.update {
            it.copy(
                boardState = BoardState.SHUFFLING,
                cinthiaSpeech = "🔄 ¡Barajando gemas!"
            )
        }
        delay(420)

        var attempts = 0
        do {
            attempts++
            if (attempts <= 25) {
                val pieces = logicalGrid.flatten().shuffled().toMutableList()
                for (r in 0 until 8) {
                    for (c in 0 until 8) {
                        logicalGrid[r][c] = pieces.removeAt(0)
                    }
                }
            } else {
                // Fallback seguro si los colores actuales no permiten jugada sin match inicial
                for (r in 0 until 8) {
                    for (c in 0 until 8) {
                        val existingPowerUp = logicalGrid[r][c].powerUp
                        logicalGrid[r][c] = GamePiece(
                            type = randomType(),
                            powerUp = existingPowerUp
                        )
                    }
                }
            }
        } while ((findMatches().isNotEmpty() || !hasPossibleMoves()) && attempts < 50)

        syncUI()
        _effects.emit(GameEffect.PlaySound(GameSound.GEM_LAND))
    }

    private suspend fun checkGameOver() {
        val currentState = _uiState.value
        if (currentState.score >= currentState.targetScore && !currentState.isGameWon) {
            idleHintJob?.cancel()

            // BONUS FINALE: Convertir movimientos restantes en puntos extra con celebración jugosa
            var bonusScore = 0
            val movesToConvert = _uiState.value.movesLeft
            if (movesToConvert > 0) {
                _uiState.update {
                    it.copy(
                        boardState = BoardState.BONUS_FINALE,
                        cinthiaState = CinthiaState.CHEERING,
                        cinthiaSpeech = "🎉 ¡BONUS DE MOVIMIENTOS!"
                    )
                }
                for (step in 1..movesToConvert) {
                    val stepPoints = 60
                    bonusScore += stepPoints
                    val pitch = (1.0f + step * 0.06f).coerceIn(1.0f, 1.85f)
                    _effects.emit(GameEffect.PlaySound(GameSound.MATCH_SUCCESS_GEM, rate = pitch, volume = 0.75f))
                    _effects.emit(GameEffect.SwipeHaptic)

                    val randomRow = (1..6).random()
                    val randomCol = (1..6).random()
                    val popup = FloatingScorePopup(
                        row = randomRow.toFloat(),
                        col = randomCol.toFloat(),
                        pointsText = "+$stepPoints",
                        calloutText = if (step == 1) "¡BONUS!" else null,
                        colorHex = 0xFFFFD700L,
                        scaleMultiplier = 1.15f
                    )
                    _uiState.update { st ->
                        st.copy(
                            movesLeft = maxOf(0, st.movesLeft - 1),
                            score = st.score + stepPoints,
                            bonusMovesScore = bonusScore,
                            floatingPopups = (st.floatingPopups + popup).takeLast(5)
                        )
                    }
                    delay(110L)
                }
                delay(280L)
            }

            val finalState = _uiState.value
            val target = finalState.targetScore
            val score = finalState.score
            val stars = when {
                score >= (target * 1.8).toInt() -> 3
                score >= (target * 1.4).toInt() -> 2
                else -> 1
            }
            val streakBonusCoins = minOf(75, (finalState.winStreak + 1) * 15)
            val coinReward = 50 + stars * 25 + streakBonusCoins

            viewModelScope.launch {
                playerRepository.completeLevelAndUnlockNext(safeLevel, stars)
                playerRepository.addCoins(coinReward)
            }

            _uiState.update {
                it.copy(
                    isGameWon = true,
                    isGameOver = true,
                    boardState = BoardState.IDLE,
                    selectedCell = null,
                    hintCells = emptySet(),
                    cinthiaState = CinthiaState.CHEERING,
                    cinthiaSpeech = "👑 ¡Victoria Real!",
                    earnedStars = stars,
                    earnedCoinsReward = coinReward,
                    doubleCoinsClaimed = false,
                    winStreak = it.winStreak + 1
                )
            }

            // Secuencia de sonidos de Victoria, Público, Cinthia y Medalla (Bronce/Plata/Oro)
            viewModelScope.launch {
                _effects.emit(GameEffect.PlaySound(GameSound.GAME_WON))
                _effects.emit(GameEffect.PlaySound(GameSound.CROWD_VICTORY, delayMs = 250L))
                _effects.emit(GameEffect.PlaySound(GameSound.KNIGHT_CHEER_VICTORY, delayMs = 550L))
                val awardSound = when (stars) {
                    3 -> GameSound.AWARD_GOLD
                    2 -> GameSound.AWARD_SILVER
                    else -> GameSound.AWARD_BRONZE
                }
                _effects.emit(GameEffect.PlaySound(awardSound, delayMs = 950L))
            }
        } else if (currentState.movesLeft <= 0 && !currentState.isGameOver) {
            idleHintJob?.cancel()
            streakAtRiskOnLoss = currentState.winStreak
            if (streakAtRiskOnLoss > 0) {
                viewModelScope.launch {
                    playerRepository.resetWinStreak()
                }
            }
            _uiState.update {
                it.copy(
                    isGameOver = true,
                    boardState = BoardState.IDLE,
                    activeBooster = null,
                    selectedCell = null,
                    hintCells = emptySet(),
                    invalidSwapCells = emptySet(),
                    hitFlashCells = emptySet(),
                    matchParticles = emptySet(),
                    cinthiaState = CinthiaState.SAD,
                    cinthiaSpeech = "💔 ¡Estuvimos tan cerca!"
                )
            }
            // Secuencia de sonidos de Derrota y Público
            viewModelScope.launch {
                _effects.emit(GameEffect.PlaySound(GameSound.GAME_LOST))
                _effects.emit(GameEffect.PlaySound(GameSound.CROWD_DEFEAT, delayMs = 300L))
            }
        } else if (currentState.movesLeft in 1..5 && !currentState.isGameOver) {
            val wasAlreadyWorried = currentState.cinthiaState == CinthiaState.WORRIED
            _uiState.update { it.copy(cinthiaState = CinthiaState.WORRIED) }

            viewModelScope.launch {
                if (currentState.movesLeft == 1) {
                    _effects.emit(GameEffect.PlaySound(GameSound.TIMER_COUNT_FINAL))
                } else {
                    _effects.emit(GameEffect.PlaySound(GameSound.TIMER_COUNT))
                    if (!wasAlreadyWorried && currentState.movesLeft == 5) {
                        _effects.emit(GameEffect.PlaySound(GameSound.KNIGHT_CHEER_KEEP_GOING, delayMs = 250L))
                    }
                }
            }
        }
    }

    fun claimDoubleCoinsReward() {
        val state = _uiState.value
        if (!state.isGameWon || state.doubleCoinsClaimed || state.earnedCoinsReward <= 0) return

        _uiState.update { it.copy(doubleCoinsClaimed = true) }
        viewModelScope.launch {
            _effects.emit(GameEffect.PlaySound(GameSound.KNIGHT_CHEER_COMBO))
            playerRepository.addCoins(state.earnedCoinsReward)
        }
    }

    /**
     * Revive al jugador otorgando `+count` movimientos extra y desbloqueando inmediatamente
     * el tablero en `BoardState.IDLE` para que pueda seguir haciendo movimientos sin bloqueos.
     */
    fun addRewardMoves(count: Int) {
        if (count <= 0) return
        idleHintJob?.cancel()
        val restoredStreak = maxOf(_uiState.value.winStreak, streakAtRiskOnLoss)
        streakAtRiskOnLoss = 0

        _uiState.update {
            it.copy(
                movesLeft = maxOf(0, it.movesLeft) + count,
                isGameOver = false,
                isGameWon = false,
                boardState = BoardState.IDLE,
                activeBooster = null,
                selectedCell = null,
                hintCells = emptySet(),
                invalidSwapCells = emptySet(),
                hitFlashCells = emptySet(),
                matchParticles = emptySet(),
                lastMatchCombo = 0,
                winStreak = restoredStreak,
                cinthiaState = CinthiaState.CHEERING,
                cinthiaSpeech = "💪 ¡+$count movimientos extra!"
            )
        }
        viewModelScope.launch {
            if (restoredStreak > 0) {
                playerRepository.restoreWinStreak(restoredStreak)
            }
            _effects.emit(GameEffect.PlaySound(GameSound.KNIGHT_CHEER_KEEP_GOING))
            if (!hasPossibleMoves()) {
                shuffleBoard()
                _uiState.update { it.copy(boardState = BoardState.IDLE) }
            }
            scheduleIdleHintTimer()
        }
    }

    /**
     * Permite continuar con +5 movimientos usando monedas (80 🪙) cuando el jugador está en zona Near-Miss
     * y quiere salvar su Racha de Victorias.
     */
    fun continueWithCoins(cost: Int = 80, extraMoves: Int = 5) {
        viewModelScope.launch {
            val spent = playerRepository.spendCoins(cost)
            if (spent) {
                addRewardMoves(extraMoves)
            } else {
                _effects.emit(GameEffect.PlaySound(GameSound.TILE_INVALID))
                _effects.emit(GameEffect.InvalidMoveHaptic)
            }
        }
    }

    /**
     * Reinicia el nivel actual al instante sin obligar al jugador a volver al Mapa de Mundos.
     */
    fun retryLevel() {
        idleHintJob?.cancel()
        streakAtRiskOnLoss = 0
        viewModelScope.launch {
            playerRepository.resetWinStreak()
            val levelConfig = WorldRepository.getLevelConfig(safeLevel)
            initializeBoard()
            _uiState.update {
                it.copy(
                    score = 0,
                    movesLeft = levelConfig.moves,
                    boardState = BoardState.IDLE,
                    isGameOver = false,
                    isGameWon = false,
                    cinthiaState = CinthiaState.IDLE,
                    cinthiaSpeech = "⚔️ ¡Nuevo intento!",
                    lastMatchCombo = 0,
                    matchParticles = emptySet(),
                    hitFlashCells = emptySet(),
                    selectedCell = null,
                    hintCells = emptySet(),
                    invalidSwapCells = emptySet(),
                    floatingPopups = emptyList(),
                    activeBooster = null,
                    earnedStars = 0,
                    earnedCoinsReward = 0,
                    bonusMovesScore = 0,
                    doubleCoinsClaimed = false,
                    winStreak = 0,
                    streakStarterCount = 0
                )
            }
            _effects.emit(GameEffect.PlaySound(GameSound.GEM_LAND))
            scheduleIdleHintTimer()
        }
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch { playerRepository.setSoundEnabled(enabled) }
    }

    fun toggleHaptics(enabled: Boolean) {
        viewModelScope.launch { playerRepository.setHapticsEnabled(enabled) }
    }

    fun toggleReducedMotion(enabled: Boolean) {
        viewModelScope.launch { playerRepository.setReducedMotion(enabled) }
    }

    fun cancelActiveBooster() {
        if (_uiState.value.boardState == BoardState.WAITING_FOR_BOOSTER_TARGET) {
            _uiState.update {
                it.copy(
                    activeBooster = null,
                    boardState = BoardState.IDLE
                )
            }
            viewModelScope.launch {
                _effects.emit(GameEffect.PlaySound(GameSound.TILE_DESELECT))
                scheduleIdleHintTimer()
            }
        }
    }

    fun onBoosterSelected(type: BoosterType) {
        val currentState = _uiState.value
        if (currentState.boardState != BoardState.IDLE && currentState.boardState != BoardState.WAITING_FOR_BOOSTER_TARGET) return
        if (currentState.isGameOver || currentState.isGameWon) return

        clearInteractionOverlays()
        if (currentState.activeBooster == type) {
            cancelActiveBooster()
            return
        }

        val count = currentState.boosterInventory[type] ?: 0
        if (count > 0) {
            if (type == BoosterType.SHUFFLE) {
                viewModelScope.launch {
                    _effects.emit(GameEffect.PlaySound(GameSound.TILE_SELECT))
                    playerRepository.updateBoosterCount(BoosterType.SHUFFLE, -1)
                    shuffleBoard()
                    val matches = findMatches()
                    if (matches.isNotEmpty()) {
                        resolveMatches(matches)
                    } else {
                        _uiState.update { it.copy(boardState = BoardState.IDLE) }
                        scheduleIdleHintTimer()
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        activeBooster = type,
                        boardState = BoardState.WAITING_FOR_BOOSTER_TARGET
                    )
                }
                viewModelScope.launch {
                    _effects.emit(GameEffect.PlaySound(GameSound.BOMB_POWER_RISE))
                }
            }
        } else {
            viewModelScope.launch {
                _effects.emit(GameEffect.PlaySound(GameSound.TILE_INVALID))
                _effects.emit(GameEffect.InvalidMoveHaptic)
            }
        }
    }

    /**
     * Soporta tanto el uso de Boosters como el control Tap-to-Select / Tap-to-Swap
     * para maximizar la precisión y responsividad táctil (Real-Time Control de Steve Swink).
     */
    fun onPieceTapped(row: Int, col: Int) {
        if (row !in 0..7 || col !in 0..7) return
        val state = _uiState.value
        if (state.isGameWon || state.isGameOver) return
        if (logicalGrid[row][col].isEmpty) return

        // Caso 1: Aplicar Booster activo sobre la celda tocada
        if (state.boardState == BoardState.WAITING_FOR_BOOSTER_TARGET && state.activeBooster != null) {
            viewModelScope.launch {
                val booster = state.activeBooster
                _effects.emit(GameEffect.PlaySound(GameSound.TILE_SELECT))
                _uiState.update { it.copy(activeBooster = null, boardState = BoardState.RESOLVING) }

                val affected = mutableSetOf<Pair<Int, Int>>()
                when (booster) {
                    BoosterType.HAMMER -> {
                        affected.add(row to col)
                    }
                    BoosterType.DYNAMITE -> {
                        for (r in (row - 1)..(row + 1)) {
                            for (c in (col - 1)..(col + 1)) {
                                if (r in 0..7 && c in 0..7) affected.add(r to c)
                            }
                        }
                    }
                    else -> {}
                }

                if (affected.isNotEmpty()) {
                    playerRepository.updateBoosterCount(booster, -1)
                    destroyPieces(affected, boosterUsed = booster)
                } else {
                    _uiState.update { it.copy(boardState = BoardState.IDLE) }
                    scheduleIdleHintTimer()
                }
            }
            return
        }

        // Caso 2: Tap-to-Select y Tap-to-Swap en estado IDLE
        if (state.boardState == BoardState.IDLE) {
            val currentSelected = state.selectedCell
            if (currentSelected == null) {
                _uiState.update {
                    it.copy(
                        selectedCell = row to col,
                        hintCells = emptySet()
                    )
                }
                viewModelScope.launch {
                    _effects.emit(GameEffect.PlaySound(GameSound.TILE_SELECT))
                    _effects.emit(GameEffect.SwipeHaptic)
                }
                scheduleIdleHintTimer()
            } else if (currentSelected == (row to col)) {
                _uiState.update { it.copy(selectedCell = null) }
                viewModelScope.launch {
                    _effects.emit(GameEffect.PlaySound(GameSound.TILE_DESELECT))
                }
                scheduleIdleHintTimer()
            } else {
                val manhattan = abs(currentSelected.first - row) + abs(currentSelected.second - col)
                if (manhattan == 1) {
                    clearInteractionOverlays()
                    viewModelScope.launch {
                        processMove(currentSelected.first, currentSelected.second, row, col)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            selectedCell = row to col,
                            hintCells = emptySet()
                        )
                    }
                    viewModelScope.launch {
                        _effects.emit(GameEffect.PlaySound(GameSound.TILE_SELECT))
                        _effects.emit(GameEffect.SwipeHaptic)
                    }
                    scheduleIdleHintTimer()
                }
            }
        }
    }

    private suspend fun destroyPieces(
        coords: Set<Pair<Int, Int>>,
        boosterUsed: BoosterType? = null
    ) {
        val expansion = expandWithPowerUps(coords)
        val finalDestructionSet = expansion.finalCoords
        val triggeredPowerUps = expansion.triggeredPowerUps
        val powerUpTriggered = triggeredPowerUps.isNotEmpty()

        // Hit-Stop y Flash de impacto en uso de Booster
        _uiState.update { it.copy(hitFlashCells = finalDestructionSet) }
        delay(65L)
        _uiState.update { it.copy(hitFlashCells = emptySet()) }

        when (boosterUsed) {
            BoosterType.HAMMER -> {
                _effects.emit(GameEffect.PlaySound(GameSound.MATCH_STONE))
                if (GamePiecePowerUp.COLOR_BOMB in triggeredPowerUps) {
                    _effects.emit(GameEffect.PlaySound(GameSound.BOMB_EXPLODE_RED, delayMs = 80L))
                } else if (powerUpTriggered) {
                    _effects.emit(GameEffect.PlaySound(GameSound.BOMB_EXPLODE, delayMs = 80L))
                }
            }
            BoosterType.DYNAMITE -> {
                if (GamePiecePowerUp.COLOR_BOMB in triggeredPowerUps) {
                    _effects.emit(GameEffect.PlaySound(GameSound.BOMB_EXPLODE_RED))
                } else {
                    _effects.emit(GameEffect.PlaySound(GameSound.BOMB_EXPLODE))
                }
            }
            else -> {
                if (GamePiecePowerUp.COLOR_BOMB in triggeredPowerUps) {
                    _effects.emit(GameEffect.PlaySound(GameSound.BOMB_EXPLODE_RED))
                } else if (powerUpTriggered) {
                    _effects.emit(GameEffect.PlaySound(GameSound.BOMB_EXPLODE))
                }
            }
        }

        if (powerUpTriggered || boosterUsed == BoosterType.DYNAMITE) {
            _effects.emit(GameEffect.ComboHaptic(intensity = 3))
        } else {
            _effects.emit(GameEffect.MatchHaptic)
        }
        _effects.emit(
            GameEffect.ScreenShake(
                intensityPx = if (boosterUsed == BoosterType.DYNAMITE || powerUpTriggered) 15f else 9f,
                cycles = 5
            )
        )

        val cellColorMap = finalDestructionSet.associateWith { (r, c) ->
            logicalGrid[r][c].type.particleColorHex
        }
        _effects.emit(
            GameEffect.SpawnParticles(
                ParticleBurstEvent(
                    cellColors = cellColorMap,
                    shockwaveCenters = expansion.shockwaveCenters + listOfNotNull(coords.firstOrNull()),
                    lineBlasterRows = expansion.lineBlasterRows,
                    lineBlasterCols = expansion.lineBlasterCols
                )
            )
        )

        val points = finalDestructionSet.size * 15
        val center = coords.firstOrNull() ?: (3 to 3)
        val popup = FloatingScorePopup(
            row = center.first.toFloat(),
            col = center.second.toFloat(),
            pointsText = "+$points",
            calloutText = if (boosterUsed == BoosterType.DYNAMITE) "💥 ¡BOOM!" else "🔨 ¡GOLPE!",
            colorHex = 0xFFFFD700L,
            scaleMultiplier = 1.25f
        )

        _uiState.update {
            it.copy(
                score = it.score + points,
                matchParticles = finalDestructionSet,
                floatingPopups = (it.floatingPopups + popup).takeLast(5),
                cinthiaState = CinthiaState.CHEERING
            )
        }

        viewModelScope.launch {
            delay(950)
            _uiState.update { state ->
                state.copy(floatingPopups = state.floatingPopups.filterNot { it.id == popup.id })
            }
        }

        viewModelScope.launch {
            playerRepository.recordMatchTelemetry(matchesMade = 1, powerUpsTriggered = 1)
        }

        if (points >= 100) {
            viewModelScope.launch {
                playerRepository.addCoins(points / 100)
            }
        }

        delay(180)
        _uiState.update { it.copy(matchParticles = emptySet()) }

        for (coord in finalDestructionSet) {
            logicalGrid[coord.first][coord.second] = GamePiece(
                type = randomType(),
                id = "EMPTY_${UUID.randomUUID()}",
                isEmpty = true
            )
        }

        _uiState.update { it.copy(boardState = BoardState.DROPPING) }
        applyGravity()
        syncUI()
        _effects.emit(GameEffect.PlaySound(GameSound.GEM_LAND, delayMs = 120L))
        delay(270)

        val matches = findMatches()
        if (matches.isNotEmpty()) {
            resolveMatches(matches)
        } else {
            if (!hasPossibleMoves()) {
                shuffleBoard()
            }
            checkGameOver()
            _uiState.update { it.copy(boardState = BoardState.IDLE) }
            if (!_uiState.value.isGameOver && !_uiState.value.isGameWon) {
                scheduleIdleHintTimer()
            }
        }
    }

    companion object {
        private const val IDLE_HINT_DELAY_MS = 4500L
    }

    class Factory(
        private val playerRepository: PlayerRepository,
        private val selectedLevel: Int = 1
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return GameViewModel(playerRepository, selectedLevel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
