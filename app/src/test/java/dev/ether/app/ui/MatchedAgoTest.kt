package dev.ether.app.ui

import dev.ether.app.ui.matches.matchedAgo
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class MatchedAgoTest {
    private val now = Instant.parse("2026-11-22T12:00:00Z")
    private fun ago(minutes: Long) = matchedAgo(now.minus(Duration.ofMinutes(minutes)), now)

    @Test
    fun `phrases step from minutes to hours to yesterday`() {
        assertEquals("Matched just now", ago(0))
        assertEquals("Matched 5 min ago", ago(5))
        assertEquals("Matched 1 hour ago", ago(60))
        assertEquals("Matched 23 hours ago", ago(23 * 60 + 59))
        assertEquals("Matched yesterday", ago(30 * 60))
    }
}
