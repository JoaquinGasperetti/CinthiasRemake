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
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.LinearEasing
import com.hkemtrentainment.cinthiasquestr.R
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.core.animateValue
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.asImageBitmap

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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight
        val currentDensity = LocalDensity.current
        val totalHeightPx = with(currentDensity) { totalHeight.toPx() }

        Image(
            painter = painterResource(id = R.drawable.bg_game),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        val cinthiaSizeDp = totalHeight * 0.22f
        val maxCinthiaWidth = totalWidth * 0.65f
        val finalCinthiaSize = minOf(cinthiaSizeDp, maxCinthiaWidth)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { 
                    IntOffset(
                        x = 0,
                        y = (totalHeightPx * 0.333f - with(currentDensity) { finalCinthiaSize.toPx() }).roundToInt()
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            CinthiaCharacter(state = gameState.cinthiaState, size = finalCinthiaSize)
        }

        Scaffold(
            containerColor = Color.Transparent,
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
                        .background(Color.Transparent)
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
            val bottomBarTopHeight = totalHeight - innerPadding.calculateBottomPadding()
            
            Box(modifier = Modifier.fillMaxSize()) {
                val boardPadding = 24.dp
                val maxBoardWidth = totalWidth - boardPadding * 2
                val maxBoardHeight = totalHeight * 0.65f
                var boardSize = minOf(maxBoardWidth, maxBoardHeight)
                
                val preferredBoardTop = totalHeight * 0.385f
                val safeBottomLimit = bottomBarTopHeight - 16.dp
                if (preferredBoardTop + boardSize > safeBottomLimit) {
                    boardSize = safeBottomLimit - preferredBoardTop
                }

                val boardInset = boardSize * 0.03f
                val cellSize = (boardSize - boardInset * 2f) / 8
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
                        .align(Alignment.TopCenter)
                        .offset { 
                            IntOffset(
                                x = 0,
                                y = preferredBoardTop.roundToPx()
                            )
                        }
                        .zIndex(2f)
                        .graphicsLayer {
                            translationX = shakeOffset.value.x
                            translationY = shakeOffset.value.y
                        }
                        .then(
                            if (isSelectingBoosterTarget) {
                                Modifier.border(4.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                            } else Modifier
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.board_frame),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    val boardCellBitmap = androidx.compose.ui.graphics.ImageBitmap.imageResource(id = R.drawable.board_cell)
                    
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(boardInset)) {
                        val totalW = size.width
                        val totalH = size.height
                        
                        drawRect(color = Color(0xFF4E342E), size = size)
                        
                        for (r in 0 until 8) {
                            for (c in 0 until 8) {
                                val left = (c * totalW / 8f).roundToInt()
                                val top = (r * totalH / 8f).roundToInt()
                                val right = ((c + 1) * totalW / 8f).roundToInt()
                                val bottom = ((r + 1) * totalH / 8f).roundToInt()
                                
                                drawImage(
                                    image = boardCellBitmap,
                                    dstOffset = IntOffset(left, top),
                                    dstSize = androidx.compose.ui.unit.IntSize(right - left, bottom - top)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize().padding(boardInset)) {
                        val totalW = with(LocalDensity.current) { (boardSize - boardInset * 2f).toPx() }
                        val totalH = totalW

                        for (r in 0 until 8) {
                            for (c in 0 until 8) {
                                val left = (c * totalW / 8f).roundToInt()
                                val top = (r * totalH / 8f).roundToInt()
                                val right = ((c + 1) * totalW / 8f).roundToInt()
                                val bottom = ((r + 1) * totalH / 8f).roundToInt()
                                val cellWidth = with(LocalDensity.current) { (right - left).toDp() }
                                val cellHeight = with(LocalDensity.current) { (bottom - top).toDp() }

                                val piece = gameState.grid.getOrNull(r)?.getOrNull(c)
                            if (piece != null && !piece.isEmpty) {
                                val isMatched = gameState.matchParticles.contains(r to c)
                                key(piece.id) {
                                    val targetOffset = IntOffset(left, top)
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
                                            .size(cellWidth, cellHeight)
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
                    } // closes padding Box
                } // closes board container Box
            } // closes board outer Box
        } // closes Scaffold content

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
}

@Composable
fun CinthiaCharacter(state: CinthiaState, size: androidx.compose.ui.unit.Dp) {
    val frames = remember {
        listOf(
            R.drawable.cinthia_idle_01, R.drawable.cinthia_idle_02,
            R.drawable.cinthia_idle_03, R.drawable.cinthia_idle_04,
            R.drawable.cinthia_idle_05, R.drawable.cinthia_idle_06,
            R.drawable.cinthia_idle_07, R.drawable.cinthia_idle_08,
            R.drawable.cinthia_idle_09, R.drawable.cinthia_idle_10,
            R.drawable.cinthia_idle_11, R.drawable.cinthia_idle_12
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cinthia_anim")
    val frameIndex by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = 12,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(12 * 120, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "frame"
    )

    val currentFrame = if (state == CinthiaState.SAD) frames[0] else frames[frameIndex.coerceIn(0, frames.lastIndex)]

    val jump by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (state == CinthiaState.CHEERING) -20f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jump"
    )

    val shake by infiniteTransition.animateFloat(
        initialValue = if (state == CinthiaState.WORRIED) -5f else 0f,
        targetValue = if (state == CinthiaState.WORRIED) 5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    val colorMatrix = remember(state) {
        if (state == CinthiaState.SAD) {
            ColorMatrix().apply { setToSaturation(0f) }
        } else {
            ColorMatrix()
        }
    }

    Image(
        painter = painterResource(id = currentFrame),
        contentDescription = "Cinthia",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(size)
            .offset(x = shake.dp, y = jump.dp),
        colorFilter = ColorFilter.colorMatrix(colorMatrix)
    )
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
            .background(Color.Black.copy(alpha = 0.35f)),
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
