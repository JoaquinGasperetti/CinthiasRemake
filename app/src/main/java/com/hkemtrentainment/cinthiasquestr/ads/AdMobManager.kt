package com.hkemtrentainment.cinthiasquestr.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.hkemtrentainment.cinthiasquestr.audio.SoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reglas puras de frecuencia (Pacing), cumplimiento de políticas de Google AdMob
 * y protección de retención D1/D7/D30 + Android Vitals para el algoritmo de Google Play.
 */
object AdPacingPolicy {
    /** Clasificación oficial ATP (General Audiences - "G") exigida para todos los anuncios. */
    const val MAX_CONTENT_RATING_ATP = RequestConfiguration.MAX_AD_CONTENT_RATING_G

    /** IDs oficiales de producción en Google AdMob para Cinthia's Quest. */
    const val ADMOB_APP_ID = "ca-app-pub-2266949018056491~1454950722"
    const val BANNER_AD_UNIT_ID = "ca-app-pub-2266949018056491/2593606728"
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-2266949018056491/6206769867"
    const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-2266949018056491/6341280048"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-2266949018056491/3844330405"
    const val REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-2266949018056491/3527414441"

    /** Expiración máxima de anuncios App Open según Google (4 horas). */
    const val APP_OPEN_EXPIRATION_MS = 4L * 3_600_000L

    /** Expiración preventiva de caché de Interstitial / Rewarded (55 minutos; límite Google: 60 min). */
    const val FULLSCREEN_CACHE_EXPIRATION_MS = 55L * 60_000L

    /** Cooldown mínimo entre intersticiales forzados para maximizar impresiones sin fatiga ni penalización. */
    const val INTERSTITIAL_COOLDOWN_MS = 60_000L

    /** Cooldown de gracia después de que el jugador ve voluntariamente un anuncio bonificado (evita ad-stacking). */
    const val POST_REWARDED_GRACE_MS = 40_000L

    /** Cooldown mínimo entre App Open Ads al volver del segundo plano. */
    const val APP_OPEN_COOLDOWN_MS = 90_000L

    /** Mínimo de acciones de usuario requeridas por la política de AdMob antes de mostrar otro intersticial. */
    const val MIN_USER_ACTIONS_BETWEEN_INTERSTITIALS = 2

    /**
     * Evalúa si un anuncio en caché sigue vigente antes de su ventana de expiración.
     */
    fun isCacheValid(loadTimestampMs: Long, nowMs: Long, maxAgeMs: Long): Boolean {
        if (loadTimestampMs <= 0L) return false
        val age = nowMs - loadTimestampMs
        return age in 0L until maxAgeMs
    }

    /**
     * Evalúa si es seguro y conforme a las políticas mostrar un App Open Ad (Carga de aplicación):
     * - Protege la primera apertura en frío de un jugador nuevo (Onboarding D1) según las mejores prácticas de AdMob.
     * - Nunca superpone sobre otro anuncio a pantalla completa.
     * - Respeta el vencimiento de 4 horas y el cooldown de retorno al primer plano.
     */
    fun canShowAppOpenAd(
        isAdLoadedAndValid: Boolean,
        isShowingFullScreenAd: Boolean,
        foregroundEventCount: Int,
        hasCompletedTutorial: Boolean,
        lastFullScreenDismissedMs: Long,
        nowMs: Long
    ): Boolean {
        if (!isAdLoadedAndValid || isShowingFullScreenAd) return false
        // Proteger el primer arranque del jugador nuevo para no dañar la retención D1
        if (foregroundEventCount <= 1 && !hasCompletedTutorial) return false
        if (lastFullScreenDismissedMs > 0L && (nowMs - lastFullScreenDismissedMs) < APP_OPEN_COOLDOWN_MS) {
            return false
        }
        return true
    }

