package com.hkemtrentainment.cinthiasquestr.ui.gameboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hkemtrentainment.cinthiasquestr.audio.GameSound
import com.hkemtrentainment.cinthiasquestr.data.PlayerRepository
import com.hkemtrentainment.cinthiasquestr.data.WorldRepository
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

enum class BoardState {
    IDLE, SWAPPING, RESOLVING, DROPPING, SHUFFLING, WAITING_FOR_BOOSTER_TARGET
}

data class GameUIState(
    val grid: List<List<GamePiece>> = emptyList(),
    val score: Int = 0,
    val movesLeft: Int = 20,
    val boardState: BoardState = BoardState.IDLE,
    val isGameOver: Boolean = false,
    val isGameWon: Boolean = false,
    val targetScore: Int = 2000,
    val cinthiaState: CinthiaState = CinthiaState.IDLE,
    val lastMatchCombo: Int = 0,
    val matchParticles: Set<Pair<Int, Int>> = emptySet(),
    val boosterInventory: Map<BoosterType, Int> = emptyMap(),
    val activeBooster: BoosterType? = null,
    val coins: Int = 0,
    val level: Int = 1,
    val earnedStars: Int = 0,
    val earnedCoinsReward: Int = 0,
    val doubleCoinsClaimed: Boolean = false
)

