package com.hkemtrentainment.cinthiasquestr

import com.hkemtrentainment.cinthiasquestr.audio.SoundAsset
import com.hkemtrentainment.cinthiasquestr.data.PlayerRepository
import com.hkemtrentainment.cinthiasquestr.data.WorldRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLogicTest {

    @Test
    fun `parseStars and formatStars round-trip accurately`() {
        val original = mapOf(1 to 3, 2 to 2, 5 to 1)
        val formatted = PlayerRepository.formatStars(original)
        assertEquals("1:3,2:2,5:1", formatted)

        val parsed = PlayerRepository.parseStars(formatted)
        assertEquals(original, parsed)
    }

    @Test
    fun `getLevelConfig clamps out-of-range levels to MAX_GAME_LEVELS`() {
        val level25 = WorldRepository.getLevelConfig(25)
        val level99 = WorldRepository.getLevelConfig(99)
        assertEquals(25, level99.levelNumber)
        assertEquals(level25.targetScore, level99.targetScore)
        assertEquals(level25.moves, level99.moves)
        assertTrue(level25.moves >= 12)
    }

    @Test
    fun `all 25 levels map to valid worlds 1 through 5`() {
        for (lvl in 1..WorldRepository.MAX_GAME_LEVELS) {
            val world = WorldRepository.getWorldForLevel(lvl)
            assertTrue(lvl in world.startLevel..world.endLevel)
        }
    }

    @Test
    fun `all 33 sound assets are registered with valid filenames and raw identifiers`() {
        assertEquals(33, SoundAsset.entries.size)
        val rawNames = SoundAsset.entries.map { it.rawResName }.toSet()
        assertEquals(33, rawNames.size)
        for (asset in SoundAsset.entries) {
            assertTrue(asset.fileName.endsWith(".WAV"))
            assertTrue(asset.rawResName.matches(Regex("^[a-z0-9_]+$")))
        }
    }
}
