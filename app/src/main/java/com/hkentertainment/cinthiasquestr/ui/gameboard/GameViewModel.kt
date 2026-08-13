package com.hkentertainment.cinthiasquestr.ui.gameboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hkentertainment.cinthiasquestr.data.PlayerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    val level: Int = 1
)

sealed class GameEffect {
    object SwipeHaptic : GameEffect()
    object MatchHaptic : GameEffect()
    object ComboHaptic : GameEffect()
    object ScreenShake : GameEffect()
}

enum class SwipeDirection {
    UP, DOWN, LEFT, RIGHT
}

class GameViewModel(private val playerRepository: PlayerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(GameUIState())
    val uiState: StateFlow<GameUIState> = _uiState.asStateFlow()

    // Backward compatibility for the screen
    val gameState: StateFlow<GameUIState> = uiState

    private val _effects = MutableSharedFlow<GameEffect>()
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

    private val logicalGrid = MutableList(8) { MutableList(8) { GamePiece(type = randomType()) } }

    init {
        viewModelScope.launch {
            playerRepository.playerDataFlow.collect { playerData ->
                _uiState.update { it.copy(
                    boosterInventory = mapOf(
                        BoosterType.HAMMER to playerData.hammer,
                        BoosterType.DYNAMITE to playerData.dynamite,
                        BoosterType.SHUFFLE to playerData.shuffle
                    ),
                    coins = playerData.coins,
                    level = playerData.level
                ) }
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

        // Swap logical
        val temp = logicalGrid[r1][c1]
        logicalGrid[r1][c1] = logicalGrid[r2][c2]
        logicalGrid[r2][c2] = temp
        syncUI()

        delay(250) // Swap animation time

        val matches = findMatches()
        if (matches.isEmpty()) {
            // Swap back
            val tempBack = logicalGrid[r1][c1]
            logicalGrid[r1][c1] = logicalGrid[r2][c2]
            logicalGrid[r2][c2] = tempBack
            syncUI()
            delay(250)
            _uiState.update { it.copy(boardState = BoardState.IDLE) }
        } else {
            // Success! 
            _uiState.update { 
                it.copy(
                    movesLeft = it.movesLeft - 1,
                    boardState = BoardState.RESOLVING
                ) 
            }
            resolveMatches(matches, swapOrigin = r2 to c2)
        }
    }

    private suspend fun resolveMatches(initialMatches: List<Set<Pair<Int, Int>>>, swapOrigin: Pair<Int, Int>? = null) {
        var matches = initialMatches
        var combo = 0
        
        while (matches.isNotEmpty()) {
            combo++
            _uiState.update { it.copy(boardState = BoardState.RESOLVING, lastMatchCombo = combo) }
            
            val allMatchedCoords = matches.flatten().toSet()
            
            // Expand destruction set for power-ups and chain reactions
            val finalDestructionSet = allMatchedCoords.toMutableSet()
            val queue = java.util.ArrayDeque<Pair<Int, Int>>()
            queue.addAll(allMatchedCoords)
            val processedPowerUps = mutableSetOf<Pair<Int, Int>>()
            var powerUpTriggered = false

            while (queue.isNotEmpty()) {
                val coord = queue.poll() ?: continue
                val piece = logicalGrid[coord.first][coord.second]
                if (piece.powerUp != GamePiecePowerUp.NONE && coord !in processedPowerUps) {
                    processedPowerUps.add(coord)
                    powerUpTriggered = true
                    
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
                                    if (logicalGrid[r][c].type == targetType) {
                                        if (finalDestructionSet.add(r to c)) queue.add(r to c)
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }

            // Spawn power-ups
            val powerUpsToSpawn = mutableListOf<Triple<Int, Int, GamePiecePowerUp>>()
            for (matchSet in matches) {
                val powerUp = determinePowerUp(matchSet)
                if (powerUp != GamePiecePowerUp.NONE) {
                    // Spawn at swap origin if part of match, otherwise center of match
                    val spawnPos = if (swapOrigin != null && swapOrigin in matchSet) swapOrigin else matchSet.first()
                    powerUpsToSpawn.add(Triple(spawnPos.first, spawnPos.second, powerUp))
                }
            }

            // Haptics & Shake
            if (powerUpTriggered || combo > 1) {
                _effects.emit(GameEffect.ComboHaptic)
            } else {
                _effects.emit(GameEffect.MatchHaptic)
            }
            _effects.emit(GameEffect.ScreenShake)

            // Update score
            val points = finalDestructionSet.size * 10 * combo
            _uiState.update { it.copy(score = it.score + points, matchParticles = finalDestructionSet) }
            
            // Reward coins for matches (e.g., 1 coin for every 100 points)
            if (points >= 100) {
                viewModelScope.launch {
                    playerRepository.addCoins(points / 100)
                }
            }
            
            delay(200) // Destruction anim
            _uiState.update { it.copy(matchParticles = emptySet()) }

            // Apply logical removal (except where power-ups spawn)
            val powerUpPositions = powerUpsToSpawn.map { it.first to it.second }.toSet()
            for (coord in finalDestructionSet) {
                if (coord !in powerUpPositions) {
                    // Use a unique ID with an EMPTY prefix to avoid key collisions in Compose
                    // while still allowing the logic to identify pieces to be removed.
                    logicalGrid[coord.first][coord.second] = GamePiece(
                        type = GamePieceType.entries.random(),
                        id = "EMPTY_${java.util.UUID.randomUUID()}"
                    )
                }
            }
            
            // Place power-ups
            for (p in powerUpsToSpawn) {
                val basePiece = logicalGrid[p.first][p.second]
                logicalGrid[p.first][p.second] = basePiece.copy(powerUp = p.third)
            }

            _uiState.update { it.copy(boardState = BoardState.DROPPING) }
            applyGravity()
            syncUI()
            delay(300) // Drop anim

            matches = findMatches()
        }

        // Check for deadlock
        if (!hasPossibleMoves()) {
            shuffleBoard()
        }

        checkGameOver()
        _uiState.update { it.copy(boardState = BoardState.IDLE, lastMatchCombo = 0) }
    }

    private fun determinePowerUp(matchSet: Set<Pair<Int, Int>>): GamePiecePowerUp {
        val hLines = matchSet.groupBy { it.first }.values.filter { it.size >= 3 }
        val vLines = matchSet.groupBy { it.second }.values.filter { it.size >= 3 }

        // Intersection (Explosive Bomb)
        if (hLines.isNotEmpty() && vLines.isNotEmpty()) return GamePiecePowerUp.EXPLOSIVE_BOMB

        // 5-line (Color Bomb)
        if (hLines.any { it.size >= 5 } || vLines.any { it.size >= 5 }) return GamePiecePowerUp.COLOR_BOMB

        // 4-line (Line Blaster)
        if (hLines.any { it.size >= 4 } || vLines.any { it.size >= 4 }) return GamePiecePowerUp.LINE_BLASTER

        return GamePiecePowerUp.NONE
    }

    private fun applyGravity() {
        for (c in 0 until 8) {
            val column = mutableListOf<GamePiece>()
            for (r in 7 downTo 0) {
                if (!logicalGrid[r][c].id.startsWith("EMPTY")) {
                    column.add(logicalGrid[r][c])
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

    private fun findMatches(): List<Set<Pair<Int, Int>>> {
        val horizontalMatches = mutableListOf<Set<Pair<Int, Int>>>()
        for (r in 0 until 8) {
            var c = 0
            while (c < 8) {
                var count = 1
                while (c + count < 8 && 
                    logicalGrid[r][c].id != "EMPTY" &&
                    logicalGrid[r][c + count].id != "EMPTY" && 
                    logicalGrid[r][c + count].type == logicalGrid[r][c].type) {
                    count++
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
                var count = 1
                while (r + count < 8 && 
                    logicalGrid[r][c].id != "EMPTY" &&
                    logicalGrid[r + count][c].id != "EMPTY" && 
                    logicalGrid[r + count][c].type == logicalGrid[r][c].type) {
                    count++
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
                // Try Right
                if (c + 1 < 8) {
                    if (checkSwapForMatch(r, c, r, c + 1)) return true
                }
                // Try Down
                if (r + 1 < 8) {
                    if (checkSwapForMatch(r, c, r + 1, c)) return true
                }
            }
        }
        return false
    }

    private fun checkSwapForMatch(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        // Temp swap
        val temp = logicalGrid[r1][c1]
        logicalGrid[r1][c1] = logicalGrid[r2][c2]
        logicalGrid[r2][c2] = temp
        
        val hasMatch = findMatches().isNotEmpty()
        
        // Swap back
        val tempBack = logicalGrid[r1][c1]
        logicalGrid[r1][c1] = logicalGrid[r2][c2]
        logicalGrid[r2][c2] = tempBack
        
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
            // Ensure no starting matches after shuffle
        } while (findMatches().isNotEmpty() || !hasPossibleMoves())
        
        syncUI()
    }

    private fun checkGameOver() {
        val currentState = _uiState.value
        if (currentState.score >= currentState.targetScore) {
            viewModelScope.launch {
                playerRepository.levelUp()
                playerRepository.addCoins(50)
            }
            _uiState.update {
                it.copy(
                    isGameWon = true,
                    isGameOver = true,
                    boardState = BoardState.IDLE,
                    cinthiaState = CinthiaState.CHEERING
                )
            }
        } else if (currentState.movesLeft <= 0) {
            _uiState.update { 
                it.copy(
                    isGameOver = true,
                    cinthiaState = CinthiaState.SAD
                ) 
            }
        } else if (currentState.movesLeft < 5) {
            _uiState.update { it.copy(cinthiaState = CinthiaState.WORRIED) }
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
    }

    fun onBoosterSelected(type: BoosterType) {
        if (_uiState.value.boardState != BoardState.IDLE && _uiState.value.boardState != BoardState.WAITING_FOR_BOOSTER_TARGET) return
        if (_uiState.value.isGameOver) return

        val currentState = _uiState.value
        if (currentState.activeBooster == type) {
            // Cancel selection
            _uiState.update { it.copy(
                activeBooster = null,
                boardState = BoardState.IDLE
            ) }
            return
        }

        // If another booster is active, cancel it first
        if (currentState.activeBooster != null) {
            _uiState.update { it.copy(
                activeBooster = null,
                boardState = BoardState.IDLE
            ) }
        }

        val count = _uiState.value.boosterInventory[type] ?: 0
        if (count > 0) {
            if (type == BoosterType.SHUFFLE) {
                viewModelScope.launch {
                    playerRepository.updateBoosterCount(BoosterType.SHUFFLE, -1)
                    shuffleBoard()
                    resolveMatches(findMatches())
                }
            } else {
                _uiState.update { it.copy(
                    activeBooster = type,
                    boardState = BoardState.WAITING_FOR_BOOSTER_TARGET
                ) }
            }
        }
    }

    fun onPieceTapped(row: Int, col: Int) {
        val state = _uiState.value
        if (state.isGameWon) return
        if (state.boardState != BoardState.WAITING_FOR_BOOSTER_TARGET || state.activeBooster == null) return

        viewModelScope.launch {
            val booster = state.activeBooster
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
                if (booster != null) {
                    playerRepository.updateBoosterCount(booster, -1)
                }
                _effects.emit(GameEffect.MatchHaptic)
                _effects.emit(GameEffect.ScreenShake)
                destroyPieces(affected)
            }
        }
    }

    private suspend fun destroyPieces(coords: Set<Pair<Int, Int>>) {
        _uiState.update { it.copy(matchParticles = coords) }
        delay(200)
        _uiState.update { it.copy(matchParticles = emptySet()) }

        for (coord in coords) {
            logicalGrid[coord.first][coord.second] = GamePiece(
                type = GamePieceType.entries.random(),
                id = "EMPTY_${java.util.UUID.randomUUID()}"
            )
        }
        
        _uiState.update { it.copy(boardState = BoardState.DROPPING) }
        applyGravity()
        syncUI()
        delay(300)
        
        val matches = findMatches()
        if (matches.isNotEmpty()) {
            resolveMatches(matches)
        } else {
            _uiState.update { it.copy(boardState = BoardState.IDLE) }
        }
    }

    class Factory(private val playerRepository: PlayerRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return GameViewModel(playerRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
