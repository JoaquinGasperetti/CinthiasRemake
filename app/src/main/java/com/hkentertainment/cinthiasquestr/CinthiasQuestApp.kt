package com.hkemtrentainment.cinthiasquestr

import android.app.Application
import com.hkemtrentainment.cinthiasquestr.data.CloudSaveManager
import com.hkemtrentainment.cinthiasquestr.data.PlayerRepository
import com.hkemtrentainment.cinthiasquestr.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CinthiasQuestApp : Application() {
    lateinit var playerRepository: PlayerRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        playerRepository = PlayerRepository(dataStore)
        CloudSaveManager.startSync(playerRepository, applicationScope)
    }
}
