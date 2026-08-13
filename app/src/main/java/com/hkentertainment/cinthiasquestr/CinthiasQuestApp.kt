package com.hkentertainment.cinthiasquestr

import android.app.Application
import com.hkentertainment.cinthiasquestr.data.PlayerRepository
import com.hkentertainment.cinthiasquestr.data.dataStore

class CinthiasQuestApp : Application() {
    lateinit var playerRepository: PlayerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        playerRepository = PlayerRepository(dataStore)
    }
}
