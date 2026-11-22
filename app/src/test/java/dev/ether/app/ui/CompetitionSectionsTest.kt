package dev.ether.app.ui

import dev.ether.app.domain.Competition
import dev.ether.app.domain.CompetitionId
import dev.ether.app.ui.competitions.EventStatus
import dev.ether.app.ui.competitions.bySection
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class CompetitionSectionsTest {
    private val now = Instant.parse("2026-11-22T12:00:00Z")
    private fun day(n: Long) = now.plus(n, ChronoUnit.DAYS)
    private fun comp(id: String, deadline: Long, end: Long) =
        Competition(CompetitionId(id), id, "", null, day(deadline), day(end), 2, 4)

    @Test
    fun `groups open, closed, finished in that order with the most urgent first`() {
        val sections = listOf(
            comp("old", -40, -20), comp("open-late", 9, 20), comp("closed", -1, 5),
            comp("recent", -10, -2), comp("open-soon", 2, 10),
        ).bySection(now)

        assertEquals(
            listOf(
                EventStatus.OPEN to listOf("open-soon", "open-late"),
                EventStatus.CLOSED to listOf("closed"),
                EventStatus.ENDED to listOf("recent", "old"),
            ),
            sections.map { (status, list) -> status to list.map { it.id.value } },
        )
    }

    @Test
    fun `empty groups are left out`() {
        assertEquals(listOf(EventStatus.ENDED), listOf(comp("old", -40, -20)).bySection(now).map { it.first })
    }
}
