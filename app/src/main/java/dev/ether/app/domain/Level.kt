package dev.ether.app.domain

/** Display-only rank derived from total XP (SPEC §4 Levels). [nextXp] is null at the cap. */
data class Level(val number: Int, val title: String, val floorXp: Int, val nextXp: Int?) {
    /** 0..1 progress from this level's floor to the next threshold; 1 at the cap. */
    fun progress(xp: Int): Float =
        nextXp?.let { ((xp - floorXp).toFloat() / (it - floorXp)).coerceIn(0f, 1f) } ?: 1f
}

private val thresholds = listOf(
    0 to "Rookie", 100 to "Hacker", 250 to "Maker", 400 to "Builder",
    600 to "Captain", 850 to "Architect", 1150 to "Mentor", 1500 to "Legend",
)

fun levelFor(xp: Int): Level {
    val index = thresholds.indexOfLast { (floor, _) -> xp >= floor }.coerceAtLeast(0)
    val (floor, title) = thresholds[index]
    return Level(index + 1, title, floor, thresholds.getOrNull(index + 1)?.first)
}