    /**
     * Evalúa si se puede mostrar un Intersticial en una pausa/transición natural entre niveles:
     * - Protege el Nivel 1 (mientras el usuario aprende el tutorial básico) para cuidar la retención D1.
     * - Exige al menos 2 acciones de usuario desde el último intersticial (política oficial de Google AdMob).
     * - Evita mostrar un intersticial inmediatamente después de que el usuario vio un anuncio bonificado.
     * - Respeta el cooldown mínimo entre intersticiales.
     */
    fun canShowInterstitialAd(
        isAdLoadedAndValid: Boolean,
        isShowingFullScreenAd: Boolean,
        currentLevel: Int,
        userActionsSinceLastInterstitial: Int,
        lastInterstitialShownMs: Long,
        lastRewardedShownMs: Long,
        nowMs: Long
    ): Boolean {
        if (!isAdLoadedAndValid || isShowingFullScreenAd) return false
        if (currentLevel < 2) return false
        if (userActionsSinceLastInterstitial < MIN_USER_ACTIONS_BETWEEN_INTERSTITIALS) return false
        if (lastInterstitialShownMs > 0L && (nowMs - lastInterstitialShownMs) < INTERSTITIAL_COOLDOWN_MS) {
            return false
        }
        if (lastRewardedShownMs > 0L && (nowMs - lastRewardedShownMs) < POST_REWARDED_GRACE_MS) {
            return false
        }
        return true
    }

    /**
     * Evalúa si conviene ofrecer automáticamente el diálogo introductorio de un Intersticial Bonificado
     * durante una transición de nivel cuando el Intersticial normal no se disparó (maximiza fill rate e ingresos
     * ofreciendo una recompensa real con opción de saltar).
     */
    fun shouldOfferTransitionRewardedInterstitial(
        isRewardedInterstitialLoadedAndValid: Boolean,
        isShowingFullScreenAd: Boolean,
        currentLevel: Int,
        lastRewardedShownMs: Long,
        nowMs: Long
    ): Boolean {
        if (!isRewardedInterstitialLoadedAndValid || isShowingFullScreenAd) return false
        if (currentLevel < 2) return false
        if (lastRewardedShownMs > 0L && (nowMs - lastRewardedShownMs) < POST_REWARDED_GRACE_MS) {
            return false
        }
        return true
    }
}

/**
 * Gestor centralizado de Google AdMob para Cinthia's Quest:
 * - Configura estrictamente todos los anuncios como ATP (`MAX_AD_CONTENT_RATING_G`) antes de inicializar el SDK.
 * - Pre-carga en segundo plano los 4 formatos de pantalla completa (`AppOpen`, `Interstitial`, `Rewarded`,
 *   `RewardedInterstitial`) y provee solicitudes ATP para `Banner`.
 * - Sincroniza el silenciamiento automático del `SoundManager` mientras hay un anuncio en pantalla.
 */
