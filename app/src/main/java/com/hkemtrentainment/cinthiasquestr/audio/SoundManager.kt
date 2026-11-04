package com.hkemtrentainment.cinthiasquestr.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Catálogo completo de los 33 archivos de sonido del proyecto.
 * Soporta tanto los nombres originales en `app/src/main/assets/sounds/` (o `assets/Sonidos/`)
 * como nombres normalizados en minúsculas en `app/src/main/res/raw/`.
 */
enum class SoundAsset(
    val fileName: String,
    val rawResName: String,
    val useMediaPlayer: Boolean = false
) {
    // Medallas / Estrellas al ganar el nivel
    AWARD_BRONZE("Award_Bronze.WAV", "award_bronze", useMediaPlayer = true),
    AWARD_SILVER("Award_Silver.WAV", "award_silver", useMediaPlayer = true),
    AWARD_GOLD("Award_Gold.WAV", "award_gold", useMediaPlayer = true),

    // Creación y explosión de bombas / power-ups
    BOMB_POWER_FULL("BombPower_Full.WAV", "bombpower_full"),
    BOMB_POWER_RISE("BombPower_Rise.WAV", "bombpower_rise"),
    BOMB_EXPLODE("Bomb_Explode.WAV", "bomb_explode"),
    BOMB_EXPLODE_RED("Bomb_ExplodeRed.WAV", "bomb_explodered"),

    // Reacciones del público y música de victoria/derrota
    CROWD_DEFEAT("Crowd_Defeat.WAV", "crowd_defeat", useMediaPlayer = true),
    CROWD_VICTORY("Crowd_Victory.WAV", "crowd_victory", useMediaPlayer = true),
    GAME_LOST("Game_Lost.WAV", "game_lost", useMediaPlayer = true),
    GAME_WON("Game_Won.WAV", "game_won", useMediaPlayer = true),

    // Caída / aterrizaje de gemas
    GEM_LAND_01("Gem_Land01.WAV", "gem_land01"),
    GEM_LAND_02("Gem_Land02.WAV", "gem_land02"),
    GEM_LAND_03("Gem_Land03.WAV", "gem_land03"),

    // Voces de celebración y ánimo de Cinthia (Knight)
    KNIGHT_CHEER_AWESOME_01("Knight_Cheer_Awesome01.WAV", "knight_cheer_awesome01"),
    KNIGHT_CHEER_AWESOME_02("Knight_Cheer_Awesome02.WAV", "knight_cheer_awesome02"),
    KNIGHT_CHEER_GOOD_JOB_01("Knight_Cheer_GoodJob01.WAV", "knight_cheer_goodjob01"),
    KNIGHT_CHEER_GREAT("Knight_Cheer_Great.WAV", "knight_cheer_great"),
    KNIGHT_CHEER_KEEP_GOING("Knight_Cheer_KeepGoing.WAV", "knight_cheer_keepgoing"),
    KNIGHT_CHEER_WAY_TO_GO_01("Knight_Cheer_WayToGo01.WAV", "knight_cheer_waytogo01"),
    KNIGHT_CHEER_WAY_TO_GO_02("Knight_Cheer_WayToGo02.WAV", "knight_cheer_waytogo02"),
    KNIGHT_CHEER_WOOHOO("Knight_Cheer_Woohoo.WAV", "knight_cheer_woohoo"),
    KNIGHT_CHEER_YAY("Knight_Cheer_Yay.WAV", "knight_cheer_yay"),
    KNIGHT_CHEER_YAY_YOU_DID_IT("Knight_Cheer_YayYouDidIt.WAV", "knight_cheer_yayyoudidit"),

    // Combinaciones de fichas y golpes
    MATCH_COMBO("Match_Combo.WAV", "match_combo"),
    MATCH_STONE("Match_Stone.WAV", "match_stone"),
    MATCH_SUCCESS_GEM("Match_SuccessGem.WAV", "match_successgem"),
    MATCH_TRIPLE("Match_Triple.WAV", "match_triple"),

    // Interacción con fichas (selección, deselección, movimiento inválido)
    TILE_DESELECT("Tile_Deselect.WAV", "tile_deselect"),
    TILE_INVALID("Tile_Invalid.WAV", "tile_invalid"),
    TILE_SELECT("Tile_Select.WAV", "tile_select"),

    // Alerta de pocos movimientos restantes
    TIMER_COUNT("Timer_Count.WAV", "timer_count"),
    TIMER_COUNT_FINAL("Timer_CountFinal.WAV", "timer_countfinal")
}

