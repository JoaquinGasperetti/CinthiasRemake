package com.hkentertainment.cinthiasquestr.ui.gameboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkentertainment.cinthiasquestr.ui.gameboard.components.GameTopBar
import com.hkentertainment.cinthiasquestr.ui.gameboard.components.OutOfMovesDialog
import com.hkentertainment.cinthiasquestr.ui.gameboard.components.PauseDialog
import com.hkentertainment.cinthiasquestr.ui.gameboard.components.VictoryDialog
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkentertainment.cinthiasquestr.CinthiasQuestApp
import com.hkentertainment.cinthiasquestr.ui.theme.CinthiasQuestTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun GameBoardScreen(
    onPauseClick: () -> Unit,
    viewModel: GameViewModel = viewModel(
        factory = GameViewModel.Factory((LocalContext.current.applicationContext as CinthiasQuestApp).playerRepository)
    )
) {
    val gameState by viewModel.gameState.collectAsState()
    var showPauseDialog by remember { mutableStateOf(false) }
    
    val haptic = LocalHapticFeedback.current
    val shakeOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is GameEffect.SwipeHaptic -> {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                is GameEffect.MatchHaptic -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                is GameEffect.ComboHaptic -> {
                    // Staggered pattern for chain combos
                    repeat(3) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        delay(50)
                    }
                }
                is GameEffect.ScreenShake -> {
                    coroutineScope.launch {
                        repeat(5) {
                            val randomOffset = Offset(
                                x = Random.nextInt(-10, 10).toFloat(),
                                y = Random.nextInt(-10, 10).toFloat()
                            )
                            shakeOffset.animateTo(randomOffset, tween(50))
                        }
                        shakeOffset.animateTo(Offset.Zero, spring())
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            GameTopBar(
                score = gameState.score,
                targetScore = gameState.targetScore,
                movesLeft = gameState.movesLeft,
                level = gameState.level,
                coins = gameState.coins,
                onPauseClick = { showPauseDialog = true }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BoosterBar(
                    inventory = gameState.boosterInventory,
                    activeBooster = gameState.activeBooster,
                    onBoosterClick = { viewModel.onBoosterSelected(it) }
                )
                AdMobPlaceholder()
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF3E2723), // Medieval Dark Brown
                            Color(0xFF1A1A1A)  // Deep Shadow
                        )
                    )
                )
        ) {
            val boardPadding = 48.dp // Increased padding for future frame
            val maxBoardWidth = maxWidth - boardPadding * 2
            val maxBoardHeight = maxHeight * 0.65f // Slightly more room for character
            val boardSize = minOf(maxBoardWidth, maxBoardHeight)
            val cellSize = (boardSize - 24.dp) / 8 // Adjusted for border/padding

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Character Area
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CinthiaCharacter(state = gameState.cinthiaState)
                }

                // 2. Game Board Area
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(boardSize)
                        .offset {
                            IntOffset(shakeOffset.value.x.roundToInt(), shakeOffset.value.y.roundToInt())
                        }
                ) {
                    GameBoard(
                        grid = gameState.grid,
                        onSwipe = { r, c, dir -> viewModel.onSwipe(r, c, dir) },
                        onPieceTapped = { r, c -> viewModel.onPieceTapped(r, c) },
                        cellSize = cellSize
                    )

                    if (gameState.boardState == BoardState.SHUFFLING) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.matchParentSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "SHUFFLING...",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    ParticleExplosionOverlay(
                        particles = gameState.matchParticles,
                        cellSize = cellSize
                    )

                    ComboAnimation(combo = gameState.lastMatchCombo)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. Dimming Layer (Only visible when selecting booster target)
            if (gameState.boardState == BoardState.WAITING_FOR_BOOSTER_TARGET) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Text(
                        "SELECT TARGET",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 40.dp)
                    )
                }
            }
        }

        if (gameState.isGameWon) {
            VictoryDialog(
                score = gameState.score,
                onWatchAdClick = {
                    // TODO: Implement double coins reward
                },
                onNextLevelClick = onPauseClick
            )
        } else if (gameState.isGameOver) {
            OutOfMovesDialog(
                onWatchAdClick = { viewModel.addRewardMoves(5) },
                onQuitClick = onPauseClick
            )
        }

        if (showPauseDialog) {
            PauseDialog(
                onResumeClick = { showPauseDialog = false },
                onQuitClick = onPauseClick
            )
        }
    }
}

