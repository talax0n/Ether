package dev.ether.app.ui

import dev.ether.app.domain.leveledUp
import dev.ether.app.ui.components.EthaController
import dev.ether.app.ui.components.EthaMove
import dev.ether.app.ui.components.performOrYield
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EthaControllerTest {
    @Test
    fun `a newer move interrupts the one playing and Etha returns to base after it`() = runTest {
        val etha = EthaController(EthaMove.IDLE)
        val fidget = async { etha.performOrYield(EthaMove.LOOK) }
        runCurrent()
        assertEquals(EthaMove.LOOK, etha.move)

        val poke = async { etha.performOrYield(EthaMove.REACT) }
        runCurrent()
        assertEquals(EthaMove.REACT, etha.move)
        assertFalse("the fidget gives way", fidget.await())

        etha.finished()
        assertTrue(poke.await())
        assertEquals(EthaMove.IDLE, etha.move)
    }

    @Test
    fun `a looping move becomes the pose and returns at once`() = runTest {
        val etha = EthaController(EthaMove.IDLE)
        etha.perform(EthaMove.RUN)
        assertEquals(EthaMove.RUN, etha.move)
    }

    @Test
    fun `level-up only when the level beats one already shown`() {
        assertFalse("first look", leveledUp(lastSeen = 0, now = 3))
        assertFalse(leveledUp(lastSeen = 3, now = 3))
        assertTrue(leveledUp(lastSeen = 2, now = 3))
        assertFalse("XP never drops a level, but don't cheer if it did", leveledUp(lastSeen = 4, now = 3))
    }
}
