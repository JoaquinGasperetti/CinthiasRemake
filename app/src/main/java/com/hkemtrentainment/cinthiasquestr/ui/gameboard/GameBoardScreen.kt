package com.hkemtrentainment.cinthiasquestr.ui.gameboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkemtrentainment.cinthiasquestr.CinthiasQuestApp
import com.hkemtrentainment.cinthiasquestr.R
import com.hkemtrentainment.cinthiasquestr.audio.GameSound
import com.hkemtrentainment.cinthiasquestr.data.WorldRepository
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.components.GameTopBar
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.components.OutOfMovesDialog
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.components.PauseDialog
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.components.VictoryDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Partícula física individual renderizada a 60fps cuando estallan gemas o bombas.
 */
private class VisualParticle(
    var xNorm: Float,
    var yNorm: Float,
    var vxNorm: Float,
    var vyNorm: Float,
    val color: Color,
    val radiusDp: Float,
    var alpha: Float = 1f,
    val decayRate: Float = 1.9f
)

/**
 * Onda expansiva o rayo láser de Line Blaster en el Canvas de efectos.
 */
private class VisualShockwave(
    val centerRow: Float,
    val centerCol: Float,
    val color: Color,
    var progress: Float = 0f,
    val speed: Float = 2.6f,
    val isFullBoard: Boolean = false
)

