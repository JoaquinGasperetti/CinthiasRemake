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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
    // Animación progresiva del marcador de puntos y monedas (Rolling Numbers Juice)
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

    // Pulso de alerta cuando quedan pocos movimientos (<= 5)
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

                // MOVS con latido de urgencia
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
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "$movesLeft",
                        fontSize = 16.sp,
                        color = if (movesLeft <= 5) Color(0xFFFF5252) else Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        modifier = Modifier.scale(lowMovesPulse)
                    )
                }

                Spacer(modifier = Modifier.weight(0.043f))

                // PUNTOS + Barra de progreso de 3 Estrellas en tiempo real
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
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$animatedScore / $targetScore",
                            fontSize = 13.sp,
                            color = if (score >= targetScore) Color(0xFFFFD700) else Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            repeat(3) { idx ->
                                val lit = idx < earnedStarsLive
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (lit) Color(0xFFFFD700) else Color(0xFF8D6E63).copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Barra de progreso con marcadores de 1⭐, 2⭐ y 3⭐
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C1A14))
                            .border(1.dp, Color(0xFF8D6E63), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(starBarProgress)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF66BB6A),
                                            Color(0xFFFFCA28),
                                            Color(0xFFFF8F00)
                                        )
                                    )
                                )
                        )
                    }
                }

                // NIVEL, monedas y racha
                Column(
                    modifier = Modifier
                        .weight(0.188f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (winStreak > 0) "NV.$level 🔥$winStreak" else "NIVEL $level",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF5E6CE),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "🪙 $animatedCoins",
                        fontSize = 10.sp,
                        color = Color(0xFFFFD700),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // PAUSE
                Box(
                    modifier = Modifier
                        .weight(0.126f)
                        .fillMaxHeight()
                        .clickable(onClick = onPauseClick),
                    contentAlignment = Alignment.Center
                ) {
                    // El ícono ya está en la imagen, el Box transparente lo hace clickeable
                }

                Spacer(modifier = Modifier.weight(0.005f))
            }
        }
    }
}
