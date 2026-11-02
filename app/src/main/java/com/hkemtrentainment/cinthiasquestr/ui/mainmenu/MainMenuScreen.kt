package com.hkemtrentainment.cinthiasquestr.ui.mainmenu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkemtrentainment.cinthiasquestr.CinthiasQuestApp

@Composable
fun MainMenuScreen(
    onPlayClick: () -> Unit,
    viewModel: MainMenuViewModel = viewModel(
        factory = MainMenuViewModel.Factory(
            (LocalContext.current.applicationContext as CinthiasQuestApp).playerRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isShopOpen) {
        ShopDialog(
            playerData = uiState.playerData,
            shopMessage = uiState.shopMessage,
            onBuyBooster = { viewModel.buyBooster(it) },
            onClaimBonusCoins = { viewModel.claimFreeBonusCoins() },
            onDismiss = { viewModel.closeShop() }
        )
    }

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = Color(0xFF3E2723)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Espacio Publicitario (AdMob)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8D6E63)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF3E2723), Color(0xFF1B1210))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Cinthia's Quest",
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD4AF37)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF2C1A14),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFD700))
                    ) {
                        Text(
                            text = "⭐ ${uiState.playerData.levelStars.values.sum()} Estrellas",
                            color = Color(0xFFF5E6CE),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFF2C1A14),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFB300))
                    ) {
                        Text(
                            text = "🪙 ${uiState.playerData.coins} Monedas",
                            color = Color(0xFFF5E6CE),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(56.dp))

                Button(
                    onClick = onPlayClick,
                    modifier = Modifier.fillMaxWidth(0.65f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4AF37),
                        contentColor = Color(0xFF3E2723)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Text(text = "COMENZAR AVENTURA", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = { viewModel.openShop() },
                    modifier = Modifier.fillMaxWidth(0.65f),
                    border = BorderStroke(2.dp, Color(0xFF8D6E63)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF5E6CE)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "TIENDA DE OBJETOS", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
