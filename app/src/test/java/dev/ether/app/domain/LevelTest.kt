package dev.ether.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelTest {
    @Test fun boundaries() {
        assertEquals(Level(1, "Rookie", 0, 100), levelFor(0))
        assertEquals(Level(1, "Rookie", 0, 100), levelFor(99))
        assertEquals(Level(2, "Hacker", 100, 250), levelFor(100))
        assertEquals(Level(3, "Maker", 250, 400), levelFor(399))
        assertEquals(Level(4, "Builder", 400, 600), levelFor(400))
        assertEquals(Level(4, "Builder", 400, 600), levelFor(545))
        assertEquals(Level(7, "Mentor", 1150, 1500), levelFor(1499))
        assertEquals(Level(8, "Legend", 1500, null), levelFor(1500))
        assertEquals(Level(8, "Legend", 1500, null), levelFor(9999))
    }

    @Test fun progressTowardsNextLevel() {
        assertEquals(0.725f, levelFor(545).progress(545), 0.0001f)
        assertEquals(1f, levelFor(9999).progress(9999), 0f)
    }
}
