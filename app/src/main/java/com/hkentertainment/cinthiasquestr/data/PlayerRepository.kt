// Dependency Remark: implementation("androidx.datastore:datastore-preferences:1.0.0")
package com.hkemtrentainment.cinthiasquestr.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
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
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.COINS] ?: 0
            preferences[PreferencesKeys.COINS] = current + amount
        }
    }

    suspend fun spendCoins(amount: Int) {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.COINS] ?: 0
            preferences[PreferencesKeys.COINS] = maxOf(0, current - amount)
        }
    }

    suspend fun updateBoosterCount(type: BoosterType, amount: Int) {
        dataStore.edit { preferences ->
            val key = when (type) {
                BoosterType.HAMMER -> PreferencesKeys.HAMMER
                BoosterType.DYNAMITE -> PreferencesKeys.DYNAMITE
                BoosterType.SHUFFLE -> PreferencesKeys.SHUFFLE
            }
            val current = preferences[key] ?: 0
            preferences[key] = maxOf(0, current + amount)
        }
    }

    suspend fun levelUp() {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.LEVEL] ?: 1
            preferences[PreferencesKeys.LEVEL] = current + 1
        }
    }

    suspend fun completeLevelAndUnlockNext(completedLevel: Int, starsEarned: Int) {
        dataStore.edit { preferences ->
            val currentMaxLevel = preferences[PreferencesKeys.LEVEL] ?: 1
            if (completedLevel >= currentMaxLevel) {
                preferences[PreferencesKeys.LEVEL] = completedLevel + 1
            }

            val currentStarsMap = parseStars(preferences[PreferencesKeys.LEVEL_STARS]).toMutableMap()
            val existingStars = currentStarsMap[completedLevel] ?: 0
            if (starsEarned > existingStars) {
                currentStarsMap[completedLevel] = starsEarned
                preferences[PreferencesKeys.LEVEL_STARS] = formatStars(currentStarsMap)
            }
        }
    }

    private fun parseStars(starsStr: String?): Map<Int, Int> {
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

    private fun formatStars(map: Map<Int, Int>): String {
        return map.entries.joinToString(",") { "${it.key}:${it.value}" }
    }
}