class AdMobManager(
    private val application: Application,
    private val soundManager: SoundManager,
    private val appScope: CoroutineScope
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val isSdkInitialized = AtomicBoolean(false)

    private var currentActivityRef: WeakReference<Activity>? = null

    // Estado de anuncios a pantalla completa para evitar solapamientos
    @Volatile
    var isShowingFullScreenAd: Boolean = false
        private set

    // Contadores de telemetría de sesión y pacing
    private var foregroundEventCount: Int = 0
    private var userActionsSinceLastInterstitial: Int = 2 // Permite el primer intersticial tras superar Nivel 2+
    private var lastInterstitialShownMs: Long = 0L
    private var lastRewardedShownMs: Long = 0L
    private var lastFullScreenDismissedMs: Long = 0L

    @Volatile
    var hasCompletedTutorialFlag: Boolean = false

    // 1. App Open Ad (Carga de aplicación)
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAppOpen: Boolean = false
    private var appOpenLoadTimeMs: Long = 0L

    // 2. Interstitial Ad (Intersticial)
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial: Boolean = false
    private var interstitialLoadTimeMs: Long = 0L

    // 3. Rewarded Ad (Bonificado)
    private var rewardedAd: RewardedAd? = null
    private var isLoadingRewarded: Boolean = false
    private var rewardedLoadTimeMs: Long = 0L

    private val _isRewardedAdReady = MutableStateFlow(false)
    val isRewardedAdReady: StateFlow<Boolean> = _isRewardedAdReady.asStateFlow()

    // 4. Rewarded Interstitial Ad (Intersticial bonificado)
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isLoadingRewardedInterstitial: Boolean = false
    private var rewardedInterstitialLoadTimeMs: Long = 0L

    private val _isRewardedInterstitialReady = MutableStateFlow(false)
    val isRewardedInterstitialReady: StateFlow<Boolean> = _isRewardedInterstitialReady.asStateFlow()

    /**
     * Registra observadores de ciclo de vida, aplica el filtro global ATP (`MAX_AD_CONTENT_RATING_G`)
     * e inicializa el SDK de Google Mobile Ads sin bloquear el hilo principal (protegiendo el ANR rate).
     */
    fun initialize() {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // 1. Configurar globalmente clasificación ATP (General Audiences - "G") ANTES de MobileAds.initialize()
        // Ref: https://developers.google.com/admob/android/targeting
        try {
            val requestConfiguration = MobileAds.getRequestConfiguration()
                .toBuilder()
                .setMaxAdContentRating(AdPacingPolicy.MAX_CONTENT_RATING_ATP)
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo aplicar RequestConfiguration inicial: ${e.message}")
        }

        // 2. Inicializar Google Mobile Ads SDK en hilo de I/O y luego pre-cargar en el hilo principal
        appScope.launch(Dispatchers.IO) {
            try {
                MobileAds.initialize(application) { initStatus ->
                    isSdkInitialized.set(true)
                    Log.d(TAG, "AdMob SDK inicializado (ATP/G activo): ${initStatus.adapterStatusMap.keys}")
                    mainHandler.post {
                        preloadAllFormats()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando MobileAds SDK: ${e.message}")
            }
        }
    }

    /**
     * Construye un `AdRequest` con clasificación máxima de contenido `G` (ATP - Apto para Todo Público)
     * tanto en la configuración global como en los extras de red del adaptador AdMob.
     */
    fun buildAtpAdRequest(): AdRequest {
        val extras = Bundle().apply {
            putString("max_ad_content_rating", AdPacingPolicy.MAX_CONTENT_RATING_ATP)
        }
        return AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            .build()
    }

    /**
     * Registra una acción significativa de navegación o interacción del jugador
     * para cumplir la regla de AdMob de mínimo 2 acciones entre intersticiales.
     */
    fun recordUserAction() {
        userActionsSinceLastInterstitial = (userActionsSinceLastInterstitial + 1).coerceAtMost(100)
    }

    fun preloadAllFormats() {
        mainHandler.post {
            loadAppOpenAd()
            loadInterstitialAd()
            loadRewardedAd()
            loadRewardedInterstitialAd()
        }
    }

    // =========================================================================
    // 1. CARGA DE APLICACIÓN (APP OPEN AD - ca-app-pub-2266949018056491/6341280048)
    // =========================================================================

    fun loadAppOpenAd() {
        if (isLoadingAppOpen || isAppOpenAdValid()) return
        isLoadingAppOpen = true

        AppOpenAd.load(
            application,
            AdPacingPolicy.APP_OPEN_AD_UNIT_ID,
            buildAtpAdRequest(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    appOpenLoadTimeMs = System.currentTimeMillis()
                    isLoadingAppOpen = false
                    Log.d(TAG, "AppOpenAd cargado y listo.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                    isLoadingAppOpen = false
                    Log.d(TAG, "AppOpenAd falló al cargar: ${loadAdError.message}")
                }
            }
        )
    }

    private fun isAppOpenAdValid(): Boolean {
        val valid = appOpenAd != null && AdPacingPolicy.isCacheValid(
            loadTimestampMs = appOpenLoadTimeMs,
            nowMs = System.currentTimeMillis(),
            maxAgeMs = AdPacingPolicy.APP_OPEN_EXPIRATION_MS
        )
        if (!valid && appOpenAd != null) {
            appOpenAd = null
        }
        return valid
    }

    fun showAppOpenAdIfAvailable(activity: Activity, onComplete: () -> Unit = {}) {
        val now = System.currentTimeMillis()
        if (!AdPacingPolicy.canShowAppOpenAd(
                isAdLoadedAndValid = isAppOpenAdValid(),
                isShowingFullScreenAd = isShowingFullScreenAd,
                foregroundEventCount = foregroundEventCount,
                hasCompletedTutorial = hasCompletedTutorialFlag,
                lastFullScreenDismissedMs = lastFullScreenDismissedMs,
                nowMs = now
            )
        ) {
            if (!isAppOpenAdValid()) {
                loadAppOpenAd()
            }
            onComplete()
            return
        }

        val ad = appOpenAd ?: run {
            loadAppOpenAd()
            onComplete()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingFullScreenAd = true
                soundManager.setAdMuted(true)
            }

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingFullScreenAd = false
                lastFullScreenDismissedMs = System.currentTimeMillis()
                soundManager.setAdMuted(false)
                loadAppOpenAd()
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingFullScreenAd = false
                soundManager.setAdMuted(false)
                loadAppOpenAd()
                onComplete()
            }
        }

        isShowingFullScreenAd = true
        ad.show(activity)
    }

    // =========================================================================
    // 2. INTERSTICIAL (INTERSTITIAL AD - ca-app-pub-2266949018056491/3844330405)
    // =========================================================================

    fun loadInterstitialAd() {
        if (isLoadingInterstitial || isInterstitialAdValid()) return
        isLoadingInterstitial = true

        InterstitialAd.load(
            application,
            AdPacingPolicy.INTERSTITIAL_AD_UNIT_ID,
            buildAtpAdRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    interstitialLoadTimeMs = System.currentTimeMillis()
                    isLoadingInterstitial = false
                    Log.d(TAG, "InterstitialAd cargado y listo.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                    Log.d(TAG, "InterstitialAd falló al cargar: ${loadAdError.message}")
                }
            }
        )
    }

    private fun isInterstitialAdValid(): Boolean {
        val valid = interstitialAd != null && AdPacingPolicy.isCacheValid(
            loadTimestampMs = interstitialLoadTimeMs,
            nowMs = System.currentTimeMillis(),
            maxAgeMs = AdPacingPolicy.FULLSCREEN_CACHE_EXPIRATION_MS
        )
        if (!valid && interstitialAd != null) {
            interstitialAd = null
        }
        return valid
    }

    /**
     * Muestra un anuncio intersticial en una pausa/transición natural (ej. Siguiente Nivel, Reintentar,
     * Volver al Mapa) únicamente si cumple las políticas de AdMob y las reglas de retención.
     * Devuelve `true` si el anuncio se mostró, o `false` si se ejecutó `onComplete` directamente.
     */
    fun showInterstitialIfEligible(
        activity: Activity?,
        currentLevel: Int,
        onAdDismissed: () -> Unit
    ): Boolean {
        recordUserAction()
        if (activity == null || activity.isFinishing) {
            return false
        }

        val now = System.currentTimeMillis()
        if (!AdPacingPolicy.canShowInterstitialAd(
                isAdLoadedAndValid = isInterstitialAdValid(),
                isShowingFullScreenAd = isShowingFullScreenAd,
                currentLevel = currentLevel,
                userActionsSinceLastInterstitial = userActionsSinceLastInterstitial,
                lastInterstitialShownMs = lastInterstitialShownMs,
                lastRewardedShownMs = lastRewardedShownMs,
                nowMs = now
            )
        ) {
            if (!isInterstitialAdValid()) {
                loadInterstitialAd()
            }
            return false
        }

        val ad = interstitialAd ?: run {
            loadInterstitialAd()
            return false
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingFullScreenAd = true
                lastInterstitialShownMs = System.currentTimeMillis()
                userActionsSinceLastInterstitial = 0
                soundManager.setAdMuted(true)
            }

            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                isShowingFullScreenAd = false
                lastFullScreenDismissedMs = System.currentTimeMillis()
                soundManager.setAdMuted(false)
                loadInterstitialAd()
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                isShowingFullScreenAd = false
                soundManager.setAdMuted(false)
                loadInterstitialAd()
                onAdDismissed()
            }
        }

        isShowingFullScreenAd = true
        ad.show(activity)
        return true
    }

    // =========================================================================
    // 3. BONIFICADO (REWARDED AD - ca-app-pub-2266949018056491/6206769867)
    // =========================================================================

    fun loadRewardedAd() {
        if (isLoadingRewarded || isRewardedAdValid()) {
            _isRewardedAdReady.value = isRewardedAdValid()
            return
        }
        isLoadingRewarded = true

        RewardedAd.load(
            application,
            AdPacingPolicy.REWARDED_AD_UNIT_ID,
            buildAtpAdRequest(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    rewardedLoadTimeMs = System.currentTimeMillis()
                    isLoadingRewarded = false
                    _isRewardedAdReady.value = true
                    Log.d(TAG, "RewardedAd cargado y listo.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isLoadingRewarded = false
                    _isRewardedAdReady.value = false
                    Log.d(TAG, "RewardedAd falló al cargar: ${loadAdError.message}")
                }
            }
        )
    }

    private fun isRewardedAdValid(): Boolean {
        val valid = rewardedAd != null && AdPacingPolicy.isCacheValid(
            loadTimestampMs = rewardedLoadTimeMs,
            nowMs = System.currentTimeMillis(),
            maxAgeMs = AdPacingPolicy.FULLSCREEN_CACHE_EXPIRATION_MS
        )
        if (!valid && rewardedAd != null) {
            rewardedAd = null
            _isRewardedAdReady.value = false
        }
        return valid
    }

    /**
     * Muestra un anuncio de video bonificado (Opt-In voluntario del jugador: +5 Movimientos, Duplicar Monedas,
     * Cofre Gratis de la Armería o Bono Diario Extra).
     * Si el anuncio bonificado principal no está en caché pero el Intersticial Bonificado sí lo está,
     * utiliza el Intersticial Bonificado como respaldo para maximizar el fill rate sin frustrar al jugador.
     */
    fun showRewardedAd(
        activity: Activity?,
        onUserEarnedReward: () -> Unit,
        onAdUnavailable: () -> Unit = {},
        onDismissed: () -> Unit = {}
    ) {
        recordUserAction()
        if (activity == null || activity.isFinishing || isShowingFullScreenAd) {
            onAdUnavailable()
            return
        }

        val ad = if (isRewardedAdValid()) rewardedAd else null
        if (ad == null) {
            loadRewardedAd()
            // Fallback inteligente al Intersticial Bonificado si está disponible en caché
            if (isRewardedInterstitialAdValid()) {
                showRewardedInterstitialAd(
                    activity = activity,
                    onUserEarnedReward = onUserEarnedReward,
                    onAdUnavailable = onAdUnavailable,
                    onDismissed = onDismissed
                )
            } else {
                loadRewardedInterstitialAd()
                onAdUnavailable()
            }
            return
        }

        var rewardGranted = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingFullScreenAd = true
                lastRewardedShownMs = System.currentTimeMillis()
                _isRewardedAdReady.value = false
                soundManager.setAdMuted(true)
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                isShowingFullScreenAd = false
                lastFullScreenDismissedMs = System.currentTimeMillis()
                soundManager.setAdMuted(false)
                loadRewardedAd()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                isShowingFullScreenAd = false
                _isRewardedAdReady.value = false
                soundManager.setAdMuted(false)
                loadRewardedAd()
                if (!rewardGranted) {
                    onAdUnavailable()
                }
            }
        }

        isShowingFullScreenAd = true
        ad.show(activity) { _ ->
            rewardGranted = true
            lastRewardedShownMs = System.currentTimeMillis()
            onUserEarnedReward()
        }
    }

    // =========================================================================
    // 4. INTERSTICIAL BONIFICADO (REWARDED INTERSTITIAL - ca-app-pub-2266949018056491/3527414441)
    // =========================================================================

    fun loadRewardedInterstitialAd() {
        if (isLoadingRewardedInterstitial || isRewardedInterstitialAdValid()) {
            _isRewardedInterstitialReady.value = isRewardedInterstitialAdValid()
            return
        }
        isLoadingRewardedInterstitial = true

        RewardedInterstitialAd.load(
            application,
            AdPacingPolicy.REWARDED_INTERSTITIAL_AD_UNIT_ID,
            buildAtpAdRequest(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    rewardedInterstitialLoadTimeMs = System.currentTimeMillis()
                    isLoadingRewardedInterstitial = false
                    _isRewardedInterstitialReady.value = true
                    Log.d(TAG, "RewardedInterstitialAd cargado y listo.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedInterstitialAd = null
                    isLoadingRewardedInterstitial = false
                    _isRewardedInterstitialReady.value = false
                    Log.d(TAG, "RewardedInterstitialAd falló al cargar: ${loadAdError.message}")
                }
            }
        )
    }

    fun isRewardedInterstitialAdValid(): Boolean {
        val valid = rewardedInterstitialAd != null && AdPacingPolicy.isCacheValid(
            loadTimestampMs = rewardedInterstitialLoadTimeMs,
            nowMs = System.currentTimeMillis(),
            maxAgeMs = AdPacingPolicy.FULLSCREEN_CACHE_EXPIRATION_MS
        )
        if (!valid && rewardedInterstitialAd != null) {
            rewardedInterstitialAd = null
            _isRewardedInterstitialReady.value = false
        }
        return valid
    }

    /**
     * Verifica si en una transición entre niveles se debe presentar el diálogo de introducción
     * del Intersticial Bonificado (cuando el Intersticial estándar no aplica pero hay un
     * Intersticial Bonificado listo).
     */
    fun canOfferTransitionRewardedInterstitial(currentLevel: Int): Boolean {
        return AdPacingPolicy.shouldOfferTransitionRewardedInterstitial(
            isRewardedInterstitialLoadedAndValid = isRewardedInterstitialAdValid(),
            isShowingFullScreenAd = isShowingFullScreenAd,
            currentLevel = currentLevel,
            lastRewardedShownMs = lastRewardedShownMs,
            nowMs = System.currentTimeMillis()
        )
    }

    /**
     * Muestra el Intersticial Bonificado después de que el usuario vio el diálogo de introducción
     * obligatorio (`RewardedInterstitialIntroDialog`) sin saltarlo.
     */
    fun showRewardedInterstitialAd(
        activity: Activity?,
        onUserEarnedReward: () -> Unit,
        onAdUnavailable: () -> Unit = {},
        onDismissed: () -> Unit = {}
    ) {
        recordUserAction()
        if (activity == null || activity.isFinishing || isShowingFullScreenAd) {
            onAdUnavailable()
            return
        }

        val ad = if (isRewardedInterstitialAdValid()) rewardedInterstitialAd else null
        if (ad == null) {
            loadRewardedInterstitialAd()
            onAdUnavailable()
            return
        }

        var rewardGranted = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingFullScreenAd = true
                lastRewardedShownMs = System.currentTimeMillis()
                _isRewardedInterstitialReady.value = false
                soundManager.setAdMuted(true)
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null
                isShowingFullScreenAd = false
                lastFullScreenDismissedMs = System.currentTimeMillis()
                soundManager.setAdMuted(false)
                loadRewardedInterstitialAd()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedInterstitialAd = null
                isShowingFullScreenAd = false
                _isRewardedInterstitialReady.value = false
                soundManager.setAdMuted(false)
                loadRewardedInterstitialAd()
                if (!rewardGranted) {
                    onAdUnavailable()
                }
                onDismissed()
            }
        }

        isShowingFullScreenAd = true
        ad.show(activity) { _ ->
            rewardGranted = true
            lastRewardedShownMs = System.currentTimeMillis()
            onUserEarnedReward()
        }
    }

    // =========================================================================
    // CICLO DE VIDA DE APLICACIÓN Y ACTIVIDAD (PARA APP OPEN ADS)
    // =========================================================================

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        foregroundEventCount++
        val activity = currentActivityRef?.get()
        if (activity != null && !activity.isFinishing) {
            showAppOpenAdIfAvailable(activity)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (!isShowingFullScreenAd) {
            currentActivityRef = WeakReference(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (!isShowingFullScreenAd) {
            currentActivityRef = WeakReference(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivityRef?.get() === activity) {
            currentActivityRef = null
        }
    }

    companion object {
        private const val TAG = "AdMobManager"
    }
}

/**
 * Extensión segura para obtener la `Activity` anfitriona desde cualquier `Context` de Jetpack Compose.
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