sealed class GameEffect {
    data object SwipeHaptic : GameEffect()
    data object MatchHaptic : GameEffect()
    data object ComboHaptic : GameEffect()
    data object ScreenShake : GameEffect()
    data class PlaySound(val sound: GameSound, val delayMs: Long = 0L) : GameEffect()
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

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 32)
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

    private val logicalGrid = MutableList(8) { MutableList(8) { GamePiece(type = randomType()) } }

    init {
        val levelConfig = WorldRepository.getLevelConfig(safeLevel)
        _uiState.update {
            it.copy(
                level = safeLevel,
                targetScore = levelConfig.targetScore,
                movesLeft = levelConfig.moves
            )
        }

        viewModelScope.launch {
            playerRepository.playerDataFlow.collect { playerData ->
                _uiState.update {
                    it.copy(
                        boosterInventory = mapOf(
                            BoosterType.HAMMER to playerData.hammer,
                            BoosterType.DYNAMITE to playerData.dynamite,
                            BoosterType.SHUFFLE to playerData.shuffle
                        ),
                        coins = playerData.coins
                    )
                }
            }
        }
        initializeBoard()
    }

    private fun initializeBoard() {
        var hasMatches: Boolean
        do {
            hasMatches = false
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    logicalGrid[r][c] = GamePiece(type = randomType())
                }
            }
            val matches = findMatches()
            if (matches.isNotEmpty()) hasMatches = true
        } while (hasMatches || !hasPossibleMoves())

        syncUI()
    }

    private fun randomType() = GamePieceType.entries.random()

    private fun syncUI() {
        _uiState.update { it.copy(grid = logicalGrid.map { row -> row.toList() }) }
    }

    fun onSwipe(row: Int, col: Int, direction: SwipeDirection) {
        if (_uiState.value.boardState != BoardState.IDLE || _uiState.value.isGameOver || _uiState.value.isGameWon) return

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

        viewModelScope.launch {
            processMove(row, col, targetRow, targetCol)
        }
    }

    private suspend fun processMove(r1: Int, c1: Int, r2: Int, c2: Int) {
        _uiState.update { it.copy(boardState = BoardState.SWAPPING) }
        _effects.emit(GameEffect.SwipeHaptic)

        // Intercambio lógico
        val temp = logicalGrid[r1][c1]
        logicalGrid[r1][c1] = logicalGrid[r2][c2]
        logicalGrid[r2][c2] = temp
        syncUI()

        delay(250) // Tiempo de animación de swap

        val movedPiece = logicalGrid[r2][c2]
        val otherPiece = logicalGrid[r1][c1]

        // 1. Verificar si es un intercambio especial de COLOR_BOMB o combinación de 2 Power-Ups
        val specialSwapCoords = evaluateSpecialSwap(r1, c1, otherPiece, r2, c2, movedPiece)
        if (specialSwapCoords.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    movesLeft = it.movesLeft - 1,
                    boardState = BoardState.RESOLVING
                )
            }
            resolveMatches(
                initialMatches = listOf(specialSwapCoords),
                swapOrigin = r2 to c2,
                isSpecialSwap = true
            )
            return
        }

        // 2. Verificar matches normales de 3 o más piezas
        val matches = findMatches()
        if (matches.isEmpty()) {
            // Sonido de movimiento inválido (Tile_Invalid.WAV)
            _effects.emit(GameEffect.PlaySound(GameSound.TILE_INVALID))

            // Deshacer swap si no hay combinación válida
            val tempBack = logicalGrid[r1][c1]
            logicalGrid[r1][c1] = logicalGrid[r2][c2]
            logicalGrid[r2][c2] = tempBack
            syncUI()
            delay(250)
            _uiState.update { it.copy(boardState = BoardState.IDLE) }
        } else {
            _uiState.update {
                it.copy(
                    movesLeft = it.movesLeft - 1,
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
        // Doble Color Bomb: limpia todo el tablero
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
            val targetType = if (p1.powerUp == GamePiecePowerUp.COLOR_BOMB) p2.type else p1.type
            val affected = mutableSetOf(r1 to c1, r2 to c2)
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    if (!logicalGrid[r][c].isEmpty && logicalGrid[r][c].type == targetType) {
                        affected.add(r to c)
                    }
                }
            }
            return affected
        }

        // Dos Power-Ups cualesquiera intercambiados entre sí se activan juntos
        if (p1.powerUp != GamePiecePowerUp.NONE && p2.powerUp != GamePiecePowerUp.NONE) {
            return setOf(r1 to c1, r2 to c2)
        }

        return emptySet()
    }

    private fun expandWithPowerUps(seedCoords: Set<Pair<Int, Int>>): Pair<Set<Pair<Int, Int>>, Set<GamePiecePowerUp>> {
        val finalDestructionSet = seedCoords.toMutableSet()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.addAll(seedCoords)
        val processedPowerUps = mutableSetOf<Pair<Int, Int>>()
        val triggeredPowerUps = mutableSetOf<GamePiecePowerUp>()

        while (queue.isNotEmpty()) {
            val coord = queue.poll() ?: continue
            val piece = logicalGrid[coord.first][coord.second]
            if (!piece.isEmpty && piece.powerUp != GamePiecePowerUp.NONE && coord !in processedPowerUps) {
                processedPowerUps.add(coord)
                triggeredPowerUps.add(piece.powerUp)

                when (piece.powerUp) {
                    GamePiecePowerUp.LINE_BLASTER -> {
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
                        val targetType = piece.type
                        for (r in 0 until 8) {
                            for (c in 0 until 8) {
                                if (!logicalGrid[r][c].isEmpty && logicalGrid[r][c].type == targetType) {
                                    if (finalDestructionSet.add(r to c)) queue.add(r to c)
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
        return finalDestructionSet to triggeredPowerUps
    }

    private suspend fun resolveMatches(
        initialMatches: List<Set<Pair<Int, Int>>>,
        swapOrigin: Pair<Int, Int>? = null,
        isSpecialSwap: Boolean = false
    ) {
        var matches = initialMatches
        var combo = 0

        while (matches.isNotEmpty()) {
            combo++
            _uiState.update {
                it.copy(
                    boardState = BoardState.RESOLVING,
                    lastMatchCombo = combo,
                    cinthiaState = if (combo >= 2) CinthiaState.CHEERING else it.cinthiaState
                )
            }

            val allMatchedCoords = matches.flatten().toSet()
            val (finalDestructionSet, triggeredPowerUps) = expandWithPowerUps(allMatchedCoords)
            val powerUpTriggered = triggeredPowerUps.isNotEmpty() || isSpecialSwap

            // Generar nuevos power-ups si hubo alineaciones de 4, 5 o L/T
            val powerUpsToSpawn = mutableListOf<Triple<Int, Int, GamePiecePowerUp>>()
            for (matchSet in matches) {
                val powerUp = determinePowerUp(matchSet)
                if (powerUp != GamePiecePowerUp.NONE) {
                    val spawnPos = if (swapOrigin != null && swapOrigin in matchSet) swapOrigin else matchSet.first()
                    powerUpsToSpawn.add(Triple(spawnPos.first, spawnPos.second, powerUp))
                }
            }

            // 1. Sonido de combinación (Match_Triple, Match_SuccessGem o Match_Combo)
            val hasBigMatch = powerUpsToSpawn.isNotEmpty() || matches.any { it.size >= 4 } || (isSpecialSwap && combo == 1)
            when {
                combo >= 2 -> _effects.emit(GameEffect.PlaySound(GameSound.MATCH_COMBO))
                hasBigMatch -> _effects.emit(GameEffect.PlaySound(GameSound.MATCH_SUCCESS_GEM))
                else -> _effects.emit(GameEffect.PlaySound(GameSound.MATCH_TRIPLE))
            }

            // 2. Sonido de explosión de Bomba / Power-Up activado (Bomb_Explode o Bomb_ExplodeRed)
            if (GamePiecePowerUp.COLOR_BOMB in triggeredPowerUps || (isSpecialSwap && combo == 1)) {
                _effects.emit(GameEffect.PlaySound(GameSound.BOMB_EXPLODE_RED))
            } else if (triggeredPowerUps.isNotEmpty()) {
                _effects.emit(GameEffect.PlaySound(GameSound.BOMB_EXPLODE))
            }

            // 3. Sonido de creación/carga de nuevo Power-Up (BombPower_Full o BombPower_Rise)
            if (powerUpsToSpawn.isNotEmpty()) {
                val spawnedTypes = powerUpsToSpawn.map { it.third }.toSet()
                if (GamePiecePowerUp.COLOR_BOMB in spawnedTypes || GamePiecePowerUp.EXPLOSIVE_BOMB in spawnedTypes) {
                    _effects.emit(GameEffect.PlaySound(GameSound.BOMB_POWER_FULL, delayMs = 80L))
                } else if (GamePiecePowerUp.LINE_BLASTER in spawnedTypes) {
                    _effects.emit(GameEffect.PlaySound(GameSound.BOMB_POWER_RISE, delayMs = 80L))
                }
            }

            // 4. Voz de celebración de Cinthia (Knight_Cheer_*) en combos o grandes explosiones
            if (combo == 2 || (combo >= 4 && combo % 2 == 0) || (combo == 1 && powerUpTriggered)) {
                _effects.emit(GameEffect.PlaySound(GameSound.KNIGHT_CHEER_COMBO, delayMs = 120L))
            }

            if (powerUpTriggered || combo > 1) {
                _effects.emit(GameEffect.ComboHaptic)
            } else {
                _effects.emit(GameEffect.MatchHaptic)
            }
            _effects.emit(GameEffect.ScreenShake)

            val points = finalDestructionSet.size * 10 * combo
            _uiState.update { it.copy(score = it.score + points, matchParticles = finalDestructionSet) }

            if (points >= 100) {
                viewModelScope.launch {
                    playerRepository.addCoins(points / 100)
                }
            }

            delay(200)
            _uiState.update { it.copy(matchParticles = emptySet()) }

            val powerUpPositions = powerUpsToSpawn.map { it.first to it.second }.toSet()
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
                    isEmpty = false
                )
            }

            _uiState.update { it.copy(boardState = BoardState.DROPPING) }
            applyGravity()
            syncUI()
            // Sonido de caída / aterrizaje de gemas (Gem_Land01..03.WAV)
            _effects.emit(GameEffect.PlaySound(GameSound.GEM_LAND, delayMs = 140L))
            delay(300)

            matches = findMatches()
        }

        if (!hasPossibleMoves()) {
            shuffleBoard()
        }

        checkGameOver()
        _uiState.update { state ->
            val nextCinthiaState = when {
                state.isGameWon -> CinthiaState.CHEERING
                state.isGameOver -> CinthiaState.SAD
                state.movesLeft <= 5 -> CinthiaState.WORRIED
                else -> CinthiaState.IDLE
            }
            state.copy(
                boardState = BoardState.IDLE,
                lastMatchCombo = 0,
                cinthiaState = nextCinthiaState
            )
        }
    }

    private fun determinePowerUp(matchSet: Set<Pair<Int, Int>>): GamePiecePowerUp {
        val hLines = matchSet.groupBy { it.first }.values.filter { it.size >= 3 }
        val vLines = matchSet.groupBy { it.second }.values.filter { it.size >= 3 }

        if (hLines.isNotEmpty() && vLines.isNotEmpty()) return GamePiecePowerUp.EXPLOSIVE_BOMB
        if (hLines.any { it.size >= 5 } || vLines.any { it.size >= 5 }) return GamePiecePowerUp.COLOR_BOMB
        if (hLines.any { it.size >= 4 } || vLines.any { it.size >= 4 }) return GamePiecePowerUp.LINE_BLASTER

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
                if (startPiece.isEmpty || startPiece.id.startsWith("EMPTY")) {
                    c++
                    continue
                }
                var count = 1
                while (c + count < 8) {
                    val nextPiece = logicalGrid[r][c + count]
                    if (!nextPiece.isEmpty && !nextPiece.id.startsWith("EMPTY") && nextPiece.type == startPiece.type) {
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
                if (startPiece.isEmpty || startPiece.id.startsWith("EMPTY")) {
                    r++
                    continue
                }
                var count = 1
                while (r + count < 8) {
                    val nextPiece = logicalGrid[r + count][c]
                    if (!nextPiece.isEmpty && !nextPiece.id.startsWith("EMPTY") && nextPiece.type == startPiece.type) {
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
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                val piece = logicalGrid[r][c]
                if (!piece.isEmpty && piece.powerUp == GamePiecePowerUp.COLOR_BOMB) {
                    return true
                }
                if (c + 1 < 8) {
                    if (checkSwapForMatch(r, c, r, c + 1)) return true
                }
                if (r + 1 < 8) {
                    if (checkSwapForMatch(r, c, r + 1, c)) return true
                }
            }
        }
        return false
    }

    private fun checkSwapForMatch(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        val p1 = logicalGrid[r1][c1]
        val p2 = logicalGrid[r2][c2]
        if (p1.powerUp != GamePiecePowerUp.NONE && p2.powerUp != GamePiecePowerUp.NONE) return true

        logicalGrid[r1][c1] = p2
        logicalGrid[r2][c2] = p1

        val hasMatch = findMatches().isNotEmpty()

        logicalGrid[r1][c1] = p1
        logicalGrid[r2][c2] = p2

        return hasMatch
    }

    private suspend fun shuffleBoard() {
        _uiState.update { it.copy(boardState = BoardState.SHUFFLING) }
        delay(500)

        do {
            val pieces = logicalGrid.flatten().shuffled().toMutableList()
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    logicalGrid[r][c] = pieces.removeAt(0)
                }
            }
        } while (findMatches().isNotEmpty() || !hasPossibleMoves())

        syncUI()
        _effects.emit(GameEffect.PlaySound(GameSound.GEM_LAND))
    }

    private fun checkGameOver() {
        val currentState = _uiState.value
        if (currentState.score >= currentState.targetScore && !currentState.isGameWon) {
            val target = currentState.targetScore
            val score = currentState.score
            val stars = when {
                score >= (target * 1.8).toInt() -> 3
                score >= (target * 1.4).toInt() -> 2
                else -> 1
            }
            val coinReward = 50 + stars * 25

            viewModelScope.launch {
                playerRepository.completeLevelAndUnlockNext(safeLevel, stars)
                playerRepository.addCoins(coinReward)
            }

            _uiState.update {
                it.copy(
                    isGameWon = true,
                    isGameOver = true,
                    boardState = BoardState.IDLE,
                    cinthiaState = CinthiaState.CHEERING,
                    earnedStars = stars,
                    earnedCoinsReward = coinReward,
                    doubleCoinsClaimed = false
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
            _uiState.update {
                it.copy(
                    isGameOver = true,
                    cinthiaState = CinthiaState.SAD
                )
            }
            // Secuencia de sonidos de Derrota y Público (Game_Lost.WAV + Crowd_Defeat.WAV)
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

    fun addRewardMoves(count: Int) {
        _uiState.update {
            it.copy(
                movesLeft = it.movesLeft + count,
                isGameOver = false,
                cinthiaState = CinthiaState.IDLE
            )
        }
        viewModelScope.launch {
            _effects.emit(GameEffect.PlaySound(GameSound.KNIGHT_CHEER_KEEP_GOING))
        }
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
            }
        }
    }

    fun onBoosterSelected(type: BoosterType) {
        if (_uiState.value.boardState != BoardState.IDLE && _uiState.value.boardState != BoardState.WAITING_FOR_BOOSTER_TARGET) return
        if (_uiState.value.isGameOver) return

        val currentState = _uiState.value
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
            }
        }
    }

    fun onPieceTapped(row: Int, col: Int) {
        val state = _uiState.value
        if (state.isGameWon || state.isGameOver) return
        if (state.boardState != BoardState.WAITING_FOR_BOOSTER_TARGET || state.activeBooster == null) return

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
            }
        }
    }

    private suspend fun destroyPieces(
        coords: Set<Pair<Int, Int>>,
        boosterUsed: BoosterType? = null
    ) {
        val (finalDestructionSet, triggeredPowerUps) = expandWithPowerUps(coords)
        val powerUpTriggered = triggeredPowerUps.isNotEmpty()

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

        if (powerUpTriggered) {
            _effects.emit(GameEffect.ComboHaptic)
        } else {
            _effects.emit(GameEffect.MatchHaptic)
        }
        _effects.emit(GameEffect.ScreenShake)

        val points = finalDestructionSet.size * 15
        _uiState.update {
            it.copy(
                score = it.score + points,
                matchParticles = finalDestructionSet
            )
        }

        if (points >= 100) {
            viewModelScope.launch {
                playerRepository.addCoins(points / 100)
            }
        }

        delay(200)
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
        _effects.emit(GameEffect.PlaySound(GameSound.GEM_LAND, delayMs = 140L))
        delay(300)

        val matches = findMatches()
        if (matches.isNotEmpty()) {
            resolveMatches(matches)
        } else {
            if (!hasPossibleMoves()) {
                shuffleBoard()
            }
            checkGameOver()
            _uiState.update { it.copy(boardState = BoardState.IDLE) }
        }
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