private class VisualLaserBeam(
    val isRow: Boolean,
    val index: Int,
    var alpha: Float = 1f
)

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

    val context = LocalContext.current
    val soundManager = remember(context) {
        (context.applicationContext as CinthiasQuestApp).soundManager
    }

    val haptic = LocalHapticFeedback.current
    val shakeOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val coroutineScope = rememberCoroutineScope()

    val activeParticles = remember { mutableStateListOf<VisualParticle>() }
    val activeShockwaves = remember { mutableStateListOf<VisualShockwave>() }
    val activeLaserBeams = remember { mutableStateListOf<VisualLaserBeam>() }
    var frameTickNanos by remember { mutableLongStateOf(0L) }

    // Bucle de simulación de partículas a 60fps cuando hay efectos activos
    LaunchedEffect(
        activeParticles.size,
        activeShockwaves.size,
        activeLaserBeams.size
    ) {
        if (activeParticles.isEmpty() && activeShockwaves.isEmpty() && activeLaserBeams.isEmpty()) return@LaunchedEffect
        var lastNanos = withFrameNanos { it }
        while (isActive && (activeParticles.isNotEmpty() || activeShockwaves.isNotEmpty() || activeLaserBeams.isNotEmpty())) {
            val now = withFrameNanos { it }
            val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            lastNanos = now
            frameTickNanos = now

            val pIter = activeParticles.listIterator()
            while (pIter.hasNext()) {
                val p = pIter.next()
                p.xNorm += p.vxNorm * dt
                p.yNorm += p.vyNorm * dt
                p.vyNorm += 1.45f * dt // Gravedad hacia abajo
                p.alpha -= p.decayRate * dt
                if (p.alpha <= 0f) {
                    pIter.remove()
                }
            }

            val sIter = activeShockwaves.listIterator()
            while (sIter.hasNext()) {
                val sw = sIter.next()
                sw.progress += sw.speed * dt
                if (sw.progress >= 1f) {
                    sIter.remove()
                }
            }

            val lIter = activeLaserBeams.listIterator()
            while (lIter.hasNext()) {
                val lb = lIter.next()
                lb.alpha -= 3.2f * dt
                if (lb.alpha <= 0f) {
                    lIter.remove()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is GameEffect.SwipeHaptic -> {
                    if (viewModel.gameState.value.hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
                is GameEffect.MatchHaptic -> {
                    if (viewModel.gameState.value.hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                is GameEffect.InvalidMoveHaptic -> {
                    if (viewModel.gameState.value.hapticsEnabled) {
                        coroutineScope.launch {
                            repeat(2) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                delay(45)
                            }
                        }
                    }
                }
                is GameEffect.ComboHaptic -> {
                    if (viewModel.gameState.value.hapticsEnabled) {
                        coroutineScope.launch {
                            repeat(effect.intensity.coerceIn(2, 4)) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                delay(55)
                            }
                        }
                    }
                }
                is GameEffect.ScreenShake -> {
                    if (!viewModel.gameState.value.reducedMotion) {
                        coroutineScope.launch {
                            val maxPx = effect.intensityPx.roundToInt().coerceAtLeast(2)
                            repeat(effect.cycles) { idx ->
                                val decay = 1f - (idx.toFloat() / (effect.cycles + 1))
                                val randomOffset = Offset(
                                    x = Random.nextInt(-maxPx, maxPx + 1) * decay,
                                    y = Random.nextInt(-maxPx, maxPx + 1) * decay
                                )
                                shakeOffset.animateTo(randomOffset, tween(38))
                            }
                            shakeOffset.animateTo(Offset.Zero, spring())
                        }
                    }
                }
                is GameEffect.SpawnParticles -> {
                    val event = effect.event
                    event.cellColors.forEach { (coord, colorHex) ->
                        val baseX = (coord.second + 0.5f) / 8f
                        val baseY = (coord.first + 0.5f) / 8f
                        val baseColor = Color(colorHex)
                        val countPerCell = if (event.isColorBombMega) 10 else 7
                        repeat(countPerCell) {
                            val angle = Random.nextFloat() * (Math.PI.toFloat() * 2f)
                            val speed = 0.22f + Random.nextFloat() * 0.55f
                            activeParticles.add(
                                VisualParticle(
                                    xNorm = baseX + (Random.nextFloat() - 0.5f) * 0.04f,
                                    yNorm = baseY + (Random.nextFloat() - 0.5f) * 0.04f,
                                    vxNorm = cos(angle) * speed,
                                    vyNorm = sin(angle) * speed - 0.25f,
                                    color = if (it % 3 == 0) Color.White else baseColor,
                                    radiusDp = 3.2f + Random.nextFloat() * 3.8f,
                                    decayRate = 1.6f + Random.nextFloat() * 0.8f
                                )
                            )
                        }
                    }
                    event.shockwaveCenters.forEach { (r, c) ->
                        activeShockwaves.add(
                            VisualShockwave(
                                centerRow = r + 0.5f,
                                centerCol = c + 0.5f,
                                color = Color(0xFFFFD700),
                                isFullBoard = event.isColorBombMega
                            )
                        )
                    }
                    event.lineBlasterRows.forEach { r ->
                        activeLaserBeams.add(VisualLaserBeam(isRow = true, index = r))
                    }
                    event.lineBlasterCols.forEach { c ->
                        activeLaserBeams.add(VisualLaserBeam(isRow = false, index = c))
                    }
                }
                is GameEffect.PlaySound -> {
                    if (effect.delayMs > 0L) {
                        coroutineScope.launch {
                            delay(effect.delayMs)
                            soundManager.play(
                                sound = effect.sound,
                                volume = effect.volume,
                                rate = effect.rate
                            )
                        }
                    } else {
                        soundManager.play(
                            sound = effect.sound,
                            volume = effect.volume,
                            rate = effect.rate
                        )
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
            CinthiaCharacter(
                state = gameState.cinthiaState,
                speechText = gameState.cinthiaSpeech,
                comboCount = gameState.lastMatchCombo,
                size = finalCinthiaSize
            )
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
                    winStreak = gameState.winStreak,
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

                // Banner celebratorio de Racha de Victorias al empezar el nivel
                AnimatedVisibility(
                    visible = gameState.streakBannerText != null,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = innerPadding.calculateTopPadding() + 6.dp)
                        .zIndex(5f)
                ) {
                    Surface(
                        color = Color(0xFF3E2723),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD700)),
                        shadowElevation = 8.dp
                    ) {
                        Text(
                            text = gameState.streakBannerText ?: "",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

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
                    animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
                    label = "dragX"
                )
                val animatedDragY by animateFloatAsState(
                    targetValue = dragOffsetY,
                    animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
                    label = "dragY"
                )

                // 2. Contenedor del Tablero 8x8 (con Screen Shake, Partículas y Popups Flotantes)
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

                    val boardCellBitmap = ImageBitmap.imageResource(id = R.drawable.board_cell)

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(boardInset)
                    ) {
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
                                    dstSize = IntSize(right - left, bottom - top)
                                )
                            }
                        }
                    }

                    // Grilla interactiva de gemas
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(boardInset)
                    ) {
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
                                    val coord = r to c
                                    val isMatched = gameState.matchParticles.contains(coord)
                                    val isSelected = gameState.selectedCell == coord
                                    val isHinted = gameState.hintCells.contains(coord)
                                    val isInvalidShake = gameState.invalidSwapCells.contains(coord)
                                    val isHitFlash = gameState.hitFlashCells.contains(coord)

                                    key(piece.id) {
                                        val targetOffset = IntOffset(left, top)
                                        val animatedOffset by animateIntOffsetAsState(
                                            targetValue = targetOffset,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
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
                                            isSelected = isSelected,
                                            isHinted = isHinted,
                                            isInvalidShake = isInvalidShake,
                                            isHitFlash = isHitFlash,
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
                                                            if (gameState.boardState == BoardState.IDLE && !gameState.isGameOver && !gameState.isGameWon) {
                                                                soundManager.play(GameSound.TILE_SELECT)
                                                            }
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
                                                                val threshold = cellSizePx * 0.26f
                                                                val finalIsHorizontal = isHorizontalDrag
                                                                var didSwipe = false

                                                                activeDragCell = null
                                                                isHorizontalDrag = null

                                                                if (finalIsHorizontal == true) {
                                                                    if (abs(dragOffsetX) > threshold) {
                                                                        didSwipe = true
                                                                        if (dragOffsetX > 0) {
                                                                            viewModel.onSwipe(r, c, SwipeDirection.RIGHT)
                                                                        } else {
                                                                            viewModel.onSwipe(r, c, SwipeDirection.LEFT)
                                                                        }
                                                                    }
                                                                } else if (finalIsHorizontal == false) {
                                                                    if (abs(dragOffsetY) > threshold) {
                                                                        didSwipe = true
                                                                        if (dragOffsetY > 0) {
                                                                            viewModel.onSwipe(r, c, SwipeDirection.DOWN)
                                                                        } else {
                                                                            viewModel.onSwipe(r, c, SwipeDirection.UP)
                                                                        }
                                                                    }
                                                                }

                                                                if (!didSwipe && gameState.boardState == BoardState.IDLE) {
                                                                    soundManager.play(GameSound.TILE_DESELECT)
                                                                }

                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                            }
                                                        },
                                                        onDragCancel = {
                                                            if (activeDragCell != null && gameState.boardState == BoardState.IDLE) {
                                                                soundManager.play(GameSound.TILE_DESELECT)
                                                            }
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

                        // Capa superior de Partículas, Ondas de Choque y Rayos de Line Blaster a 60fps
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(3f)
                        ) {
                            val tick = frameTickNanos // Suscripción al frame clock
                            if (tick >= 0L) {
                                val w = size.width
                                val h = size.height

                                // 1. Rayos láser de Line Blaster
                                activeLaserBeams.forEach { beam ->
                                    val a = beam.alpha.coerceIn(0f, 1f)
                                    if (beam.isRow) {
                                        val cy = (beam.index + 0.5f) * (h / 8f)
                                        val beamH = (h / 8f) * 0.55f * a
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color(0xFF80D8FF).copy(alpha = a * 0.85f),
                                                    Color.White.copy(alpha = a),
                                                    Color(0xFF80D8FF).copy(alpha = a * 0.85f),
                                                    Color.Transparent
                                                ),
                                                startY = cy - beamH,
                                                endY = cy + beamH
                                            ),
                                            topLeft = Offset(0f, cy - beamH),
                                            size = Size(w, beamH * 2f)
                                        )
                                    } else {
                                        val cx = (beam.index + 0.5f) * (w / 8f)
                                        val beamW = (w / 8f) * 0.55f * a
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color(0xFF80D8FF).copy(alpha = a * 0.85f),
                                                    Color.White.copy(alpha = a),
                                                    Color(0xFF80D8FF).copy(alpha = a * 0.85f),
                                                    Color.Transparent
                                                ),
                                                startX = cx - beamW,
                                                endX = cx + beamW
                                            ),
                                            topLeft = Offset(cx - beamW, 0f),
                                            size = Size(beamW * 2f, h)
                                        )
                                    }
                                }

                                // 2. Ondas expansivas circulares (Shockwaves)
                                activeShockwaves.forEach { sw ->
                                    val p = sw.progress.coerceIn(0f, 1f)
                                    val cx = (sw.centerCol / 8f) * w
                                    val cy = (sw.centerRow / 8f) * h
                                    val maxR = if (sw.isFullBoard) w * 0.75f else w * 0.28f
                                    val currentRadius = maxR * p
                                    val alpha = (1f - p).coerceIn(0f, 1f)
                                    drawCircle(
                                        color = sw.color.copy(alpha = alpha * 0.85f),
                                        radius = currentRadius,
                                        center = Offset(cx, cy),
                                        style = Stroke(width = (8.dp.toPx() * (1f - p * 0.6f)).coerceAtLeast(2f))
                                    )
                                }

                                // 3. Partículas radiales con gravedad
                                activeParticles.forEach { pt ->
                                    val a = pt.alpha.coerceIn(0f, 1f)
                                    val px = pt.xNorm * w
                                    val py = pt.yNorm * h
                                    val rPx = pt.radiusDp.dp.toPx() * (0.5f + 0.5f * a)
                                    drawCircle(
                                        color = pt.color.copy(alpha = a),
                                        radius = rPx,
                                        center = Offset(px, py)
                                    )
                                }
                            }
                        }

                        // Capa de Textos Flotantes de Puntos y Combos ("+120", "¡DIVINO! x4")
                        FloatingPopupsOverlay(
                            popups = gameState.floatingPopups,
                            boardInnerSizePx = totalW
                        )
                    } // closes padding Box
                } // closes board container Box
            } // closes board outer Box
        } // closes Scaffold content

        if (gameState.isGameWon) {
            VictoryDialog(
                score = gameState.score,
                stars = gameState.earnedStars,
                earnedCoins = gameState.earnedCoinsReward,
                bonusMovesScore = gameState.bonusMovesScore,
                winStreak = gameState.winStreak,
                doubleCoinsClaimed = gameState.doubleCoinsClaimed,
                hasNextLevel = gameState.level < WorldRepository.MAX_GAME_LEVELS,
                onWatchAdClick = { viewModel.claimDoubleCoinsReward() },
                onNextLevelClick = { onNextLevelClick(gameState.level + 1) },
                onWorldMapClick = onWorldMapClick
            )
        } else if (gameState.isGameOver) {
            OutOfMovesDialog(
                score = gameState.score,
                targetScore = gameState.targetScore,
                coins = gameState.coins,
                winStreak = gameState.winStreak,
                onWatchAdClick = { viewModel.addRewardMoves(5) },
                onContinueWithCoinsClick = { viewModel.continueWithCoins(cost = 80, extraMoves = 5) },
                onRetryClick = { viewModel.retryLevel() },
                onQuitClick = onWorldMapClick
            )
        }

        if (showPauseDialog) {
            PauseDialog(
                soundEnabled = gameState.soundEnabled,
                hapticsEnabled = gameState.hapticsEnabled,
                reducedMotion = gameState.reducedMotion,
                onToggleSound = { viewModel.toggleSound(it) },
                onToggleHaptics = { viewModel.toggleHaptics(it) },
                onToggleReducedMotion = { viewModel.toggleReducedMotion(it) },
                onRestartClick = { viewModel.retryLevel() },
                onResumeClick = { showPauseDialog = false },
                onQuitClick = onWorldMapClick
            )
        }
    }
}

