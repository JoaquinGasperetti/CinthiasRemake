package com.hkemtrentainment.cinthiasquestr.ui.worldmap

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkemtrentainment.cinthiasquestr.CinthiasQuestApp
import com.hkemtrentainment.cinthiasquestr.data.WorldConfig
import com.hkemtrentainment.cinthiasquestr.data.WorldRepository
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

    // Y offsets for each world section header within the scrollable Column
    val worldYOffsets = remember { mutableStateMapOf<Int, Int>() }

    // Active detail dialog
    uiState.activeDetailLevel?.let { levelNum ->
        LevelDetailDialog(
            levelNumber = levelNum,
            starsEarned = uiState.levelStars[levelNum] ?: 0,
            onPlayClick = { selected ->
                viewModel.dismissDetailDialog()
                onLevelSelect(selected)
            },
            onDismiss = { viewModel.dismissDetailDialog() }
        )
    }

    Scaffold(
        topBar = {
            WorldMapTopBar(
                coins = uiState.coins,
                totalStars = uiState.totalStars,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1B100C),
                            Color(0xFF2C1A14),
                            Color(0xFF1B100C)
                        )
                    )
                )
        ) {
            // World Quick Tabs Bar
            WorldTabsRow(
                selectedWorld = uiState.selectedWorld,
                onWorldSelect = { world ->
                    viewModel.selectWorld(world)
                    // Smooth scroll to selected world position
                    worldYOffsets[world.id]?.let { yPos ->
                        coroutineScope.launch {
                            scrollState.animateScrollTo(maxOf(0, yPos - 16))
                        }
                    }
                }
            )

            // Candy Crush Style Winding Path Map
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Group levels by world
                    WorldRepository.WORLDS.forEach { world ->
                        Box(
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                val yInScroll = coordinates.positionInParent().y.toInt()
                                worldYOffsets[world.id] = yInScroll
                            }
                        ) {
                            WorldBannerHeader(world = world)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        val worldLevels = (world.startLevel..world.endLevel).toList()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((worldLevels.size * 110).dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            // Canvas to draw path lines connecting nodes
                            PathCanvas(
                                levelsCount = worldLevels.size,
                                worldColor = world.primaryColor
                            )

                            // Level Nodes along the path
                            worldLevels.forEachIndexed { index, levelNum ->
                                val xOffsetDp = (sin(index * 1.2f) * 110).dp
                                val yOffsetDp = (index * 100).dp

                                val isUnlocked = levelNum <= uiState.maxUnlockedLevel
                                val isCurrent = levelNum == uiState.maxUnlockedLevel
                                val stars = uiState.levelStars[levelNum] ?: 0

                                Box(
                                    modifier = Modifier
                                        .offset(x = xOffsetDp, y = yOffsetDp)
                                ) {
                                    LevelNodeItem(
                                        levelNumber = levelNum,
                                        isUnlocked = isUnlocked,
                                        isCurrent = isCurrent,
                                        stars = stars,
                                        worldColor = world.primaryColor,
                                        onClick = {
                                            if (isUnlocked) {
                                                viewModel.onLevelClicked(levelNum)
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "¡Completa el Nivel ${uiState.maxUnlockedLevel} para desbloquear el Nivel $levelNum!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
}

@Composable
fun WorldMapTopBar(
    coins: Int,
    totalStars: Int,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color(0xFF3E2723),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color(0xFFF5E6CE)
                )
            }

            Text(
                text = "Cinthia's Quest",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                fontFamily = FontFamily.Serif
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cloud Sync Chip
                Surface(
                    color = Color(0xFF2C1A14),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF81D4FA))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "☁️",
                            fontSize = 13.sp
                        )
                    }
                }

                // Stars Chip
                Surface(
                    color = Color(0xFF2C1A14),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD700))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$totalStars",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Coins Chip
                Surface(
                    color = Color(0xFF2C1A14),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFB300))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🪙",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$coins",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
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
    onWorldSelect: (WorldConfig) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF251612))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(WorldRepository.WORLDS) { world ->
            val isSelected = world.id == selectedWorld.id
            FilterChip(
                selected = isSelected,
                onClick = { onWorldSelect(world) },
                label = {
                    Text(
                        text = "Mundo ${world.id}",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = world.primaryColor,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF3E2723),
                    labelColor = Color(0xFFD7CCC8)
                )
            )
        }
    }
}

@Composable
fun WorldBannerHeader(world: WorldConfig) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(colors = world.gradientColors)
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MUNDO ${world.id}",
                    color = Color(0xFFFFD700),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = world.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = world.description,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif
                )
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
            val xOffset = sin(i * 1.2f) * 110.dp.toPx()
            val x = centerX + xOffset
            val y = (i * 100).dp.toPx() + 32.dp.toPx()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                val prevXOffset = sin((i - 1) * 1.2f) * 110.dp.toPx()
                val prevX = centerX + prevXOffset
                val prevY = ((i - 1) * 100).dp.toPx() + 32.dp.toPx()

                val controlY1 = prevY + 40.dp.toPx()
                val controlY2 = y - 40.dp.toPx()

                path.cubicTo(prevX, controlY1, x, controlY2, x, y)
            }
        }

        // Base outer shadow stroke
        drawPath(
            path = path,
            color = Color.Black.copy(alpha = 0.4f),
            style = Stroke(width = 16.dp.toPx())
        )

        // Main golden path
        drawPath(
            path = path,
            color = worldColor,
            style = Stroke(
                width = 8.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f), 0f)
            )
        )
    }
}

@Composable
fun LevelNodeItem(
    levelNumber: Int,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    stars: Int,
    worldColor: Color,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isCurrent) {
            // Cinthia Avatar Marker on active level
            Surface(
                color = Color(0xFFFFD700),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, Color(0xFF3E2723)),
                modifier = Modifier
                    .offset(y = (-8).dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = "👸 Cinthia",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(if (isCurrent) pulseScale else 1f)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    when {
                        isCurrent -> Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD700), Color(0xFFFF8F00))
                        )
                        isUnlocked -> Brush.radialGradient(
                            colors = listOf(Color(0xFFFFF9C4), worldColor)
                        )
                        else -> Brush.radialGradient(
                            colors = listOf(Color(0xFF78909C), Color(0xFF37474F))
                        )
                    }
                )
                .border(
                    width = 3.dp,
                    color = when {
                        isCurrent -> Color.White
                        isUnlocked -> Color(0xFFFFD700)
                        else -> Color(0xFF546E7A)
                    },
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (isUnlocked) {
                Text(
                    text = "$levelNumber",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCurrent) Color(0xFF3E2723) else Color.White,
                    fontFamily = FontFamily.Serif
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Bloqueado",
                    tint = Color(0xFFB0BEC5),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Star rating mini display under completed node
        if (isUnlocked && stars > 0) {
            Row(
                modifier = Modifier
                    .offset(y = 4.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(3) { index ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (index < stars) Color(0xFFFFD700) else Color(0xFF78909C),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
