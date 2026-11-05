package com.hkemtrentainment.cinthiasquestr.data

/**
 * Misión diaria individual para fomentar la frecuencia de sesión (3-5 sesiones/día)
 * y la retención D1/D7/D30 mediante metas alcanzables a corto plazo.
 */
data class DailyQuest(
    val id: Int,
    val title: String,
    val description: String,
    val currentProgress: Int,
    val targetProgress: Int,
    val rewardCoins: Int,
    val isClaimed: Boolean
) {
    val isCompleted: Boolean get() = currentProgress >= targetProgress
    val progressFraction: Float
        get() = if (targetProgress > 0) (currentProgress.toFloat() / targetProgress).coerceIn(0f, 1f) else 0f
}

/**
 * Rango de Caballera (Meta-progresión por estrellas acumuladas).
 */
data class KnightRankInfo(
    val tier: Int,
    val title: String,
    val currentStars: Int,
    val nextTierStars: Int,
    val progressFraction: Float
)

data class PlayerData(
    val coins: Int = 0,
    val level: Int = 1, // Highest unlocked level
    val hammer: Int = 1,
    val dynamite: Int = 1,
    val shuffle: Int = 1,
    val levelStars: Map<Int, Int> = emptyMap(), // Map of level number -> earned stars (1..3)
    // Retención y Meta-Progresión
    val winStreak: Int = 0, // Niveles ganados de forma consecutiva (otorga Power-Ups al inicio del nivel)
    val bestWinStreak: Int = 0,
    val dailyLoginStreak: Int = 1, // Día actual de la racha diaria (1..7)
    val lastDailyClaimEpochDay: Long = 0L,
    val canClaimDailyReward: Boolean = true, // true únicamente si no se ha reclamado hoy
    val dailyMatchesCount: Int = 0,
    val dailyPowerUpsTriggered: Int = 0,
    val dailyLevelsCompleted: Int = 0,
    val dailyQuestsEpochDay: Long = 0L,
    val claimedDailyQuestsMask: Int = 0,
    val starChestClaimedTier: Int = 0, // Cada 12 estrellas desbloquea un Cofre de Estrellas Real
    // Accesibilidad y preferencias de Game Feel
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reducedMotion: Boolean = false
) {
    val totalStars: Int
        get() = levelStars.values.sum()

    val nextStarChestTarget: Int
        get() = (starChestClaimedTier + 1) * STAR_CHEST_STEP

    val canClaimStarChest: Boolean
        get() = totalStars >= nextStarChestTarget && starChestClaimedTier < MAX_STAR_CHEST_TIERS

    val starChestProgressFraction: Float
        get() {
            val prevTarget = starChestClaimedTier * STAR_CHEST_STEP
            val span = (nextStarChestTarget - prevTarget).coerceAtLeast(1)
            return ((totalStars - prevTarget).toFloat() / span).coerceIn(0f, 1f)
        }

    fun getKnightRankInfo(): KnightRankInfo {
        val stars = totalStars
        return when {
            stars < 8 -> KnightRankInfo(1, "Escudera de Bronce", stars, 8, (stars / 8f).coerceIn(0f, 1f))
            stars < 20 -> KnightRankInfo(2, "Caballera del Bosque", stars, 20, ((stars - 8) / 12f).coerceIn(0f, 1f))
            stars < 38 -> KnightRankInfo(3, "Guardiana de Cristal", stars, 38, ((stars - 20) / 18f).coerceIn(0f, 1f))
            stars < 56 -> KnightRankInfo(4, "Paladina de la Corona", stars, 56, ((stars - 38) / 18f).coerceIn(0f, 1f))
            else -> KnightRankInfo(5, "Leyenda del Reino", stars, 75, ((stars - 56) / 19f).coerceIn(0f, 1f))
        }
    }

    /**
     * Genera la lista de 3 misiones diarias activas con su progreso actual.
     */
    fun getActiveDailyQuests(): List<DailyQuest> = listOf(
        DailyQuest(
            id = 0,
            title = "Cazadora de Cascadas",
            description = "Realiza 25 combinaciones de gemas",
            currentProgress = dailyMatchesCount,
            targetProgress = 25,
            rewardCoins = 120,
            isClaimed = (claimedDailyQuestsMask and 1) != 0
        ),
        DailyQuest(
            id = 1,
            title = "Maestría Explosiva",
            description = "Activa 5 Power-Ups o Bombas",
            currentProgress = dailyPowerUpsTriggered,
            targetProgress = 5,
            rewardCoins = 150,
            isClaimed = (claimedDailyQuestsMask and 2) != 0
        ),
        DailyQuest(
            id = 2,
            title = "Paso Real",
            description = "Supera 2 niveles en cualquier mundo",
            currentProgress = dailyLevelsCompleted,
            targetProgress = 2,
            rewardCoins = 200,
            isClaimed = (claimedDailyQuestsMask and 4) != 0
        )
    )

    companion object {
        const val STAR_CHEST_STEP = 10
        const val MAX_STAR_CHEST_TIERS = 7
    }
}
