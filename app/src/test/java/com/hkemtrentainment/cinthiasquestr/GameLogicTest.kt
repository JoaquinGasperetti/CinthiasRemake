package com.hkemtrentainment.cinthiasquestr

import com.hkemtrentainment.cinthiasquestr.audio.SoundAsset
import com.hkemtrentainment.cinthiasquestr.audio.SoundManager
import com.hkemtrentainment.cinthiasquestr.data.PlayerData
import com.hkemtrentainment.cinthiasquestr.data.PlayerRepository
import com.hkemtrentainment.cinthiasquestr.data.WorldRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun `combo pitch escalates monotonically and clamps to safe SoundPool ceiling`() {
        val pitch1 = SoundManager.computeComboPitch(1)
        val pitch2 = SoundManager.computeComboPitch(2)
        val pitch5 = SoundManager.computeComboPitch(5)
        val pitch20 = SoundManager.computeComboPitch(20)

        assertEquals(1.0f, pitch1, 0.001f)
        assertTrue(pitch2 > pitch1)
        assertTrue(pitch5 > pitch2)
        assertEquals(1.75f, pitch20, 0.001f)
    }

    @Test
    fun `win streak starter power-ups and daily streak rewards scale within bounds`() {
        assertEquals(0, PlayerRepository.computeStreakStarterPowerUpsCount(0))
        assertEquals(2, PlayerRepository.computeStreakStarterPowerUpsCount(2))
        assertEquals(3, PlayerRepository.computeStreakStarterPowerUpsCount(8))

        val day1Reward = PlayerRepository.computeDailyStreakReward(1)
        val day7Reward = PlayerRepository.computeDailyStreakReward(7)
        assertEquals(150, day1Reward)
        assertEquals(390, day7Reward)
    }

    @Test
    fun `daily reward can only be claimed once per calendar day`() {
        val today = 20_000L
        // Nunca reclamado antes (lastClaim = 0) -> permitido
        assertTrue(PlayerRepository.evaluateCanClaimDailyReward(today, 0L))
        assertEquals(1, PlayerRepository.computeNextDailyStreak(today, 0L, 1))

        // Reclamado el mismo día (lastClaim = today) -> bloqueado (no reclamable infinitamente)
        assertFalse(PlayerRepository.evaluateCanClaimDailyReward(today, today))
        assertEquals(3, PlayerRepository.computeNextDailyStreak(today, today, 3))

        // Reclamado ayer (lastClaim = today - 1) -> permitido y avanza racha
        assertTrue(PlayerRepository.evaluateCanClaimDailyReward(today, today - 1L))
        assertEquals(4, PlayerRepository.computeNextDailyStreak(today, today - 1L, 3))

        // Si rompió la racha (hace 3 días) -> permitido pero reinicia a Día 1
        assertTrue(PlayerRepository.evaluateCanClaimDailyReward(today, today - 3L))
        assertEquals(1, PlayerRepository.computeNextDailyStreak(today, today - 3L, 5))
    }

    @Test
    fun `daily quests track completion and bitmask claiming accurately`() {
        val playerData = PlayerData(
            dailyMatchesCount = 25,
            dailyPowerUpsTriggered = 2,
            dailyLevelsCompleted = 2,
            claimedDailyQuestsMask = 1 // Quest 0 claimed
        )
        val quests = playerData.getActiveDailyQuests()
        assertEquals(3, quests.size)

        assertTrue(quests[0].isCompleted)
        assertTrue(quests[0].isClaimed)

        assertFalse(quests[1].isCompleted)
        assertFalse(quests[1].isClaimed)

        assertTrue(quests[2].isCompleted)
        assertFalse(quests[2].isClaimed)
    }
}