/**
 * Eventos semánticos de sonido que puede disparar el juego.
 */
enum class GameSound {
    TILE_SELECT,
    TILE_DESELECT,
    TILE_INVALID,
    GEM_LAND,
    MATCH_TRIPLE,
    MATCH_SUCCESS_GEM,
    MATCH_COMBO,
    MATCH_STONE,
    BOMB_POWER_RISE,
    BOMB_POWER_FULL,
    BOMB_EXPLODE,
    BOMB_EXPLODE_RED,
    KNIGHT_CHEER_COMBO,
    KNIGHT_CHEER_KEEP_GOING,
    KNIGHT_CHEER_VICTORY,
    TIMER_COUNT,
    TIMER_COUNT_FINAL,
    GAME_WON,
    CROWD_VICTORY,
    GAME_LOST,
    CROWD_DEFEAT,
    AWARD_BRONZE,
    AWARD_SILVER,
    AWARD_GOLD
}

class SoundManager(context: Context) {
    private val appContext = context.applicationContext

    private val soundPool: SoundPool
    private val soundPoolIds = ConcurrentHashMap<SoundAsset, Int>()
    private val loadedSoundIds = ConcurrentHashMap.newKeySet<Int>()
    private val activeMediaPlayers = ConcurrentHashMap.newKeySet<MediaPlayer>()

    private val gemLandPool = listOf(
        SoundAsset.GEM_LAND_01,
        SoundAsset.GEM_LAND_02,
        SoundAsset.GEM_LAND_03
    )

    private val comboCheerPool = listOf(
        SoundAsset.KNIGHT_CHEER_AWESOME_01,
        SoundAsset.KNIGHT_CHEER_AWESOME_02,
        SoundAsset.KNIGHT_CHEER_GOOD_JOB_01,
        SoundAsset.KNIGHT_CHEER_GREAT,
        SoundAsset.KNIGHT_CHEER_WAY_TO_GO_01,
        SoundAsset.KNIGHT_CHEER_WAY_TO_GO_02,
        SoundAsset.KNIGHT_CHEER_WOOHOO,
        SoundAsset.KNIGHT_CHEER_YAY
    )

