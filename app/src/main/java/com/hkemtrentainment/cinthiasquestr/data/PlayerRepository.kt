package com.hkemtrentainment.cinthiasquestr.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
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
    }

    val playerDataFlow: Flow<PlayerData> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            PlayerData(
                coins = preferences[PreferencesKeys.COINS] ?: 0,
                level = preferences[PreferencesKeys.LEVEL] ?: 1,
                hammer = preferences[PreferencesKeys.HAMMER] ?: 1,
                dynamite = preferences[PreferencesKeys.DYNAMITE] ?: 1,
                shuffle = preferences[PreferencesKeys.SHUFFLE] ?: 1,
                levelStars = parseStars(preferences[PreferencesKeys.LEVEL_STARS])
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
        dataStore.edit { preferences ->
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
        cloudStars: Map<Int, Int>
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
    }
}