@Composable
private fun FloatingPopupsOverlay(
    popups: List<FloatingScorePopup>,
    boardInnerSizePx: Float
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(4f)
    ) {
        popups.forEach { popup ->
            key(popup.id) {
                val animProgress = remember { Animatable(0f) }
                LaunchedEffect(popup.id) {
                    animProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
                    )
                }

                val p = animProgress.value
                val floatUpPx = with(density) { (42.dp * p).toPx() }
                val xPx = ((popup.col / 8f) * boardInnerSizePx).roundToInt()
                val yPx = (((popup.row / 8f) * boardInnerSizePx) - floatUpPx).roundToInt()
                val alpha = (1f - (p - 0.55f).coerceAtLeast(0f) / 0.45f).coerceIn(0f, 1f)
                val popScale = if (p < 0.2f) {
                    (0.6f + (p / 0.2f) * 0.55f) * popup.scaleMultiplier
                } else {
                    (1.15f - (p - 0.2f) * 0.2f) * popup.scaleMultiplier
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset { IntOffset(xPx - 48, yPx - 20) }
                        .graphicsLayer {
                            scaleX = popScale
                            scaleY = popScale
                            this.alpha = alpha
                        }
                ) {
                    if (popup.calloutText != null) {
                        Surface(
                            color = Color(0xFF3E2723).copy(alpha = 0.92f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD700))
                        ) {
                            Text(
                                text = popup.calloutText,
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = popup.pointsText,
                        color = Color(popup.colorHex),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun CinthiaCharacter(
    state: CinthiaState,
    speechText: String? = null,
    comboCount: Int = 0,
    size: Dp
) {
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

    val jumpPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == CinthiaState.CHEERING) 260 else 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jumpPhase"
    )

    val jumpDp = if (state == CinthiaState.CHEERING) (-18f * jumpPhase) else (-3f * jumpPhase)

    // Squash & Stretch sutil para dar vida al personaje
    val characterScaleX = if (state == CinthiaState.CHEERING) {
        1f + 0.08f * (1f - jumpPhase)
    } else {
        1f + 0.02f * (1f - jumpPhase)
    }
    val characterScaleY = if (state == CinthiaState.CHEERING) {
        1f + 0.09f * jumpPhase
    } else {
        1f + 0.02f * jumpPhase
    }

    val shake by infiniteTransition.animateFloat(
        initialValue = if (state == CinthiaState.WORRIED) -4.5f else 0f,
        targetValue = if (state == CinthiaState.WORRIED) 4.5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(95, easing = LinearEasing),
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

    Box(contentAlignment = Alignment.TopCenter) {
        Image(
            painter = painterResource(id = currentFrame),
            contentDescription = "Cinthia",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(size)
                .offset(x = shake.dp, y = jumpDp.dp)
                .graphicsLayer {
                    scaleX = characterScaleX
                    scaleY = characterScaleY
                },
            colorFilter = ColorFilter.colorMatrix(colorMatrix)
        )

        // Burbuja de diálogo reactiva de Cinthia (feedback emocional y pistas)
        val displayBubble = speechText ?: if (comboCount >= 2) "✨ ¡Combo x$comboCount!" else null
        AnimatedVisibility(
            visible = displayBubble != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-6).dp)
        ) {
            Surface(
                color = Color(0xFFFFF8E1),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF3E2723)),
                shadowElevation = 6.dp
            ) {
                Text(
                    text = displayBubble ?: "",
                    color = Color(0xFF3E2723),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
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
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.18f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "boosterScale"
    )

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
                .background(if (count > 0) Color(0xFFE53935) else Color(0xFF78909C), shape = CircleShape),
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
