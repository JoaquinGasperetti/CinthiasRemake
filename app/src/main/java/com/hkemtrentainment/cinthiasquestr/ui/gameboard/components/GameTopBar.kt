package com.hkemtrentainment.cinthiasquestr.ui.gameboard.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkemtrentainment.cinthiasquestr.R
import com.hkemtrentainment.cinthiasquestr.ui.components.GoldCoinIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalStarIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.StreakFlameIcon

@Composable
fun GameTopBar(
    score: Int,
    targetScore: Int,
    movesLeft: Int,
    level: Int,
    coins: Int,
    winStreak: Int = 0,
    onPauseClick: () -> Unit
) {
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "animatedScore"
    )
    val animatedCoins by animateIntAsState(
        targetValue = coins,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "animatedCoins"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "topBarUrgency")
    val lowMovesPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (movesLeft in 1..5) 1.22f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lowMovesPulse"
    )

    val maxThreeStarScore = (targetScore * 1.8f).coerceAtLeast(1f)
    val starBarProgress by animateFloatAsState(
        targetValue = (score.toFloat() / maxThreeStarScore).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "starBarProgress"
    )
    val earnedStarsLive = when {
        score >= (targetScore * 1.8).toInt() -> 3
        score >= (targetScore * 1.4).toInt() -> 2
        score >= targetScore -> 1
        else -> 0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.ui_top_bar),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1080f / 128f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1080f / 128f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(0.004f))

                // MOVIMIENTOS RESTANTES
                Column(
                    modifier = Modifier
                        .weight(0.192f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "MOVS",
                        fontSize = 10.sp,
                        color = Color(0xFFD7CCC8),
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Text(
                        text = "$movesLeft",
                        fontSize = 17.sp,
                        color = if (movesLeft <= 5) Color(0xFFFF5252) else Color.White,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        modifier = Modifier.scale(lowMovesPulse)
                    )
                }

                Spacer(modifier = Modifier.weight(0.043f))

                // PUNTOS + BARRA DE 3 ESTRELLAS EN VIVO
                Column(
                    modifier = Modifier
                        .weight(0.442f)
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "$animatedScore / $targetScore",
                            fontSize = 13.sp,
                            color = if (score >= targetScore) Color(0xFFFFD54F) else Color.White,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(3) { idx ->
                                RoyalStarIcon(
                                    filled = idx < earnedStarsLive,
                                    size = 12.dp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B1216))
                            .border(1.dp, Color(0xFF8C734B), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(starBarProgress)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF43D165),
                                            Color(0xFFFFCA28),
                                            Color(0xFFFF8F00)
                                        )
                                    )
                                )
                        )
                    }
                }

                // NIVEL, RACHA Y ORO (con iconos en Canvas en lugar de emojis)
                Column(
                    modifier = Modifier
                        .weight(0.188f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "NV.$level",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF6EAD4),
                            maxLines = 1
                        )
                        if (winStreak > 0) {
                            StreakFlameIcon(size = 12.dp)
                            Text(
                                text = "x$winStreak",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFCC80),
                                maxLines = 1
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        GoldCoinIcon(size = 11.dp)
                        Text(
                            text = "$animatedCoins",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // BOTÓN DE PAUSA INTEGRADO EN EL MARCO
                Box(
                    modifier = Modifier
                        .weight(0.126f)
                        .fillMaxHeight()
                        .clickable(onClick = onPauseClick),
                    contentAlignment = Alignment.Center
                ) {
                }

                Spacer(modifier = Modifier.weight(0.005f))
            }
        }
    }
}
