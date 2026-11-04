package com.hkemtrentainment.cinthiasquestr.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object CloudSaveManager {
    private const val TAG = "CloudSaveManager"
    private const val USERS_COLLECTION = "users"

    private val auth by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FirebaseAuth: ${e.message}")
            null
        }
    }

    private val firestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FirebaseFirestore: ${e.message}")
            null
        }
    }

    fun startSync(repository: PlayerRepository, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val currentAuth = auth ?: return@launch
                var currentUser = currentAuth.currentUser
                if (currentUser == null) {
                    val authResult = currentAuth.signInAnonymously().await()
                    currentUser = authResult.user
                }

                val uid = currentUser?.uid ?: return@launch
                Log.d(TAG, "Authenticated anonymously with UID: $uid")

                // 1. Restaurar y fusionar datos de la nube de forma idempotente (sin duplicar monedas)
                restoreFromCloudIfNewer(uid, repository)

                // 2. Escuchar cambios locales posteriores y sincronizarlos con Firestore
                var lastSynced: PlayerData? = null
                repository.playerDataFlow
                    .distinctUntilChanged()
                    .collectLatest { playerData ->
                        if (playerData != lastSynced) {
                            lastSynced = playerData
                            saveToCloudInternal(uid, playerData)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error during cloud save sync: ${e.message}")
            }
        }
    }

    private suspend fun restoreFromCloudIfNewer(uid: String, repository: PlayerRepository) {
        val db = firestore ?: return
        try {
            val snapshot = db.collection(USERS_COLLECTION).document(uid).get().await()
            if (snapshot.exists()) {
                val cloudLevel = snapshot.getLong("level")?.toInt() ?: 1
                val cloudCoins = snapshot.getLong("coins")?.toInt() ?: 0
                val cloudHammer = snapshot.getLong("hammer")?.toInt() ?: 1
                val cloudDynamite = snapshot.getLong("dynamite")?.toInt() ?: 1
                val cloudShuffle = snapshot.getLong("shuffle")?.toInt() ?: 1
                val cloudWinStreak = snapshot.getLong("winStreak")?.toInt() ?: 0
                val cloudBestWinStreak = snapshot.getLong("bestWinStreak")?.toInt() ?: 0
                val cloudDailyLoginStreak = snapshot.getLong("dailyLoginStreak")?.toInt() ?: 1
                val cloudLastDailyClaimDay = snapshot.getLong("lastDailyClaimEpochDay") ?: 0L

                val starsMapRaw = snapshot.get("levelStars") as? Map<*, *>
                val cloudStars = starsMapRaw?.mapNotNull { (k, v) ->
                    val levelKey = k.toString().toIntOrNull()
                    val starVal = (v as? Long)?.toInt() ?: (v as? Int)
                    if (levelKey != null && starVal != null) levelKey to starVal else null
                }?.toMap() ?: emptyMap()

                Log.d(TAG, "Fetched cloud save: level=$cloudLevel, coins=$cloudCoins, stars=$cloudStars")

                repository.syncFromCloud(
                    cloudCoins = cloudCoins,
                    cloudLevel = cloudLevel,
                    cloudHammer = cloudHammer,
                    cloudDynamite = cloudDynamite,
                    cloudShuffle = cloudShuffle,
                    cloudStars = cloudStars,
                    cloudWinStreak = cloudWinStreak,
                    cloudBestWinStreak = cloudBestWinStreak,
                    cloudDailyLoginStreak = cloudDailyLoginStreak,
                    cloudLastDailyClaimDay = cloudLastDailyClaimDay
                )
            } else {
                Log.d(TAG, "No cloud save document found yet for UID: $uid")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore from cloud: ${e.message}")
        }
    }

    private fun saveToCloudInternal(uid: String, playerData: PlayerData) {
        val db = firestore ?: return
        try {
            val saveData = hashMapOf(
                "coins" to playerData.coins,
                "level" to playerData.level,
                "hammer" to playerData.hammer,
                "dynamite" to playerData.dynamite,
                "shuffle" to playerData.shuffle,
                "winStreak" to playerData.winStreak,
                "bestWinStreak" to playerData.bestWinStreak,
                "dailyLoginStreak" to playerData.dailyLoginStreak,
                "lastDailyClaimEpochDay" to playerData.lastDailyClaimEpochDay,
                "levelStars" to playerData.levelStars.mapKeys { it.key.toString() },
                "lastUpdated" to System.currentTimeMillis()
            )

            db.collection(USERS_COLLECTION)
                .document(uid)
                .set(saveData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Cloud save successfully synced to Firestore for UID: $uid")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to sync cloud save: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saving to cloud: ${e.message}")
        }
    }
}
