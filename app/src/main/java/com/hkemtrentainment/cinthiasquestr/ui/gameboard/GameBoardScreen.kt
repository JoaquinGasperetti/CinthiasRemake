package com.hkemtrentainment.cinthiasquestr.ui.gameboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkemtrentainment.cinthiasquestr.CinthiasQuestApp
import com.hkemtrentainment.cinthiasquestr.data.WorldRepository
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.components.GameTopBar
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.components.OutOfMovesDialog
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.components.PauseDialog
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.components.VictoryDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun GameBoardScreen(
    selectedLevel: Int = 1,
    onPauseClick: () -> Unit,
    onNextLevelClick: (Int) -> Unit = {},
    onWorldMapClick: () -> Unit = onPauseClick,
    viewModel: GameViewModel = viewModel(
        factory = GameViewModel.Factory(
            playerRepository = (LocalContext.current.applicationContext as CinthiasQuestApp).playerRepository,
            selectedLevel = selectedLevel
        )
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
                            Color(0xFF3E2723),
                            Color(0xFF1A1A1A)
                        )
                    )
                )
        ) {
            val boardPadding = 24.dp
            val maxBoardWidth = maxWidth - boardPadding * 2
            val maxBoardHeight = maxHeight * 0.65f
            val boardSize = minOf(maxBoardWidth, maxBoardHeight)
            val cellSize = (boardSize - 24.dp) / 8
            val cellSizePx = with(LocalDensity.current) { cellSize.toPx() }
            val isSelectingBoosterTarget = gameState.boardState == BoardState.WAITING_FOR_BOOSTER_TARGET

            // Capa de oscurecimiento DETRÁS del tablero (zIndex = 1f) para resaltar las fichas
            if (isSelectingBoosterTarget) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { viewModel.cancelActiveBooster() }
                ) {
                    Surface(
                        color = Color(0xFF3E2723),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 20.dp, start = 16.dp, end = 16.dp)
                            .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "TOCA UNA FICHA PARA USAR ${gameState.activeBooster?.displayName?.uppercase() ?: ""}",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.ExtraBold
                            )
                            TextButton(onClick = { viewModel.cancelActiveBooster() }) {
                                Text(
                                    text = "Cancelar",
                                    color = Color(0xFFF5E6CE),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // 1. Personaje Cinthia
                CinthiaCharacter(state = gameState.cinthiaState)

                Spacer(modifier = Modifier.weight(1f))

                var activeDragCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
                var dragOffsetX by remember { mutableFloatStateOf(0f) }
                var dragOffsetY by remember { mutableFloatStateOf(0f) }
                var isHorizontalDrag by remember { mutableStateOf<Boolean?>(null) }

                val animatedDragX by animateFloatAsState(
                    targetValue = dragOffsetX,
                    animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
                    label = "dragX"
                )
                val animatedDragY by animateFloatAsState(
                    targetValue = dragOffsetY,
                    animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
                    label = "dragY"
                )

                // 2. Contenedor del Tablero 8x8 (iluminado si hay un booster activo)
                Box(
                    modifier = Modifier
                        .size(boardSize)
                        .graphicsLayer {
                            translationX = shakeOffset.value.x
                            translationY = shakeOffset.value.y
                        }
                        .background(Color(0xFF2C1B18), shape = RoundedCornerShape(16.dp))
                        .border(
                            width = if (isSelectingBoosterTarget) 4.dp else 4.dp,
                            color = if (isSelectingBoosterTarget) Color(0xFFFFD700) else Color(0xFF8D6E63),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {
                    for (r in 0 until 8) {
                        for (c in 0 until 8) {
                            val piece = gameState.grid.getOrNull(r)?.getOrNull(c)
                            if (piece != null && !piece.isEmpty) {
                                val isMatched = gameState.matchParticles.contains(r to c)
                                key(piece.id) {
                                    val targetOffset = IntOffset(
                                        x = (c * cellSizePx).roundToInt(),
                                        y = (r * cellSizePx).roundToInt()
                                    )
                                    val animatedOffset by animateIntOffsetAsState(
                                        targetValue = targetOffset,
                                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                                        label = "pieceOffset"
                                    )

                                    val (dragR, dragC) = activeDragCell ?: (-1 to -1)
                                    val isCurrentDragged = (dragR == r && dragC == c)

                                    val currentDragX: Float
                                    val currentDragY: Float

                                    if (isCurrentDragged) {
                                        currentDragX = animatedDragX
                                        currentDragY = animatedDragY
                                    } else if (activeDragCell != null) {
                                        if (isHorizontalDrag == true && dragR == r) {
                                            if (animatedDragX > 0 && c == dragC + 1) {
                                                currentDragX = -animatedDragX
                                                currentDragY = 0f
                                            } else if (animatedDragX < 0 && c == dragC - 1) {
                                                currentDragX = -animatedDragX
                                                currentDragY = 0f
                                            } else {
                                                currentDragX = 0f
                                                currentDragY = 0f
                                            }
                                        } else if (isHorizontalDrag == false && dragC == c) {
                                            if (animatedDragY > 0 && r == dragR + 1) {
                                                currentDragX = 0f
                                                currentDragY = -animatedDragY
                                            } else if (animatedDragY < 0 && r == dragR - 1) {
                                                currentDragX = 0f
                                                currentDragY = -animatedDragY
                                            } else {
                                                currentDragX = 0f
                                                currentDragY = 0f
                                            }
                                        } else {
                                            currentDragX = 0f
                                            currentDragY = 0f
                                        }
                                    } else {
                                        currentDragX = 0f
                                        currentDragY = 0f
                                    }

                                    val renderOffset = IntOffset(
                                        x = animatedOffset.x + currentDragX.roundToInt(),
                                        y = animatedOffset.y + currentDragY.roundToInt()
                                    )

                                    GamePieceItem(
                                        piece = piece,
                                        isMatched = isMatched,
                                        modifier = Modifier
                                            .offset { renderOffset }
                                            .size(cellSize)
                                            .pointerInput(r, c, isSelectingBoosterTarget) {
                                                detectTapGestures {
                                                    viewModel.onPieceTapped(r, c)
                                                }
                                            }
                                            .pointerInput(r, c, isSelectingBoosterTarget) {
                                                if (isSelectingBoosterTarget) return@pointerInput
                                                detectDragGestures(
                                                    onDragStart = {
                                                        activeDragCell = r to c
                                                        dragOffsetX = 0f
                                                        dragOffsetY = 0f
                                                        isHorizontalDrag = null
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        if (activeDragCell == r to c) {
                                                            val newX = dragOffsetX + dragAmount.x
                                                            val newY = dragOffsetY + dragAmount.y

                                                            if (isHorizontalDrag == null) {
                                                                if (abs(newX) > 6f || abs(newY) > 6f) {
                                                                    isHorizontalDrag = abs(newX) >= abs(newY)
                                                                }
                                                            }

                                                            if (isHorizontalDrag == true) {
                                                                dragOffsetX = newX.coerceIn(-cellSizePx, cellSizePx)
                                                                dragOffsetY = 0f
                                                            } else if (isHorizontalDrag == false) {
                                                                dragOffsetX = 0f
                                                                dragOffsetY = newY.coerceIn(-cellSizePx, cellSizePx)
                                                            }
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        if (activeDragCell == r to c) {
                                                            val threshold = cellSizePx * 0.3f
                                                            val finalIsHorizontal = isHorizontalDrag

                                                            activeDragCell = null
                                                            isHorizontalDrag = null

                                                            if (finalIsHorizontal == true) {
                                                                if (abs(dragOffsetX) > threshold) {
                                                                    if (dragOffsetX > 0) {
                                                                        viewModel.onSwipe(r, c, SwipeDirection.RIGHT)
                                                                    } else {
                                                                        viewModel.onSwipe(r, c, SwipeDirection.LEFT)
                                                                    }
                                                                }
                                                            } else if (finalIsHorizontal == false) {
                                                                if (abs(dragOffsetY) > threshold) {
                                                                    if (dragOffsetY > 0) {
                                                                        viewModel.onSwipe(r, c, SwipeDirection.DOWN)
                                                                    } else {
                                                                        viewModel.onSwipe(r, c, SwipeDirection.UP)
                                                                    }
                                                                }
                                                            }

                                                            dragOffsetX = 0f
                                                            dragOffsetY = 0f
                                                        }
                                                    },
                                                    onDragCancel = {
                                                        activeDragCell = null
                                                        isHorizontalDrag = null
                                                        dragOffsetX = 0f
                                                        dragOffsetY = 0f
                                                    }
                                                )
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }

        if (gameState.isGameWon) {
            VictoryDialog(
                score = gameState.score,
                stars = gameState.earnedStars,
                earnedCoins = gameState.earnedCoinsReward,
                doubleCoinsClaimed = gameState.doubleCoinsClaimed,
                hasNextLevel = gameState.level < WorldRepository.MAX_GAME_LEVELS,
                onWatchAdClick = { viewModel.claimDoubleCoinsReward() },
                onNextLevelClick = { onNextLevelClick(gameState.level + 1) },
                onWorldMapClick = onWorldMapClick
            )
        } else if (gameState.isGameOver) {
            OutOfMovesDialog(
                onWatchAdClick = { viewModel.addRewardMoves(5) },
                onQuitClick = onWorldMapClick
            )
        }

        if (showPauseDialog) {
            PauseDialog(
                onResumeClick = { showPauseDialog = false },
                onQuitClick = onWorldMapClick
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
                .size(110.dp)
                .shadow(12.dp, CircleShape)
                .background(Color(0xFF8D6E63), CircleShape)
                .padding(16.dp),
            colorFilter = ColorFilter.tint(Color(0xFFFFE0B2))
        )
    }
}

@Composable
fun BoosterBar(
    inventory: Map<BoosterType, Int>,
    activeBooster: BoosterType?,
    onBoosterClick: (BoosterType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoosterItem(
            type = BoosterType.HAMMER,
            count = inventory[BoosterType.HAMMER] ?: 0,
            isActive = activeBooster == BoosterType.HAMMER,
            icon = Icons.Default.Build,
            onClick = { onBoosterClick(BoosterType.HAMMER) }
        )
        BoosterItem(
            type = BoosterType.DYNAMITE,
            count = inventory[BoosterType.DYNAMITE] ?: 0,
            isActive = activeBooster == BoosterType.DYNAMITE,
            icon = Icons.Default.Clear,
            onClick = { onBoosterClick(BoosterType.DYNAMITE) }
        )
        BoosterItem(
            type = BoosterType.SHUFFLE,
            count = inventory[BoosterType.SHUFFLE] ?: 0,
            isActive = activeBooster == BoosterType.SHUFFLE,
            icon = Icons.Default.Refresh,
            onClick = { onBoosterClick(BoosterType.SHUFFLE) }
        )
    }
}

@Composable
fun BoosterItem(
    type: BoosterType,
    count: Int,
    isActive: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isActive) 1.15f else 1.0f, label = "boosterScale")

    Box(
        modifier = Modifier
            .scale(scale)
            .size(56.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(
                if (isActive) Color(0xFFFFD700) else Color(0xFF3E2723),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                2.dp,
                if (isActive) Color.White else Color(0xFF8D6E63),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = type.displayName,
            tint = if (isActive) Color(0xFF3E2723) else Color(0xFFF5E6CE),
            modifier = Modifier.size(28.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .size(20.dp)
                .background(Color(0xFFE53935), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$count",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AdMobPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color(0xFF2C1B18)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Espacio Publicitario (AdMob)",
            color = Color(0xFF8D6E63),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