@Composable
fun CinthiaCharacter(state: CinthiaState) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        // Character Image Placeholder - Ready for PNG
        Image(
            painter = rememberVectorPainter(
                image = when (state) {
                    CinthiaState.IDLE -> Icons.Default.Face
                    CinthiaState.CHEERING -> Icons.Default.Star
                    CinthiaState.WORRIED -> Icons.Default.Warning
                    CinthiaState.SAD -> Icons.Default.SentimentVeryDissatisfied
                }
            ),
            contentDescription = "Cinthia",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(120.dp)
                .shadow(12.dp, CircleShape)
                .background(Color(0xFFF5E6CE).copy(alpha = 0.2f), CircleShape)
                .border(2.dp, Color(0xFF8D6E63), CircleShape),
            colorFilter = ColorFilter.tint(Color(0xFFF5E6CE))
        )
        
        Text(
            text = when (state) {
                CinthiaState.IDLE -> "Cinthia: Ready!"
                CinthiaState.CHEERING -> "Cinthia: Amazing!"
                CinthiaState.WORRIED -> "Cinthia: Oh no..."
                CinthiaState.SAD -> "Cinthia: We'll win next time!"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF5E6CE),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun GameBoard(
    grid: List<List<GamePiece>>,
    onSwipe: (Int, Int, SwipeDirection) -> Unit,
    onPieceTapped: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    cellSize: androidx.compose.ui.unit.Dp = 45.dp
) {
    val boardSize = cellSize * 8
    
    Box(
        modifier = modifier
            .size(boardSize + 24.dp)
            .padding(12.dp)
            .shadow(16.dp, RoundedCornerShape(12.dp))
            .background(Color(0xFF2B1B17), RoundedCornerShape(12.dp)) // Dark Dungeon Stone
            .border(4.dp, Color(0xFF8D6E63), RoundedCornerShape(12.dp)) // Wood Frame
    ) {
        // Flatten the grid into a list of pieces with their (row, col) coordinates.
        // This ensures that when we use key(piece.id), all pieces are siblings in the composition.
        // If we used nested loops, pieces moving between rows (vertical swap/gravity) would be
        // seen as "removed from one row and added to another", resetting their animation state.
        val flatGrid = remember(grid) {
            grid.flatMapIndexed { r, row ->
                row.mapIndexed { c, piece -> Triple(r, c, piece) }
            }
        }

        flatGrid.forEach { (rowIndex, colIndex, piece) ->
            key(piece.id) {
                AnimatedGamePieceItem(
                    piece = piece,
                    rowIndex = rowIndex,
                    colIndex = colIndex,
                    onSwipe = onSwipe,
                    onPieceTapped = onPieceTapped,
                    cellSize = cellSize
                )
            }
        }
    }
}

@Composable
fun AnimatedGamePieceItem(
    piece: GamePiece,
    rowIndex: Int,
    colIndex: Int,
    onSwipe: (Int, Int, SwipeDirection) -> Unit,
    onPieceTapped: (Int, Int) -> Unit,
    cellSize: androidx.compose.ui.unit.Dp
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val targetOffset = with(density) {
        IntOffset(
            x = (colIndex * cellSize.toPx()).roundToInt(),
            y = (rowIndex * cellSize.toPx()).roundToInt()
        )
    }

    val spawnOffset = with(density) { (cellSize.toPx() * 8).roundToInt() }

    // Use Animatable for finer control over the cascade and swap animations
    val animatedOffset = remember(piece.id) {
        Animatable(
            // If it's a new piece, start it from above the board for the "sliding down" effect
            // We use targetOffset.y - boardHeight to make it fall from exactly above the board
            targetOffset.copy(y = targetOffset.y - spawnOffset),
            IntOffset.VectorConverter
        )
    }

    LaunchedEffect(targetOffset) {
        animatedOffset.animateTo(
            targetValue = targetOffset,
            animationSpec = tween(durationMillis = 250, easing = LinearOutSlowInEasing)
        )
    }

    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .offset { animatedOffset.value }
            .size(cellSize)
            .padding(2.dp)
            .pointerInput(rowIndex, colIndex) {
                detectTapGestures { onPieceTapped(rowIndex, colIndex) }
            }
            .pointerInput(rowIndex, colIndex) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetX += dragAmount.x
                        dragOffsetY += dragAmount.y
                    },
                    onDragEnd = {
                        val threshold = 50
                        val direction = when {
                            abs(dragOffsetX) > abs(dragOffsetY) -> {
                                if (dragOffsetX > threshold) SwipeDirection.RIGHT
                                else if (dragOffsetX < -threshold) SwipeDirection.LEFT
                                else null
                            }
                            abs(dragOffsetY) > abs(dragOffsetX) -> {
                                if (dragOffsetY > threshold) SwipeDirection.DOWN
                                else if (dragOffsetY < -threshold) SwipeDirection.UP
                                else null
                            }
                            else -> null
                        }
                        
                        if (direction != null) {
                            onSwipe(rowIndex, colIndex, direction)
                        }
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                    },
                    onDragCancel = {
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                    }
                )
            }
    ) {
        GemPiece(piece = piece)
    }
}

