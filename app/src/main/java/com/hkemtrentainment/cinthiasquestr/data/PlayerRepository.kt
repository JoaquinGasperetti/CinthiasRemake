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

            PlayerData(
                coins = preferences[PreferencesKeys.COINS] ?: 0,
                level = preferences[PreferencesKeys.LEVEL] ?: 1,
                hammer = preferences[PreferencesKeys.HAMMER] ?: 1,
                dynamite = preferences[PreferencesKeys.DYNAMITE] ?: 1,
                shuffle = preferences[PreferencesKeys.SHUFFLE] ?: 1,
                levelStars = parseStars(preferences[PreferencesKeys.LEVEL_STARS]),
                winStreak = preferences[PreferencesKeys.WIN_STREAK] ?: 0,
                bestWinStreak = preferences[PreferencesKeys.BEST_WIN_STREAK] ?: 0,
                dailyLoginStreak = preferences[PreferencesKeys.DAILY_LOGIN_STREAK] ?: 1,
                lastDailyClaimEpochDay = preferences[PreferencesKeys.LAST_DAILY_CLAIM_DAY] ?: 0L,
                dailyMatchesCount = if (isSameQuestDay) (preferences[PreferencesKeys.DAILY_MATCHES] ?: 0) else 0,
                dailyPowerUpsTriggered = if (isSameQuestDay) (preferences[PreferencesKeys.DAILY_POWERUPS] ?: 0) else 0,
                dailyLevelsCompleted = if (isSameQuestDay) (preferences[PreferencesKeys.DAILY_LEVELS] ?: 0) else 0,
                dailyQuestsEpochDay = today,
                claimedDailyQuestsMask = if (isSameQuestDay) (preferences[PreferencesKeys.CLAIMED_QUESTS_MASK] ?: 0) else 0,
                soundEnabled = preferences[PreferencesKeys.SOUND_ENABLED] ?: true,
                hapticsEnabled = preferences[PreferencesKeys.HAPTICS_ENABLED] ?: true,
                reducedMotion = preferences[PreferencesKeys.REDUCED_MOTION] ?: false
            )
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
        val bit = 1 shl questId
        val today = currentEpochDay()
        var claimed = false
        dataStore.edit { preferences ->
            ensureDailyQuestsReset(preferences, today)
            val mask = preferences[PreferencesKeys.CLAIMED_QUESTS_MASK] ?: 0
            if ((mask and bit) == 0) {
                preferences[PreferencesKeys.CLAIMED_QUESTS_MASK] = mask or bit
                val currentCoins = preferences[PreferencesKeys.COINS] ?: 0
                preferences[PreferencesKeys.COINS] = currentCoins + rewardCoins
                claimed = true
            }
        }
        return claimed
    }

    /**
     * Reclama el Cofre de Racha Diaria (escalando la recompensa según los días consecutivos).
     * Si ya fue reclamado hoy, otorga una bonificación menor de cortesía o informa el estado.
     */
    suspend fun claimDailyStreakChest(): Pair<Int, Int> {
        val today = currentEpochDay()
        var rewardGranted = 0
        var updatedStreak = 1
        dataStore.edit { preferences ->
            val lastDay = preferences[PreferencesKeys.LAST_DAILY_CLAIM_DAY] ?: 0L
            val currentStreak = preferences[PreferencesKeys.DAILY_LOGIN_STREAK] ?: 1

            updatedStreak = when {
                lastDay == 0L -> 1
                today == lastDay -> currentStreak
                today == lastDay + 1L -> (currentStreak % 7) + 1
                else -> 1
            }

            rewardGranted = if (today > lastDay) {
                computeDailyStreakReward(updatedStreak)
            } else {
                // Bonificación de recarga rápida si el jugador necesita monedas el mismo día
                100
            }

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
     * evitando duplicar monedas en cada inicio de la aplicación.
     */
    suspend fun syncFromCloud(
        cloudCoins: Int,
        cloudLevel: Int,
        cloudHammer: Int,
        cloudDynamite: Int,
        cloudShuffle: Int,
        cloudStars: Map<Int, Int>,
        cloudWinStreak: Int = 0,
        cloudBestWinStreak: Int = 0
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

        /**
         * Calcula la recompensa de monedas escalada por día de racha (Día 1..7).
         */
        internal fun computeDailyStreakReward(streakDay: Int): Int {
            val clampedDay = streakDay.coerceIn(1, 7)
            return 150 + (clampedDay - 1) * 40
        }

        /**
         * Determina cuántos Power-Ups iniciales gratis recibe el jugador al empezar un nivel
         * según su Racha de Victorias (Win Streak) actual (0..3 máx).
         */
        internal fun computeStreakStarterPowerUpsCount(winStreak: Int): Int {
            return winStreak.coerceIn(0, 3)
        }
    }
}
