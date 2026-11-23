package dev.ether.app.ui

import dev.ether.app.MainDispatcherRule
import dev.ether.app.data.CompetitionRepository
import dev.ether.app.domain.Competition
import dev.ether.app.domain.CompetitionId
import dev.ether.app.domain.CompetitionMode
import dev.ether.app.domain.Listing
import dev.ether.app.ui.competitions.CompetitionFilters
import dev.ether.app.ui.competitions.CompetitionListUiState
import dev.ether.app.ui.competitions.CompetitionListViewModel
import dev.ether.app.ui.competitions.EventStatus
import dev.ether.app.ui.competitions.availableTags
import dev.ether.app.ui.competitions.officialHost
import dev.ether.app.ui.competitions.whereLabel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class CompetitionFiltersTest {
    @get:Rule val main = MainDispatcherRule()

    private val now = Instant.parse("2026-11-24T12:00:00Z")
    private fun day(n: Long) = now.plus(n, ChronoUnit.DAYS)

    private val campus = Competition(
        CompetitionId("campus"), "Campus Hackathon 2026", "", null, day(14), day(21), 2, 4,
        organizer = "Student Tech Society", prize = "Rp 50.000.000", mode = CompetitionMode.OFFLINE,
        city = "Yogyakarta", tags = listOf("Mobile", "Web", "Social impact"),
    )
    private val data = Competition(
        CompetitionId("data"), "Data Challenge 2026", "", null, day(-30), day(-7), 2, 3,
        organizer = "Data Science Student Association", tags = listOf("Data", "AI"),
    )
    private val aiWeek = Competition(
        CompetitionId("ai-week"), "Nusantara AI Build Week", "", null, day(26), day(47), 2, 5,
        organizer = "Nusantara Dev Community", mode = CompetitionMode.HYBRID, city = "Bandung",
        tags = listOf("ai", "Data", "Web"),
    )
    private val ledger = Competition(
        CompetitionId("ledger"), "Open Ledger Online Jam", "", null, day(-1), day(5), 1, 4,
        organizer = "Open Ledger Guild", tags = listOf("Web3"),
    )
    private val all = listOf(campus, data, aiWeek, ledger)

    private fun CompetitionFilters.ids() = all.filter { matches(it, now) }.map { it.id.value }

    @Test
    fun `no filters keeps everything`() {
        assertEquals(listOf("campus", "data", "ai-week", "ledger"), CompetitionFilters().ids())
        assertFalse(CompetitionFilters().isActive)
    }

    @Test
    fun `query matches name, organizer, tags and city ignoring case`() {
        assertEquals(listOf("campus"), CompetitionFilters(query = "campus").ids())
        assertEquals(listOf("data"), CompetitionFilters(query = "SCIENCE student").ids())
        assertEquals(listOf("ledger"), CompetitionFilters(query = "web3").ids())
        assertEquals(listOf("ai-week"), CompetitionFilters(query = "bandung").ids())
        assertEquals(listOf("campus"), CompetitionFilters(query = "  social IMPACT ").ids())
    }

    @Test
    fun `every word of the query has to match somewhere`() {
        assertEquals(listOf("ai-week"), CompetitionFilters(query = "ai bandung").ids())
        assertEquals(emptyList<String>(), CompetitionFilters(query = "ai yogyakarta").ids())
    }

    @Test
    fun `query does not search the description`() {
        val described = campus.copy(id = CompetitionId("described"), name = "X", description = "robotics", organizer = null, city = null, tags = emptyList())
        assertFalse(CompetitionFilters(query = "robotics").matches(described, now))
    }

    @Test
    fun `chips in one group are alternatives`() {
        assertEquals(
            listOf("campus", "ai-week"),
            CompetitionFilters(modes = setOf(CompetitionMode.OFFLINE, CompetitionMode.HYBRID)).ids(),
        )
        assertEquals(listOf("data", "ledger"), CompetitionFilters(statuses = setOf(EventStatus.CLOSED, EventStatus.ENDED)).ids())
        assertEquals(listOf("campus", "ai-week", "ledger"), CompetitionFilters(tags = setOf("web", "web3")).ids())
    }

    @Test
    fun `groups combine, and tags match ignoring case`() {
        val openAi = CompetitionFilters(statuses = setOf(EventStatus.OPEN)).toggleTag("AI")
        assertEquals(listOf("ai-week"), openAi.ids())
        assertEquals(
            emptyList<String>(),
            openAi.toggle(CompetitionMode.ONLINE).ids(),
        )
        assertEquals(listOf("ai-week"), openAi.copy(query = "nusantara").ids())
    }

    @Test
    fun `toggling twice turns a chip off`() {
        val f = CompetitionFilters().toggle(EventStatus.OPEN).toggle(EventStatus.OPEN).toggleTag("AI").toggleTag("ai")
        assertEquals(CompetitionFilters(), f)
    }

    @Test
    fun `tag chips come from the data, most common first, one per spelling`() {
        assertEquals(listOf("AI", "Data", "Web", "Mobile", "Social impact", "Web3"), all.availableTags())
    }

    @Test
    fun `where label shows the city only off line`() {
        assertEquals("Offline · Yogyakarta", campus.whereLabel)
        assertEquals("Hybrid · Bandung", aiWeek.whereLabel)
        assertEquals("Online", data.whereLabel)
    }

    @Test
    fun `official host drops www`() {
        assertEquals("dorahacks.io", officialHost("https://dorahacks.io/hackathon/even-realities/detail"))
        assertEquals("example.com", officialHost("https://www.example.com/x"))
    }

    private class FixedRepo(val list: List<Competition>) : CompetitionRepository {
        override suspend fun competitions() = list
        override suspend fun competition(id: CompetitionId) = list.first { it.id == id }
        override suspend fun myListing(id: CompetitionId): Listing? = null
        override suspend fun joinSolo(id: CompetitionId): Listing = error("not used")
        override suspend fun createTeam(id: CompetitionId, name: String, pitch: String, openRoles: List<String>): Listing = error("not used")
    }

    private fun vm() = CompetitionListViewModel(FixedRepo(all)) { now }
    private fun CompetitionListViewModel.loaded() = state.value as CompetitionListUiState.Loaded

    @Test
    fun `the view model filters the loaded list and keeps sections`() = runTest {
        val vm = vm()
        vm.setQuery("web")
        vm.toggleStatus(EventStatus.OPEN)

        val s = vm.loaded()
        assertEquals(listOf("campus", "ai-week"), s.results.map { it.id.value })
        assertEquals(listOf(EventStatus.OPEN), s.sections.map { it.first })
        assertEquals(2, s.openCount)
        assertTrue(s.filters.isActive)
    }

    @Test
    fun `no match leaves results empty, and clearing brings everything back`() = runTest {
        val vm = vm()
        vm.setQuery("nothing like this")
        vm.toggleMode(CompetitionMode.HYBRID)
        vm.toggleTag("Web3")
        assertTrue(vm.loaded().results.isEmpty())

        vm.clearFilters()

        val s = vm.loaded()
        assertEquals(CompetitionFilters(), s.filters)
        assertEquals(all.size, s.results.size)
    }

    @Test
    fun `filters survive a reload`() = runTest {
        val vm = vm()
        vm.toggleMode(CompetitionMode.OFFLINE)
        vm.load()
        assertEquals(listOf("campus"), vm.loaded().results.map { it.id.value })
    }
}
