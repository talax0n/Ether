package dev.ether.app

import dev.ether.app.data.fake.DemoData
import dev.ether.app.data.fake.FakeBackend
import dev.ether.app.domain.TeamResult
import dev.ether.app.ui.teams.TeamsUiState
import dev.ether.app.ui.teams.TeamsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class TeamsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun loaded(vm: TeamsViewModel) = vm.state.value as TeamsUiState.Loaded

    private fun teamsVm(now: Instant = DemoData.now): TeamsViewModel {
        val backend = FakeBackend(signedIn = true, clock = { now })
        return TeamsViewModel(backend) { now }.also { it.load() }
    }

    @Test
    fun `teams in an ended competition can be endorsed`() = runTest {
        val vm = teamsVm()

        val team = loaded(vm).teams.single()
        assertEquals("Night Owls", team.team.name)
        assertEquals(listOf("Ana Putri", "Budi Santoso"), team.team.teammates.map { it.name })
        assertEquals("https://wa.me/6281234567890", team.team.teammates[0].contactUrl)
        assertTrue(team.eventEnded)
    }

    @Test
    fun `endorsing a teammate marks them endorsed`() = runTest {
        val vm = teamsVm()
        val team = loaded(vm).teams.single()

        vm.endorse(team, team.team.teammates[0])

        val after = loaded(vm)
        assertEquals("You endorsed Ana Putri.", after.message)
        assertEquals(listOf(true, false), after.teams.single().team.teammates.map { it.endorsedByMe })
        assertFalse(after.busy)
    }

    @Test
    fun `endorsing before the event ends shows the database's refusal`() = runTest {
        val vm = teamsVm(now = Instant.parse("2026-10-01T00:00:00Z"))
        val team = loaded(vm).teams.single()
        assertFalse(team.eventEnded)

        vm.endorse(team, team.team.teammates[0])

        assertEquals("You can do this once the event has ended.", loaded(vm).message)
        assertEquals(listOf(false, false), loaded(vm).teams.single().team.teammates.map { it.endorsedByMe })
    }

    @Test
    fun `confirming a teammate's result makes it final`() = runTest {
        val vm = teamsVm()

        vm.confirmResult(loaded(vm).teams.single())
        assertEquals("Result confirmed.", loaded(vm).message)
        assertTrue(loaded(vm).teams.single().team.result!!.confirmed)

        vm.reportResult(loaded(vm).teams.single(), TeamResult.WINNER)
        assertEquals("Your team's result is already confirmed.", loaded(vm).message)
        assertEquals(TeamResult.FINALIST, loaded(vm).teams.single().team.result!!.result)
    }

    @Test
    fun `you cannot confirm the result you reported`() = runTest {
        val vm = teamsVm()

        vm.reportResult(loaded(vm).teams.single(), TeamResult.WINNER)
        vm.confirmResult(loaded(vm).teams.single())

        assertEquals("A teammate has to confirm the result you reported.", loaded(vm).message)
        assertTrue(loaded(vm).teams.single().team.result!!.reportedByMe)
    }
}
