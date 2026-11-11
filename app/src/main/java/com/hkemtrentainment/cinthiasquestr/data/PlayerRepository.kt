package com.hkemtrentainment.cinthiasquestr.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hkemtrentainment.cinthiasquestr.ui.gameboard.BoosterType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "player_prefs")

class PlayerRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val COINS = intPreferencesKey("coins")
        val LEVEL = intPreferencesKey("level")
        val HAMMER = intPreferencesKey("hammer")
        val DYNAMITE = intPreferencesKey("dynamite")
        val SHUFFLE = intPreferencesKey("shuffle")
        val LEVEL_STARS = stringPreferencesKey("level_stars")
        val WIN_STREAK = intPreferencesKey("win_streak")
        val BEST_WIN_STREAK = intPreferencesKey("best_win_streak")
        val DAILY_LOGIN_STREAK = intPreferencesKey("daily_login_streak")
        val LAST_DAILY_CLAIM_DAY = longPreferencesKey("last_daily_claim_day")
        val DAILY_MATCHES = intPreferencesKey("daily_matches")
        val DAILY_POWERUPS = intPreferencesKey("daily_powerups")
        val DAILY_LEVELS = intPreferencesKey("daily_levels")
        val DAILY_QUESTS_DAY = longPreferencesKey("daily_quests_day")
        val CLAIMED_QUESTS_MASK = intPreferencesKey("claimed_quests_mask")
        val STAR_CHEST_TIER = intPreferencesKey("star_chest_tier")
        val TUTORIAL_COMPLETED = booleanPreferencesKey("tutorial_completed")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
    }

    private fun currentEpochDay(): Long = System.currentTimeMillis() / MILLIS_PER_DAY

    val playerDataFlow: Flow<PlayerData> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val today = currentEpochDay()
            val savedQuestDay = preferences[PreferencesKeys.DAILY_QUESTS_DAY] ?: today
            val isSameQuestDay = savedQuestDay == today
            val lastClaimDay = preferences[PreferencesKeys.LAST_DAILY_CLAIM_DAY] ?: 0L
            val savedDailyStreak = preferences[PreferencesKeys.DAILY_LOGIN_STREAK] ?: 1
            val canClaimToday = evaluateCanClaimDailyReward(today, lastClaimDay)
            val effectiveDailyStreak = computeNextDailyStreak(today, lastClaimDay, savedDailyStreak)
            val starsMap = parseStars(preferences[PreferencesKeys.LEVEL_STARS])
            val savedLevel = preferences[PreferencesKeys.LEVEL] ?: 1
            // Si el jugador ya superó algún nivel previamente, damos por completado el tutorial inicial
            val tutorialDone = preferences[PreferencesKeys.TUTORIAL_COMPLETED]
                ?: (savedLevel > 1 || starsMap.isNotEmpty())

            PlayerData(
                coins = preferences[PreferencesKeys.COINS] ?: 0,
                level = savedLevel,
                hammer = preferences[PreferencesKeys.HAMMER] ?: 1,
                dynamite = preferences[PreferencesKeys.DYNAMITE] ?: 1,
                shuffle = preferences[PreferencesKeys.SHUFFLE] ?: 1,
                levelStars = starsMap,
                winStreak = preferences[PreferencesKeys.WIN_STREAK] ?: 0,
                bestWinStreak = preferences[PreferencesKeys.BEST_WIN_STREAK] ?: 0,
                dailyLoginStreak = effectiveDailyStreak,
                lastDailyClaimEpochDay = lastClaimDay,
                canClaimDailyReward = canClaimToday,
                dailyMatchesCount = if (isSameQuestDay) (preferences[PreferencesKeys.DAILY_MATCHES] ?: 0) else 0,
                dailyPowerUpsTriggered = if (isSameQuestDay) (preferences[PreferencesKeys.DAILY_POWERUPS] ?: 0) else 0,
                dailyLevelsCompleted = if (isSameQuestDay) (preferences[PreferencesKeys.DAILY_LEVELS] ?: 0) else 0,
                dailyQuestsEpochDay = today,
                claimedDailyQuestsMask = if (isSameQuestDay) (preferences[PreferencesKeys.CLAIMED_QUESTS_MASK] ?: 0) else 0,
                starChestClaimedTier = preferences[PreferencesKeys.STAR_CHEST_TIER] ?: 0,
                hasCompletedTutorial = tutorialDone,
                soundEnabled = preferences[PreferencesKeys.SOUND_ENABLED] ?: true,
                hapticsEnabled = preferences[PreferencesKeys.HAPTICS_ENABLED] ?: true,
                reducedMotion = preferences[PreferencesKeys.REDUCED_MOTION] ?: false
            )
        }

    suspend fun markTutorialCompleted() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TUTORIAL_COMPLETED] = true
        }
    }

    suspend fun addCoins(amount: Int) {
        if (amount <= 0) return
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.COINS] ?: 0
            preferences[PreferencesKeys.COINS] = current + amount
        }
    }

    suspend fun spendCoins(amount: Int): Boolean {
        if (amount <= 0) return true
        var success = false
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.COINS] ?: 0
            if (current >= amount) {
                preferences[PreferencesKeys.COINS] = current - amount
                success = true
            }
        }
        return success
    }

    suspend fun buyBooster(type: BoosterType, cost: Int): Boolean {
        if (cost < 0) return false
        var purchased = false
        dataStore.edit { preferences ->
            val currentCoins = preferences[PreferencesKeys.COINS] ?: 0
            if (currentCoins >= cost) {
                preferences[PreferencesKeys.COINS] = currentCoins - cost
                val key = when (type) {
                    BoosterType.HAMMER -> PreferencesKeys.HAMMER
                    BoosterType.DYNAMITE -> PreferencesKeys.DYNAMITE
                    BoosterType.SHUFFLE -> PreferencesKeys.SHUFFLE
                }
                val currentBooster = preferences[key] ?: 1
                preferences[key] = currentBooster + 1
                purchased = true
            }
        }
        return purchased
    }

    /**
     * Compra el Lote de Asedio Real (1 Martillo + 1 Dinamita + 1 Barajar) con descuento.
     */
    suspend fun buyRoyalBoosterBundle(bundleCost: Int = 290): Boolean {
        if (bundleCost < 0) return false
        var purchased = false
        dataStore.edit { preferences ->
            val currentCoins = preferences[PreferencesKeys.COINS] ?: 0
            if (currentCoins >= bundleCost) {
                preferences[PreferencesKeys.COINS] = currentCoins - bundleCost
                preferences[PreferencesKeys.HAMMER] = (preferences[PreferencesKeys.HAMMER] ?: 1) + 1
                preferences[PreferencesKeys.DYNAMITE] = (preferences[PreferencesKeys.DYNAMITE] ?: 1) + 1
                preferences[PreferencesKeys.SHUFFLE] = (preferences[PreferencesKeys.SHUFFLE] ?: 1) + 1
                purchased = true
            }
        }
        return purchased
    }

    /**
     * Reclama el Cofre de Estrellas Real cuando el jugador alcanza el umbral de estrellas (`nextStarChestTarget`).
     * Otorga 220 monedas + 1 booster adicional.
     */
    suspend fun claimStarChestReward(): Pair<Int, BoosterType>? {
        var result: Pair<Int, BoosterType>? = null
        dataStore.edit { preferences ->
            val starsMap = parseStars(preferences[PreferencesKeys.LEVEL_STARS])
            val totalStars = starsMap.values.sum()
            val currentTier = preferences[PreferencesKeys.STAR_CHEST_TIER] ?: 0
            val nextTarget = (currentTier + 1) * PlayerData.STAR_CHEST_STEP
            if (totalStars >= nextTarget && currentTier < PlayerData.MAX_STAR_CHEST_TIERS) {
                val coinReward = 220 + currentTier * 30
                val bonusBooster = BoosterType.entries[currentTier % BoosterType.entries.size]
                val boosterKey = when (bonusBooster) {
                    BoosterType.HAMMER -> PreferencesKeys.HAMMER
                    BoosterType.DYNAMITE -> PreferencesKeys.DYNAMITE
                    BoosterType.SHUFFLE -> PreferencesKeys.SHUFFLE
                }
                preferences[PreferencesKeys.STAR_CHEST_TIER] = currentTier + 1
                preferences[PreferencesKeys.COINS] = (preferences[PreferencesKeys.COINS] ?: 0) + coinReward
                preferences[boosterKey] = (preferences[boosterKey] ?: 1) + 1
                result = coinReward to bonusBooster
            }
        }
        return result
    }

    suspend fun updateBoosterCount(type: BoosterType, amount: Int) {
        dataStore.edit { preferences ->
            val key = when (type) {
                BoosterType.HAMMER -> PreferencesKeys.HAMMER
                BoosterType.DYNAMITE -> PreferencesKeys.DYNAMITE
                BoosterType.SHUFFLE -> PreferencesKeys.SHUFFLE
            }
            val current = preferences[key] ?: 1
            preferences[key] = maxOf(0, current + amount)
        }
    }

    suspend fun levelUp() {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.LEVEL] ?: 1
            preferences[PreferencesKeys.LEVEL] = minOf(WorldRepository.MAX_GAME_LEVELS, current + 1)
        }
    }

    suspend fun completeLevelAndUnlockNext(completedLevel: Int, starsEarned: Int) {
        val today = currentEpochDay()
        dataStore.edit { preferences ->
            ensureDailyQuestsReset(preferences, today)

            val currentMaxLevel = preferences[PreferencesKeys.LEVEL] ?: 1
            if (completedLevel >= currentMaxLevel && completedLevel < WorldRepository.MAX_GAME_LEVELS) {
                preferences[PreferencesKeys.LEVEL] = completedLevel + 1
            }

            val currentStarsMap = parseStars(preferences[PreferencesKeys.LEVEL_STARS]).toMutableMap()
            val existingStars = currentStarsMap[completedLevel] ?: 0
            if (starsEarned > existingStars) {
                currentStarsMap[completedLevel] = starsEarned.coerceIn(1, 3)
                preferences[PreferencesKeys.LEVEL_STARS] = formatStars(currentStarsMap)
            }

            preferences[PreferencesKeys.TUTORIAL_COMPLETED] = true

            // Incrementar Racha de Victorias (Win Streak) y misión diaria de niveles
            val newStreak = (preferences[PreferencesKeys.WIN_STREAK] ?: 0) + 1
            val bestStreak = maxOf(preferences[PreferencesKeys.BEST_WIN_STREAK] ?: 0, newStreak)
            preferences[PreferencesKeys.WIN_STREAK] = newStreak
            preferences[PreferencesKeys.BEST_WIN_STREAK] = bestStreak
            preferences[PreferencesKeys.DAILY_LEVELS] = (preferences[PreferencesKeys.DAILY_LEVELS] ?: 0) + 1
        }
    }

    suspend fun resetWinStreak() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIN_STREAK] = 0
        }
    }

    suspend fun restoreWinStreak(streak: Int) {
        if (streak <= 0) return
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.WIN_STREAK] ?: 0
            if (streak > current) {
                preferences[PreferencesKeys.WIN_STREAK] = streak
            }
        }
    }

    suspend fun recordMatchTelemetry(matchesMade: Int, powerUpsTriggered: Int) {
        if (matchesMade <= 0 && powerUpsTriggered <= 0) return
        val today = currentEpochDay()
        dataStore.edit { preferences ->
            ensureDailyQuestsReset(preferences, today)
            if (matchesMade > 0) {
                preferences[PreferencesKeys.DAILY_MATCHES] =
                    (preferences[PreferencesKeys.DAILY_MATCHES] ?: 0) + matchesMade
            }
            if (powerUpsTriggered > 0) {
                preferences[PreferencesKeys.DAILY_POWERUPS] =
                    (preferences[PreferencesKeys.DAILY_POWERUPS] ?: 0) + powerUpsTriggered
            }
        }
    }

    suspend fun claimDailyQuestReward(questId: Int, rewardCoins: Int): Boolean {
        if (questId !in 0..2 || rewardCoins <= 0) return false
        val bit = 1 shl questId
        val today = currentEpochDay()
        var claimed = false
        dataStore.edit { preferences ->
            ensureDailyQuestsReset(preferences, today)
            val mask = preferences[PreferencesKeys.CLAIMED_QUESTS_MASK] ?: 0
            val hasRequiredProgress = when (questId) {
                0 -> (preferences[PreferencesKeys.DAILY_MATCHES] ?: 0) >= 25
                1 -> (preferences[PreferencesKeys.DAILY_POWERUPS] ?: 0) >= 5
                2 -> (preferences[PreferencesKeys.DAILY_LEVELS] ?: 0) >= 2
                else -> false
            }
            if ((mask and bit) == 0 && hasRequiredProgress) {
                preferences[PreferencesKeys.CLAIMED_QUESTS_MASK] = mask or bit
                val currentCoins = preferences[PreferencesKeys.COINS] ?: 0
                preferences[PreferencesKeys.COINS] = currentCoins + rewardCoins
                claimed = true
            }
        }
        return claimed
    }

    /**
     * Reclama el Cofre de Racha Diaria UNA ÚNICA VEZ por día calendario (epochDay).
     * Si ya fue reclamado hoy (`today <= lastDay`), devuelve `0 to currentStreak` sin sumar monedas.
     */
    suspend fun claimDailyStreakChest(): Pair<Int, Int> {
        val today = currentEpochDay()
        var rewardGranted = 0
        var updatedStreak = 1
        dataStore.edit { preferences ->
            val lastDay = preferences[PreferencesKeys.LAST_DAILY_CLAIM_DAY] ?: 0L
            val savedStreak = preferences[PreferencesKeys.DAILY_LOGIN_STREAK] ?: 1

            if (!evaluateCanClaimDailyReward(today, lastDay)) {
                rewardGranted = 0
                updatedStreak = savedStreak
                return@edit
            }

            updatedStreak = computeNextDailyStreak(today, lastDay, savedStreak)
            rewardGranted = computeDailyStreakReward(updatedStreak)

            preferences[PreferencesKeys.DAILY_LOGIN_STREAK] = updatedStreak
            preferences[PreferencesKeys.LAST_DAILY_CLAIM_DAY] = today
            val currentCoins = preferences[PreferencesKeys.COINS] ?: 0
            preferences[PreferencesKeys.COINS] = currentCoins + rewardGranted
        }
        return rewardGranted to updatedStreak
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.SOUND_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.REDUCED_MOTION] = enabled }
    }

    private fun ensureDailyQuestsReset(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        today: Long
    ) {
        val savedDay = preferences[PreferencesKeys.DAILY_QUESTS_DAY] ?: 0L
        if (savedDay != today) {
            preferences[PreferencesKeys.DAILY_QUESTS_DAY] = today
            preferences[PreferencesKeys.DAILY_MATCHES] = 0
            preferences[PreferencesKeys.DAILY_POWERUPS] = 0
            preferences[PreferencesKeys.DAILY_LEVELS] = 0
            preferences[PreferencesKeys.CLAIMED_QUESTS_MASK] = 0
        }
    }

    /**
     * Sincroniza de forma idempotente los datos de la nube con los locales,
     * evitando duplicar monedas o reiniciar el bloqueo del cofre diario.
     */
    suspend fun syncFromCloud(
        cloudCoins: Int,
        cloudLevel: Int,
        cloudHammer: Int,
        cloudDynamite: Int,
        cloudShuffle: Int,
        cloudStars: Map<Int, Int>,
        cloudWinStreak: Int = 0,
        cloudBestWinStreak: Int = 0,
        cloudDailyLoginStreak: Int = 1,
        cloudLastDailyClaimDay: Long = 0L
    ) {
        dataStore.edit { preferences ->
            val localCoins = preferences[PreferencesKeys.COINS] ?: 0
            preferences[PreferencesKeys.COINS] = maxOf(localCoins, cloudCoins)

            val localHammer = preferences[PreferencesKeys.HAMMER] ?: 1
            preferences[PreferencesKeys.HAMMER] = maxOf(localHammer, cloudHammer)

            val localDynamite = preferences[PreferencesKeys.DYNAMITE] ?: 1
            preferences[PreferencesKeys.DYNAMITE] = maxOf(localDynamite, cloudDynamite)

            val localShuffle = preferences[PreferencesKeys.SHUFFLE] ?: 1
            preferences[PreferencesKeys.SHUFFLE] = maxOf(localShuffle, cloudShuffle)

            val localStreak = preferences[PreferencesKeys.WIN_STREAK] ?: 0
            preferences[PreferencesKeys.WIN_STREAK] = maxOf(localStreak, cloudWinStreak)

            val localBestStreak = preferences[PreferencesKeys.BEST_WIN_STREAK] ?: 0
            preferences[PreferencesKeys.BEST_WIN_STREAK] = maxOf(localBestStreak, cloudBestWinStreak)

            val localLastClaimDay = preferences[PreferencesKeys.LAST_DAILY_CLAIM_DAY] ?: 0L
            if (cloudLastDailyClaimDay > localLastClaimDay) {
                preferences[PreferencesKeys.LAST_DAILY_CLAIM_DAY] = cloudLastDailyClaimDay
                preferences[PreferencesKeys.DAILY_LOGIN_STREAK] = cloudDailyLoginStreak.coerceIn(1, 7)
            }

            val mergedStars = parseStars(preferences[PreferencesKeys.LEVEL_STARS]).toMutableMap()
            for ((lvl, stars) in cloudStars) {
                val existing = mergedStars[lvl] ?: 0
                if (stars > existing) {
                    mergedStars[lvl] = stars.coerceIn(1, 3)
                }
            }
            if (mergedStars.isNotEmpty()) {
                preferences[PreferencesKeys.LEVEL_STARS] = formatStars(mergedStars)
            }

            val highestCompleted = mergedStars.keys.maxOrNull() ?: 0
            val localLevel = preferences[PreferencesKeys.LEVEL] ?: 1
            val targetLevel = maxOf(localLevel, cloudLevel, highestCompleted + 1)
            preferences[PreferencesKeys.LEVEL] = minOf(WorldRepository.MAX_GAME_LEVELS, targetLevel)
        }
    }

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L

        internal fun parseStars(starsStr: String?): Map<Int, Int> {
            if (starsStr.isNullOrEmpty()) return emptyMap()
            return starsStr.split(",")
                .mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        val lvl = parts[0].toIntOrNull()
                        val stars = parts[1].toIntOrNull()
                        if (lvl != null && stars != null) lvl to stars else null
                    } else null
                }.toMap()
        }

        internal fun formatStars(map: Map<Int, Int>): String {
            return map.entries
                .sortedBy { it.key }
                .joinToString(",") { "${it.key}:${it.value}" }
        }

        internal fun evaluateCanClaimDailyReward(todayEpochDay: Long, lastClaimEpochDay: Long): Boolean {
            return todayEpochDay > lastClaimEpochDay
        }

        internal fun computeNextDailyStreak(
            todayEpochDay: Long,
            lastClaimEpochDay: Long,
            savedStreak: Int
        ): Int {
            val safeSaved = savedStreak.coerceIn(1, 7)
            return when {
                lastClaimEpochDay <= 0L -> 1
                todayEpochDay <= lastClaimEpochDay -> safeSaved
                todayEpochDay == lastClaimEpochDay + 1L -> (safeSaved % 7) + 1
                else -> 1
            }
        }

        internal fun computeDailyStreakReward(streakDay: Int): Int {
            val clampedDay = streakDay.coerceIn(1, 7)
            return 150 + (clampedDay - 1) * 40
        }

        internal fun computeStreakStarterPowerUpsCount(winStreak: Int): Int {
            return winStreak.coerceIn(0, 3)
        }
    }
}
