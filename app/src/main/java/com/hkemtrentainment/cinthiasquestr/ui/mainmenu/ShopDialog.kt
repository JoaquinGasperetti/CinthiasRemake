package com.hkemtrentainment.cinthiasquestr.ui.mainmenu

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hkemtrentainment.cinthiasquestr.R
import com.hkemtrentainment.cinthiasquestr.data.PlayerData
import com.hkemtrentainment.cinthiasquestr.data.PlayerRepository
import com.hkemtrentainment.cinthiasquestr.ui.components.ArcadeButtonStyle
import com.hkemtrentainment.cinthiasquestr.ui.components.BoosterCanvasIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.ChunkyArcadeButton
import com.hkemtrentainment.cinthiasquestr.ui.components.ForgedIronPanel
import com.hkemtrentainment.cinthiasquestr.ui.components.GoldCoinIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.MedievalPalette
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalParchmentCard
import com.hkemtrentainment.cinthiasquestr.ui.components.TreasureChestIcon
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.BoosterType

@Composable
fun ShopDialog(
    playerData: PlayerData,
    shopMessage: String?,
    onBuyBooster: (BoosterType) -> Unit,
    onBuyRoyalBundle: () -> Unit = {},
    onWatchAdForCoins: () -> Unit = {},
    onClaimBonusCoins: () -> Unit,
    onDismiss: () -> Unit
) {
    val animatedCoins by animateIntAsState(
        targetValue = playerData.coins,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "shopCoins"
    )
    val streakReward = PlayerRepository.computeDailyStreakReward(playerData.dailyLoginStreak)
    val canClaimToday = playerData.canClaimDailyReward
    val bundleCost = 290

    Dialog(onDismissRequest = onDismiss) {
        RoyalParchmentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Cabecera de la Armería Real con retrato de Cinthia y saldo de oro
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MedievalPalette.IronDark,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, MedievalPalette.GoldMid),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.cinthia_idle_01),
                            contentDescription = "Cinthia",
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ARMERÍA REAL",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MedievalPalette.ParchmentTextDark
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            GoldCoinIcon(size = 15.dp)
                            Text(
                                text = "$animatedCoins Monedas de Oro",
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MedievalPalette.ParchmentTextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Oferta Destacada de Alto Valor (Anclaje de Precios: Lote de Asedio Real -22%)
            ForgedIronPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                trimColor = MedievalPalette.GoldBright,
                contentPadding = PaddingValues(11.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = Color(0xFFC62828),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "OFERTA REAL · AHORRA 22%",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Lote de Asedio Completo",
                                color = MedievalPalette.GoldBright,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "+1 Martillo · +1 Dinamita · +1 Barajar",
                                color = Color(0xFFD7CCC8),
                                fontSize = 10.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            BoosterCanvasIcon(type = BoosterType.HAMMER, size = 22.dp)
                            BoosterCanvasIcon(type = BoosterType.DYNAMITE, size = 22.dp)
                            BoosterCanvasIcon(type = BoosterType.SHUFFLE, size = 22.dp)
                        }
                    }

                    ChunkyArcadeButton(
                        onClick = onBuyRoyalBundle,
                        enabled = playerData.coins >= bundleCost,
                        style = ArcadeButtonStyle.ROYAL_GOLD,
                        cornerRadius = 12.dp,
                        lipHeight = 4.dp,
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GoldCoinIcon(size = 16.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LLEVAR LOTE POR $bundleCost (ANTES 370)",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Artículos individuales con iconografía diegética en Canvas
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                ShopBoosterRow(
                    boosterType = BoosterType.HAMMER,
                    title = "Martillo de Guerra",
                    description = "Pulveriza cualquier gema o bloque al instante",
                    ownedCount = playerData.hammer,
                    price = BoosterType.HAMMER.priceCoins,
                    canAfford = playerData.coins >= BoosterType.HAMMER.priceCoins,
                    onBuy = { onBuyBooster(BoosterType.HAMMER) }
                )

                ShopBoosterRow(
                    boosterType = BoosterType.DYNAMITE,
                    title = "Bomba de Asedio 3x3",
                    description = "Detona un radio de 9 casillas en el tablero",
                    ownedCount = playerData.dynamite,
                    price = BoosterType.DYNAMITE.priceCoins,
                    canAfford = playerData.coins >= BoosterType.DYNAMITE.priceCoins,
                    onBuy = { onBuyBooster(BoosterType.DYNAMITE) }
                )

                ShopBoosterRow(
                    boosterType = BoosterType.SHUFFLE,
                    title = "Remolino Arcano",
                    description = "Reordena todas las gemas sin gastar turno",
                    ownedCount = playerData.shuffle,
                    price = BoosterType.SHUFFLE.priceCoins,
                    canAfford = playerData.coins >= BoosterType.SHUFFLE.priceCoins,
                    onBuy = { onBuyBooster(BoosterType.SHUFFLE) }
                )
            }

            if (shopMessage != null) {
                Spacer(modifier = Modifier.height(7.dp))
                Surface(
                    color = Color(0xFF1B5E20),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = shopMessage,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón de Video Bonificado AdMob (+100 Monedas de Oro Gratis) para impulsar eCPM y retención
            ChunkyArcadeButton(
                onClick = onWatchAdForCoins,
                style = ArcadeButtonStyle.COBALT_MAGIC,
                cornerRadius = 14.dp,
                lipHeight = 4.dp,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                GoldCoinIcon(size = 16.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TESORO REAL GRATIS · VER VIDEO (+100 ORO)",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Botón de Cofre Diario integrado en la Armería
            ChunkyArcadeButton(
                onClick = onClaimBonusCoins,
                enabled = canClaimToday,
                style = if (canClaimToday) ArcadeButtonStyle.EMERALD_PLAY else ArcadeButtonStyle.IRON_SLATE,
                cornerRadius = 14.dp,
                lipHeight = 4.dp,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TreasureChestIcon(isOpen = canClaimToday, isRoyalGold = false, size = 18.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (canClaimToday) {
                        "COFRE DIARIO DÍA ${playerData.dailyLoginStreak}/7 (+$streakReward ORO)"
                    } else {
                        "COFRE DÍA ${playerData.dailyLoginStreak}/7 RECLAMADO · VUELVE MAÑANA"
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ShopBoosterRow(
    boosterType: BoosterType,
    title: String,
    description: String,
    ownedCount: Int,
    price: Int,
    canAfford: Boolean,
    onBuy: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFDF7EC),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, MedievalPalette.BrassFrame)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = MedievalPalette.IronDark,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, MedievalPalette.GoldMid),
                    modifier = Modifier.size(42.dp)
                ) {
                    BoosterCanvasIcon(
                        type = boosterType,
                        size = 26.dp,
                        modifier = Modifier.padding(7.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = MedievalPalette.ParchmentTextDark
                        )
                        Surface(
                            color = Color(0xFFE6D2B2),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "x$ownedCount",
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = MedievalPalette.ParchmentTextDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = description,
                        fontSize = 10.sp,
                        color = MedievalPalette.ParchmentTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            ChunkyArcadeButton(
                onClick = onBuy,
                enabled = canAfford,
                style = ArcadeButtonStyle.ROYAL_GOLD,
                cornerRadius = 12.dp,
                lipHeight = 3.dp,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                modifier = Modifier.width(86.dp)
            ) {
                GoldCoinIcon(size = 14.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$price",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }
    }
}
