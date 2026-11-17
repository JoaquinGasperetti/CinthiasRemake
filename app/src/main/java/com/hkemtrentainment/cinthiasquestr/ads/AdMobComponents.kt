package com.hkemtrentainment.cinthiasquestr.ads

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.hkemtrentainment.cinthiasquestr.CinthiasQuestApp
import com.hkemtrentainment.cinthiasquestr.ui.components.ArcadeButtonStyle
import com.hkemtrentainment.cinthiasquestr.ui.components.ChunkyArcadeButton
import com.hkemtrentainment.cinthiasquestr.ui.components.ForgedIronPanel
import com.hkemtrentainment.cinthiasquestr.ui.components.GoldCoinIcon
import com.hkemtrentainment.cinthiasquestr.ui.components.MedievalPalette
import com.hkemtrentainment.cinthiasquestr.ui.components.RoyalParchmentCard
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.CinthiaAnimatedPortrait
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.CinthiaState
import kotlinx.coroutines.delay

/**
 * Banner Adaptativo Anclado oficial de Google AdMob (`ca-app-pub-2266949018056491/2593606728`)
 * con reserva de altura fija para evitar saltos de interfaz (CLS) y clics accidentales,
 * cumpliendo las políticas de implementación de banners de AdMob.
 */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdPacingPolicy.BANNER_AD_UNIT_ID
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val adWidthDp = configuration.screenWidthDp.coerceAtLeast(320)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            .background(Color(0xFF120D10).copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = MedievalPalette.BrassDark.copy(alpha = 0.55f),
                shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isPreview) {
            Text(
                text = "PATROCINIO REAL · ADMOB BANNER (ATP)",
                color = Color(0xFF7A685D),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
        } else {
            var isAdLoaded by remember { mutableStateOf(false) }
            val adMobManager = remember(context) {
                (context.applicationContext as? CinthiasQuestApp)?.adMobManager
            }

            val adView = remember(context, adUnitId, adWidthDp) {
                AdView(context).apply {
                    this.adUnitId = adUnitId
                    setAdSize(
                        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                            context,
                            adWidthDp
                        )
                    )
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isAdLoaded = false
                        }
                    }
                    val request = adMobManager?.buildAtpAdRequest()
                        ?: com.google.android.gms.ads.AdRequest.Builder().build()
                    loadAd(request)
                }
            }

            DisposableEffect(adView) {
                onDispose {
                    try {
                        adView.destroy()
                    } catch (_: Exception) {
                    }
                }
            }

            if (!isAdLoaded) {
                Text(
                    text = "PATROCINIO REAL · ADMOB ATP",
                    color = Color(0xFF7A685D),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )
            }

            AndroidView(
                factory = { adView },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Pantalla de introducción obligatoria para el formato **Intersticial Bonificado**
 * (`ca-app-pub-2266949018056491/3527414441`).
 *
 * Cumple estrictamente la política de Google AdMob para Rewarded Interstitial Ads:
 * - Anuncia claramente la recompensa que recibirá el jugador (`+60 Monedas de Oro`).
 * - Muestra una cuenta regresiva visible (3 segundos) antes de iniciar el anuncio.
 * - Incluye un botón claro de "Saltar / No, gracias" para cancelar el anuncio antes de que empiece.
 * Ref: https://developers.google.com/admob/android/rewarded-interstitial
 */
@Composable
fun RewardedInterstitialIntroDialog(
    rewardCoins: Int = 60,
    title: String = "BENDICIÓN DE LA CORONA",
    subtitle: String = "Recibe un tesoro extra viendo un anuncio breve",
    countdownSeconds: Int = 3,
    onStartAdNow: () -> Unit,
    onSkipAd: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(countdownSeconds.coerceAtLeast(1)) }
    var hasTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !hasTriggered) {
            delay(1000L)
            secondsLeft--
        }
        if (!hasTriggered && secondsLeft <= 0) {
            hasTriggered = true
            onStartAdNow()
        }
    }

    val progressFraction by animateFloatAsState(
        targetValue = (secondsLeft.toFloat() / countdownSeconds.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "introCountdownProgress"
    )

    Dialog(
        onDismissRequest = {
            if (!hasTriggered) {
                hasTriggered = true
                onSkipAd()
            }
        },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        RoyalParchmentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            accentTrimColor = MedievalPalette.GoldBright,
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MedievalPalette.IronDark,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MedievalPalette.GoldMid)
                ) {
                    Text(
                        text = "ANUNCIO BONIFICADO BREVE · ATP",
                        color = MedievalPalette.GoldBright,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = title,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp,
                    color = MedievalPalette.ParchmentTextDark,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MedievalPalette.ParchmentTextMuted,
                    textAlign = TextAlign.Center
                )

                ForgedIronPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    trimColor = MedievalPalette.GoldBright,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = Color(0xFF161014),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, MedievalPalette.GoldBright),
                            modifier = Modifier.size(56.dp)
                        ) {
                            CinthiaAnimatedPortrait(
                                state = CinthiaState.CHEERING,
                                contentDescription = "Cinthia",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RECOMPENSA GARANTIZADA",
                                color = Color(0xFFD7CCC8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                GoldCoinIcon(size = 20.dp)
                                Text(
                                    text = "+$rewardCoins Monedas de Oro",
                                    color = MedievalPalette.GoldBright,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Comienza automáticamente en ${secondsLeft}s...",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Barra de cuenta regresiva
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF120C0F))
                        .border(1.dp, MedievalPalette.BrassFrame, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF43D165), Color(0xFFFFCA28))
                                )
                            )
                    )
                }

                ChunkyArcadeButton(
                    onClick = {
                        if (!hasTriggered) {
                            hasTriggered = true
                            onStartAdNow()
                        }
                    },
                    style = ArcadeButtonStyle.EMERALD_PLAY,
                    cornerRadius = 14.dp,
                    lipHeight = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VER AHORA Y GANAR +$rewardCoins ORO",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                TextButton(
                    onClick = {
                        if (!hasTriggered) {
                            hasTriggered = true
                            onSkipAd()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MedievalPalette.ParchmentTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Saltar anuncio y continuar sin bono",
                        color = MedievalPalette.ParchmentTextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