    var isSoundEnabled: Boolean = true

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds.add(sampleId)
            }
        }

        preloadShortSounds()
    }

    private fun preloadShortSounds() {
        SoundAsset.entries
            .filter { !it.useMediaPlayer }
            .forEach { asset ->
                openAssetFd(asset)?.use { afd ->
                    val sampleId = soundPool.load(afd, 1)
                    if (sampleId != 0) {
                        soundPoolIds[asset] = sampleId
                    }
                }
            }
    }

    /**
     * Busca el archivo de audio tanto en `app/src/main/assets/sounds/` (o `assets/Sonidos/`, `assets/`)
     * con su nombre original (`Award_Bronze.WAV`) como en `app/src/main/res/raw/` en minúsculas (`award_bronze.wav`).
     */
    private fun openAssetFd(asset: SoundAsset): AssetFileDescriptor? {
        val candidateAssetPaths = listOf(
            "sounds/${asset.fileName}",
            "sounds/${asset.fileName.lowercase()}",
            "Sonidos/${asset.fileName}",
            "sonidos/${asset.fileName.lowercase()}",
            asset.fileName,
            asset.fileName.lowercase()
        )

        for (path in candidateAssetPaths) {
            try {
                return appContext.assets.openFd(path)
            } catch (_: Exception) {
                // Continuar probando la siguiente ruta candidata
            }
        }

        try {
            val resId = appContext.resources.getIdentifier(
                asset.rawResName,
                "raw",
                appContext.packageName
            )
            if (resId != 0) {
                return appContext.resources.openRawResourceFd(resId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo abrir el recurso raw ${asset.rawResName}", e)
        }

        Log.w(TAG, "Archivo de audio no encontrado para ${asset.fileName}")
        return null
    }

    fun play(sound: GameSound, volume: Float = 1.0f, rate: Float = 1.0f) {
        if (!isSoundEnabled) return

        val asset = when (sound) {
            GameSound.TILE_SELECT -> SoundAsset.TILE_SELECT
            GameSound.TILE_DESELECT -> SoundAsset.TILE_DESELECT
            GameSound.TILE_INVALID -> SoundAsset.TILE_INVALID
            GameSound.GEM_LAND -> gemLandPool[Random.nextInt(gemLandPool.size)]
            GameSound.MATCH_TRIPLE -> SoundAsset.MATCH_TRIPLE
            GameSound.MATCH_SUCCESS_GEM -> SoundAsset.MATCH_SUCCESS_GEM
            GameSound.MATCH_COMBO -> SoundAsset.MATCH_COMBO
            GameSound.MATCH_STONE -> SoundAsset.MATCH_STONE
            GameSound.BOMB_POWER_RISE -> SoundAsset.BOMB_POWER_RISE
            GameSound.BOMB_POWER_FULL -> SoundAsset.BOMB_POWER_FULL
            GameSound.BOMB_EXPLODE -> SoundAsset.BOMB_EXPLODE
            GameSound.BOMB_EXPLODE_RED -> SoundAsset.BOMB_EXPLODE_RED
            GameSound.KNIGHT_CHEER_COMBO -> comboCheerPool[Random.nextInt(comboCheerPool.size)]
            GameSound.KNIGHT_CHEER_KEEP_GOING -> SoundAsset.KNIGHT_CHEER_KEEP_GOING
            GameSound.KNIGHT_CHEER_VICTORY -> SoundAsset.KNIGHT_CHEER_YAY_YOU_DID_IT
            GameSound.TIMER_COUNT -> SoundAsset.TIMER_COUNT
            GameSound.TIMER_COUNT_FINAL -> SoundAsset.TIMER_COUNT_FINAL
            GameSound.GAME_WON -> SoundAsset.GAME_WON
            GameSound.CROWD_VICTORY -> SoundAsset.CROWD_VICTORY
            GameSound.GAME_LOST -> SoundAsset.GAME_LOST
            GameSound.CROWD_DEFEAT -> SoundAsset.CROWD_DEFEAT
            GameSound.AWARD_BRONZE -> SoundAsset.AWARD_BRONZE
            GameSound.AWARD_SILVER -> SoundAsset.AWARD_SILVER
            GameSound.AWARD_GOLD -> SoundAsset.AWARD_GOLD
        }

        playAsset(asset, volume.coerceIn(0f, 1f), rate.coerceIn(0.5f, 2.0f))
    }

    fun playAsset(asset: SoundAsset, volume: Float = 1.0f, rate: Float = 1.0f) {
        if (!isSoundEnabled) return

        if (!asset.useMediaPlayer) {
            val sampleId = soundPoolIds[asset]
            if (sampleId != null && sampleId in loadedSoundIds) {
                soundPool.play(sampleId, volume, volume, 1, 0, rate)
                return
            }
        }

        playWithMediaPlayer(asset, volume)
    }

    private fun playWithMediaPlayer(asset: SoundAsset, volume: Float) {
        val afd = openAssetFd(asset) ?: return
        try {
            val mediaPlayer = MediaPlayer()
            activeMediaPlayers.add(mediaPlayer)
            afd.use { descriptor ->
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mediaPlayer.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
            }
            mediaPlayer.setVolume(volume, volume)
            mediaPlayer.setOnCompletionListener { mp ->
                activeMediaPlayers.remove(mp)
                mp.release()
            }
            mediaPlayer.setOnErrorListener { mp, _, _ ->
                activeMediaPlayers.remove(mp)
                mp.release()
                true
            }
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener { mp ->
                mp.start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error al reproducir ${asset.fileName}", e)
        }
    }

    fun release() {
        soundPool.release()
        activeMediaPlayers.forEach { mp ->
            try {
                mp.release()
            } catch (_: Exception) {
            }
        }
        activeMediaPlayers.clear()
    }

    companion object {
        private const val TAG = "SoundManager"
    }
}
