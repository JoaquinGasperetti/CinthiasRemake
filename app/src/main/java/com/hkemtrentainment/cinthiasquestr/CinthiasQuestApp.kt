package com.hkemtrentainment.cinthiasquestr

import android.app.Application
import com.hkemtrentainment.cinthiasquestr.audio.SoundManager
import com.hkemtrentainment.cinthiasquestr.data.CloudSaveManager
import com.hkemtrentainment.cinthiasquestr.data.PlayerRepository
import com.hkemtrentainment.cinthiasquestr.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
    }

    override fun onTerminate() {
        if (::soundManager.isInitialized) {
            soundManager.release()
        }
        super.onTerminate()
    }
}
