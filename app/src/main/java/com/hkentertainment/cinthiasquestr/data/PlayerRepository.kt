// Dependency Remark: implementation("androidx.datastore:datastore-preferences:1.0.0")
package com.hkentertainment.cinthiasquestr.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.hkentertainment.cinthiasquestr.ui.gameboard.BoosterType
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
                shuffle = preferences[PreferencesKeys.SHUFFLE] ?: 1
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
}