@Composable
fun GemPiece(piece: GamePiece) {
    val type = piece.type
    val powerUp = piece.powerUp
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        // Piece Image Placeholder - Ready for PNGs
        Image(
            painter = rememberVectorPainter(
                image = when (powerUp) {
                    GamePiecePowerUp.LINE_BLASTER -> Icons.Default.FlashOn
                    GamePiecePowerUp.COLOR_BOMB -> Icons.Default.BrightnessHigh
                    GamePiecePowerUp.EXPLOSIVE_BOMB -> Icons.Default.Whatshot
                    GamePiecePowerUp.NONE -> Icons.Default.Diamond
                }
            ),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(type.color.copy(alpha = 0.8f), type.color)
                    ),
                    RoundedCornerShape(8.dp)
                )
                .border(
                    width = if (powerUp != GamePiecePowerUp.NONE) 2.dp else 1.dp,
                    color = if (powerUp != GamePiecePowerUp.NONE) Color.White else Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ),
            colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.6f))
        )
    }
}

@Composable
fun ComboAnimation(combo: Int) {
    if (combo > 1) {
        var visible by remember { mutableStateOf(true) }
        LaunchedEffect(combo) {
            visible = true
            delay(1000)
            visible = false
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = "Combo x$combo!",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .shadow(4.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun BoosterBar(
    inventory: Map<BoosterType, Int>,
    activeBooster: BoosterType?,
    onBoosterClick: (BoosterType) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .padding(bottom = 16.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        BoosterType.entries.forEach { type ->
            val count = inventory[type] ?: 0
            val isActive = activeBooster == type
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BadgedBox(
                    badge = {
                        if (count > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ) {
                                Text(count.toString())
                            }
                        } else {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.White
                            ) {
                                Text("Ad", fontSize = 10.sp)
                            }
                        }
                    }
                ) {
                    IconButton(
                        onClick = { onBoosterClick(type) },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.secondaryContainer,
                                CircleShape
                            )
                            .border(
                                width = if (isActive) 3.dp else 1.dp,
                                color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = when (type) {
                                BoosterType.HAMMER -> Icons.Default.Build
                                BoosterType.DYNAMITE -> Icons.Default.Whatshot
                                BoosterType.SHUFFLE -> Icons.Default.Shuffle
                            },
                            contentDescription = type.name,
                            tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer 
                                   else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                Text(
                    text = type.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ParticleExplosionOverlay(
    particles: Set<Pair<Int, Int>>,
    cellSize: androidx.compose.ui.unit.Dp
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    Box(modifier = Modifier.fillMaxSize()) {
        particles.forEach { (row, col) ->
            key("${row}-${col}") {
                val x = with(density) { (col * cellSize.toPx()) + (cellSize.toPx() / 2) }
                val y = with(density) { (row * cellSize.toPx()) + (cellSize.toPx() / 2) }
                
                var animationTriggered by remember { mutableStateOf(false) }
                val radiusScale = animateFloatAsState(
                    targetValue = if (animationTriggered) 1.5f else 0f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "particle_radius"
                )
                val alpha = animateFloatAsState(
                    targetValue = if (animationTriggered) 0f else 1f,
                    animationSpec = tween(300),
                    label = "particle_alpha"
                )

                LaunchedEffect(Unit) {
                    animationTriggered = true
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = alpha.value),
                        radius = (cellSize.toPx() / 2) * radiusScale.value,
                        center = Offset(x, y)
                    )
                    
                    // Multiple smaller circles for "particles"
                    repeat(6) { i ->
                        val angle = (i * 60f) * (Math.PI / 180f)
                        val distance = (cellSize.toPx() / 2) * radiusScale.value
                        drawCircle(
                            color = Color.White.copy(alpha = alpha.value),
                            radius = 4.dp.toPx(),
                            center = Offset(
                                x = x + (Math.cos(angle) * distance).toFloat(),
                                y = y + (Math.sin(angle) * distance).toFloat()
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdMobPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "AD BANNER PLACEHOLDER",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun GameBoardScreenPreview() {
    CinthiasQuestTheme {
        GameBoardScreen(onPauseClick = {})
    }
}
