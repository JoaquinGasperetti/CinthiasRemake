package com.hkemtrentainment.cinthiasquestr.ui.worldmap

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkemtrentainment.cinthiasquestr.CinthiasQuestApp
import com.hkemtrentainment.cinthiasquestr.R
import com.hkemtrentainment.cinthiasquestr.audio.GameSound
import com.hkemtrentainment.cinthiasquestr.data.WorldConfig
import com.hkemtrentainment.cinthiasquestr.data.WorldRepository
import com.hkemtrentainment.cinthiasquestr.ui.components.ArcadeButtonStyle
import com.hkemtrentainment.cinthiasquestr.ui.components.ChunkyArcadeButton
import com.hkemtrentainment.cinthiasquestr.ui.components.ForgedIronPanel
import com.hkemtrentainment.cinthiasquestr.ui.components.GoldCoinIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.MedievalPalette
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalCrownIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalStarIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.StreakFlameIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.TorchEmbersOverlay
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.BoardShapeMiniBadge
import com.hkemtrentainment.cinthiasquestr.ui.worldmap.components.LevelDetailDialog
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun WorldMapScreen(
    onBackClick: () -> Unit,
    onLevelSelect: (Int) -> Unit,
    viewModel: WorldMapViewModel = viewModel(
        factory = WorldMapViewModel.Factory(
            (LocalContext.current.applicationContext as CinthiasQuestApp).playerRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val soundManager = remember(context) {
        (context.applicationContext as CinthiasQuestApp).soundManager
    }
    val haptic = LocalHapticFeedback.current

    val worldYOffsets = remember { mutableStateMapOf<Int, Int>() }

    uiState.activeDetailLevel?.let { levelNum ->
        LevelDetailDialog(
            levelNumber = levelNum,
            starsEarned = uiState.levelStars[levelNum] ?: 0,
            winStreak = uiState.winStreak,
            hammerCount = uiState.hammerCount,
            dynamiteCount = uiState.dynamiteCount,
            shuffleCount = uiState.shuffleCount,
            onPlayClick = { selected ->
                soundManager.play(GameSound.MATCH_SUCCESS_GEM)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.dismissDetailDialog()
                onLevelSelect(selected)
            },
            onDismiss = {
                soundManager.play(GameSound.TILE_DESELECT)
                viewModel.dismissDetailDialog()
            }
        )
    }

    Scaffold(
        containerColor = MedievalPalette.DungeonAbyss,
        topBar = {
            WorldMapTopBar(
                coins = uiState.coins,
                totalStars = uiState.totalStars,
                winStreak = uiState.winStreak,
                onBackClick = {
                    soundManager.play(GameSound.TILE_DESELECT)
                    onBackClick()
                }
            )
        },
        bottomBar = {
            // Barra fija de acción rápida para entrar al nivel actual sin perderse en el mapa
            Surface(
                color = MedievalPalette.IronDark.copy(alpha = 0.96f),
                border = BorderStroke(1.5.dp, MedievalPalette.BrassFrame),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeWorld = WorldRepository.getWorldForLevel(uiState.maxUnlockedLevel)
                    val activeLevelConfig = WorldRepository.getLevelConfig(uiState.maxUnlockedLevel)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FRENTE DE BATALLA ACTUAL",
                            color = MedievalPalette.GoldBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Nv.${uiState.maxUnlockedLevel} · ${activeLevelConfig.levelTitle} (${activeLevelConfig.boardShape.displayName})",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    ChunkyArcadeButton(
                        onClick = {
                            soundManager.play(GameSound.TILE_SELECT)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.onLevelClicked(uiState.maxUnlockedLevel)
                        },
                        style = ArcadeButtonStyle.EMERALD_PLAY,
                        cornerRadius = 14.dp,
                        lipHeight = 4.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                        modifier = Modifier.width(155.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "NIVEL ${uiState.maxUnlockedLevel}",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Image(
                painter = painterResource(id = R.drawable.bg_game),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.45f,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF140E12).copy(alpha = 0.85f),
                                Color(0xFF21171D).copy(alpha = 0.78f),
                                Color(0xFF120C0F).copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            TorchEmbersOverlay()

            Column(modifier = Modifier.fillMaxSize()) {
                // Cinta de Selección de Mundos con Estrellas por Mundo
                WorldTabsRow(
                    selectedWorld = uiState.selectedWorld,
                    maxUnlockedLevel = uiState.maxUnlockedLevel,
                    levelStars = uiState.levelStars,
                    onWorldSelect = { world ->
                        soundManager.play(GameSound.TILE_SELECT)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.selectWorld(world)
                        worldYOffsets[world.id]?.let { yPos ->
                            coroutineScope.launch {
                                scrollState.animateScrollTo(maxOf(0, yPos - 16))
                            }
                        }
                    }
                )

                // Mapa de Ruta Sinuosa con Pedestales 3D y Nodos de Jefe
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WorldRepository.WORLDS.forEach { world ->
                            val worldStarsEarned = (world.startLevel..world.endLevel).sumOf { lvl ->
                                uiState.levelStars[lvl] ?: 0
                            }
                            val worldMaxStars = (world.endLevel - world.startLevel + 1) * 3

                            Box(
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    val yInScroll = coordinates.positionInParent().y.toInt()
                                    worldYOffsets[world.id] = yInScroll
                                }
                            ) {
                                WorldBannerHeader(
                                    world = world,
                                    earnedStars = worldStarsEarned,
                                    maxStars = worldMaxStars,
                                    isWorldUnlocked = uiState.maxUnlockedLevel >= world.startLevel
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            val worldLevels = (world.startLevel..world.endLevel).toList()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((worldLevels.size * 114).dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                PathCanvas(
                                    levelsCount = worldLevels.size,
                                    worldColor = world.primaryColor
                                )

                                worldLevels.forEachIndexed { index, levelNum ->
                                    val xOffsetDp = (sin(index * 1.2f) * 105).dp
                                    val yOffsetDp = (index * 104).dp

                                    val isUnlocked = levelNum <= uiState.maxUnlockedLevel
                                    val isCurrent = levelNum == uiState.maxUnlockedLevel
                                    val stars = uiState.levelStars[levelNum] ?: 0
                                    val isBossNode = levelNum == world.endLevel

                                    Box(
                                        modifier = Modifier.offset(x = xOffsetDp, y = yOffsetDp)
                                    ) {
                                        LevelNodeItem(
                                            levelNumber = levelNum,
                                            isUnlocked = isUnlocked,
                                            isCurrent = isCurrent,
                                            isBossNode = isBossNode,
                                            stars = stars,
                                            worldColor = world.primaryColor,
                                            onClick = {
                                                if (isUnlocked) {
                                                    soundManager.play(GameSound.TILE_SELECT)
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.onLevelClicked(levelNum)
                                                } else {
                                                    soundManager.play(GameSound.TILE_INVALID)
                                                    Toast.makeText(
                                                        context,
                                                        "Supera el Nivel ${uiState.maxUnlockedLevel} para desbloquear el Nivel $levelNum",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))
                        }

                        Spacer(modifier = Modifier.height(36.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WorldMapTopBar(
    coins: Int,
    totalStars: Int,
    winStreak: Int = 0,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MedievalPalette.IronDark,
        border = BorderStroke(1.5.dp, MedievalPalette.BrassFrame),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = MedievalPalette.IronSlate,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, MedievalPalette.GoldMid),
                    modifier = Modifier.size(38.dp)
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al Salón Real",
                            tint = MedievalPalette.GoldBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "MAPA DEL REINO",
                        fontWeight = FontWeight.Black,
                        color = MedievalPalette.GoldBright,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "25 Niveles · 16 Formas de Tablero",
                        color = Color(0xFFD7CCC8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (winStreak > 0) {
                    Surface(
                        color = Color(0xFF1A1215),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFFF7043))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StreakFlameIcon(size = 15.dp)
                            Text(
                                text = "x$winStreak",
                                color = Color(0xFFFFCC80),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Surface(
                    color = Color(0xFF1A1215),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, MedievalPalette.BrassFrame)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RoyalStarIcon(filled = true, size = 15.dp)
                        Text(
                            text = "$totalStars/75",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }

                Surface(
                    color = Color(0xFF1A1215),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, MedievalPalette.GoldMid)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        GoldCoinIcon(size = 15.dp)
                        Text(
                            text = "$coins",
                            color = MedievalPalette.GoldBright,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorldTabsRow(
    selectedWorld: WorldConfig,
    maxUnlockedLevel: Int = 1,
    levelStars: Map<Int, Int> = emptyMap(),
    onWorldSelect: (WorldConfig) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF181115))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(WorldRepository.WORLDS) { world ->
            val isSelected = world.id == selectedWorld.id
            val isUnlocked = maxUnlockedLevel >= world.startLevel
            val worldStars = (world.startLevel..world.endLevel).sumOf { levelStars[it] ?: 0 }

            Surface(
                color = if (isSelected) world.primaryColor else MedievalPalette.IronDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MedievalPalette.GoldBright else MedievalPalette.BrassFrame
                ),
                modifier = Modifier.clickable { onWorldSelect(world) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isUnlocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFB0BEC5),
                            modifier = Modifier.size(13.dp)
                        )
                    } else {
                        RoyalStarIcon(filled = worldStars > 0, size = 13.dp)
                    }
                    Text(
                        text = "M${world.id} · ${world.name}",
                        color = if (isSelected) Color.White else Color(0xFFF6EAD4),
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "$worldStars/15",
                        color = if (isSelected) MedievalPalette.GoldBright else Color(0xFFBDB3B8),
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WorldBannerHeader(
    world: WorldConfig,
    earnedStars: Int = 0,
    maxStars: Int = 15,
    isWorldUnlocked: Boolean = true
) {
    val worldGemRes = when (world.id) {
        1 -> R.drawable.gem_green
        2 -> R.drawable.gem_yellow
        3 -> R.drawable.gem_blue
        4 -> R.drawable.gem_magenta
        else -> R.drawable.gem_red
    }
    val progress = if (maxStars > 0) (earnedStars.toFloat() / maxStars).coerceIn(0f, 1f) else 0f

    ForgedIronPanel(
        modifier = Modifier.fillMaxWidth(0.92f),
        cornerRadius = 20.dp,
        trimColor = if (isWorldUnlocked) MedievalPalette.GoldMid else Color(0xFF607D8B),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(colors = world.gradientColors))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, MedievalPalette.GoldBright),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Image(
                            painter = painterResource(id = worldGemRes),
                            contentDescription = world.name,
                            modifier = Modifier.padding(7.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "REGIÓN ${world.id} · NIVELES ${world.startLevel}-${world.endLevel}",
                            color = MedievalPalette.GoldBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp
                        )
                        Text(
                            text = world.name,
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = world.description,
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra de Maestría de Estrellas de la Región
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, MedievalPalette.GoldBright.copy(alpha = 0.7f), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFF176), Color(0xFFFFB300))
                                )
                            )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    RoyalStarIcon(filled = true, size = 14.dp)
                    Text(
                        text = "$earnedStars/$maxStars",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun PathCanvas(
    levelsCount: Int,
    worldColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val path = Path()

        for (i in 0 until levelsCount) {
            val xOffset = sin(i * 1.2f) * 105.dp.toPx()
            val x = centerX + xOffset
            val y = (i * 104).dp.toPx() + 36.dp.toPx()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                val prevXOffset = sin((i - 1) * 1.2f) * 105.dp.toPx()
                val prevX = centerX + prevXOffset
                val prevY = ((i - 1) * 104).dp.toPx() + 36.dp.toPx()

                val controlY1 = prevY + 42.dp.toPx()
                val controlY2 = y - 42.dp.toPx()

                path.cubicTo(prevX, controlY1, x, controlY2, x, y)
            }
        }

        // Borde exterior de piedra oscura
        drawPath(
            path = path,
            color = Color(0xFF0E090C).copy(alpha = 0.75f),
            style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
        )

        // Camino empedrado real
        drawPath(
            path = path,
            color = worldColor,
            style = Stroke(
                width = 8.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(28f, 16f), 0f)
            )
        )
    }
}

@Composable
fun LevelNodeItem(
    levelNumber: Int,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    isBossNode: Boolean = levelNumber % 5 == 0,
    stars: Int,
    worldColor: Color,
    onClick: () -> Unit
) {
    val levelConfig = remember(levelNumber) { WorldRepository.getLevelConfig(levelNumber) }
    val infiniteTransition = rememberInfiniteTransition(label = "nodePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCurrent) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(780, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nodePulseScale"
    )

    val nodeSize = if (isBossNode) 72.dp else 62.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Marcador con el retrato real de Cinthia en el nivel activo (sin emojis)
        if (isCurrent) {
            Surface(
                color = MedievalPalette.IronDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(2.dp, MedievalPalette.GoldBright),
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .shadow(6.dp, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cinthia_idle_01),
                        contentDescription = "Cinthia",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AQUÍ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MedievalPalette.GoldBright
                    )
                }
            }
        } else if (isBossNode) {
            RoyalCrownIcon(
                size = 20.dp,
                modifier = Modifier.offset(y = (-3).dp)
            )
        }

        // Pedestal 3D con labio inferior y marco metálico
        Box(
            modifier = Modifier
                .size(nodeSize)
                .scale(pulseScale)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.TopCenter
        ) {
            // Labio 3D inferior del nodo
            Box(
                modifier = Modifier
                    .size(nodeSize - 2.dp)
                    .offset(y = 4.dp)
                    .clip(CircleShape)
                    .background(MedievalPalette.InkOutline)
            )

            Box(
                modifier = Modifier
                    .size(nodeSize - 4.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFE082), Color(0xFFFF8F00))
                            )
                            isUnlocked && isBossNode -> Brush.verticalGradient(
                                colors = listOf(Color(0xFFEF5350), Color(0xFFB71C1C))
                            )
                            isUnlocked -> Brush.verticalGradient(
                                colors = listOf(worldColor, Color(0xFF1F161B))
                            )
                            else -> Brush.verticalGradient(
                                colors = listOf(Color(0xFF4F434A), Color(0xFF261F23))
                            )
                        }
                    )
                    .border(
                        width = if (isBossNode || isCurrent) 3.dp else 2.5.dp,
                        color = when {
                            isCurrent -> Color.White
                            isBossNode && isUnlocked -> MedievalPalette.GoldBright
                            isUnlocked -> MedievalPalette.GoldMid
                            else -> Color(0xFF6D6168)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$levelNumber",
                            fontSize = if (isBossNode) 20.sp else 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isCurrent) Color(0xFF2B180A) else Color.White,
                            fontFamily = FontFamily.Serif
                        )
                        BoardShapeMiniBadge(
                            shape = levelConfig.boardShape,
                            accentColor = if (isCurrent) Color(0xFF2B180A) else MedievalPalette.GoldBright,
                            size = 11.dp
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = Color(0xFF9E9299),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Cinta de 3 estrellas bajo el nodo
        if (isUnlocked && stars > 0) {
            Row(
                modifier = Modifier
                    .offset(y = 3.dp)
                    .background(MedievalPalette.IronDark.copy(alpha = 0.90f), RoundedCornerShape(8.dp))
                    .border(1.dp, MedievalPalette.BrassFrame, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(3) { index ->
                    RoyalStarIcon(
                        filled = index < stars,
                        size = 12.dp
                    )
                }
            }
        }
    }
}
