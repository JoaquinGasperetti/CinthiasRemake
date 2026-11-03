package com.hkemtrentainment.cinthiasquestr.ui.gameboard.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.hkemtrentainment.cinthiasquestr.R

@Composable
fun GameTopBar(
    score: Int,
    targetScore: Int,
    movesLeft: Int,
    level: Int,
    coins: Int,
    onPauseClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Image(
            painter = painterResource(id = R.drawable.ui_top_bar),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().aspectRatio(1080f / 128f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1080f / 128f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(0.004f))
            
            // MOVS
            Column(
                modifier = Modifier.weight(0.192f).fillMaxHeight(),
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
                    maxLines = 1
                )
            }
            
            Spacer(modifier = Modifier.weight(0.043f))
            
            // PUNTOS
            Column(
                modifier = Modifier.weight(0.442f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "PUNTOS",
                    fontSize = 10.sp,
                    color = Color(0xFFD7CCC8),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "$score / $targetScore",
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
            
            // NIVEL y monedas
            Column(
                modifier = Modifier.weight(0.188f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "NIVEL $level",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5E6CE),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "🪙 $coins",
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
