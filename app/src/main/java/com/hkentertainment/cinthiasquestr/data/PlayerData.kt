package com.hkemtrentainment.cinthiasquestr.data

data class PlayerData(
    val coins: Int = 0,
    val level: Int = 1, // Highest unlocked level
    val hammer: Int = 1,
    val dynamite: Int = 1,
    val shuffle: Int = 1,
    val levelStars: Map<Int, Int> = emptyMap() // Map of level number -> earned stars (1..3)
)
