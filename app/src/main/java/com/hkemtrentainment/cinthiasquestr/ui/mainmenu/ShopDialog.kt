package com.hkemtrentainment.cinthiasquestr.ui.mainmenu

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hkemtrentainment.cinthiasquestr.data.PlayerData
import com.hkemtrentainment.cinthiasquestr.data.PlayerRepository
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.BoosterType

@Composable
fun ShopDialog(
    playerData: PlayerData,
    shopMessage: String?,
    onBuyBooster: (BoosterType) -> Unit,
    onClaimBonusCoins: () -> Unit,
    onDismiss: () -> Unit
) {
    val animatedCoins by animateIntAsState(
        targetValue = playerData.coins,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "shopCoins"
    )
    val streakReward = PlayerRepository.computeDailyStreakReward(playerData.dailyLoginStreak)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5E6CE)),
            border = BorderStroke(3.dp, Color(0xFF3E2723)),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tienda de Cinthia",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3E2723)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF3E2723)
                        )
                    }
                }

                Surface(
                    color = Color(0xFF3E2723),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Saldo actual: 🪙 $animatedCoins",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                ShopBoosterRow(
                    title = "Martillo Mágico",
                    description = "Destruye cualquier ficha individual",
                    ownedCount = playerData.hammer,
                    price = BoosterType.HAMMER.priceCoins,
                    canAfford = playerData.coins >= BoosterType.HAMMER.priceCoins,
                    icon = Icons.Default.Build,
                    onBuy = { onBuyBooster(BoosterType.HAMMER) }
                )

                ShopBoosterRow(
                    title = "Dinamita Real",
                    description = "Explota un área de 3x3 fichas",
                    ownedCount = playerData.dynamite,
                    price = BoosterType.DYNAMITE.priceCoins,
                    canAfford = playerData.coins >= BoosterType.DYNAMITE.priceCoins,
                    icon = Icons.Default.Clear,
                    onBuy = { onBuyBooster(BoosterType.DYNAMITE) }
                )

                ShopBoosterRow(
                    title = "Barajar Tablero",
                    description = "Reordena todas las gemas del tablero",
                    ownedCount = playerData.shuffle,
                    price = BoosterType.SHUFFLE.priceCoins,
                    canAfford = playerData.coins >= BoosterType.SHUFFLE.priceCoins,
                    icon = Icons.Default.Refresh,
                    onBuy = { onBuyBooster(BoosterType.SHUFFLE) }
                )

                if (shopMessage != null) {
                    Text(
                        text = shopMessage,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = onClaimBonusCoins,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.5.dp, Color(0xFF8D6E63)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3E2723))
                ) {
                    Text(
                        text = "🎁 Cofre Racha Diaria Día ${playerData.dailyLoginStreak} (+$streakReward 🪙)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopBoosterRow(
    title: String,
    description: String,
    ownedCount: Int,
    price: Int,
    canAfford: Boolean,
    icon: ImageVector,
    onBuy: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFEDE0D4),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFD7CCC8))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = Color(0xFF3E2723),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "$title (x$ownedCount)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF3E2723)
                    )
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = Color(0xFF5D4037)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onBuy,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB300),
                    contentColor = Color(0xFF3E2723)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "$price 🪙",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
