package com.hkemtrentainment.cinthiasquestr

import android.app.Application
import com.hkemtrentainment.cinthiasquestr.audio.SoundManager
import com.hkemtrentainment.cinthiasquestr.data.CloudSaveManager
import com.hkemtrentainment.cinthiasquestr.data.PlayerRepository
import com.hkemtrentainment.cinthiasquestr.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CinthiasQuestApp : Application() {
    lateinit var playerRepository: PlayerRepository
        private set

    lateinit var soundManager: SoundManager
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        playerRepository = PlayerRepository(dataStore)
        soundManager = SoundManager(this)
        CloudSaveManager.startSync(playerRepository, applicationScope)

        applicationScope.launch {
            playerRepository.playerDataFlow
                .map { it.soundEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    soundManager.isSoundEnabled = enabled
                }
        }
    }

    override fun onTerminate() {
        if (::soundManager.isInitialized) {
            soundManager.release()
        }
        super.onTerminate()
    }
}
