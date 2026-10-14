package dev.ether.app

import dev.ether.app.data.fake.DemoData
import dev.ether.app.data.fake.FakeBackend
import dev.ether.app.domain.Listing
import dev.ether.app.ui.competitions.CompetitionDetailViewModel
import dev.ether.app.ui.competitions.TeamForm
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CompetitionDetailViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `joining after the deadline shows the registration closed message`() = runTest {
        val vm = CompetitionDetailViewModel(DemoData.campusHack, FakeBackend(signedIn = true, registrationClosed = true))

        vm.joinSolo()

        assertEquals("Registration for this competition has closed.", vm.state.value.error)
        assertNull(vm.state.value.listing)
        assertFalse(vm.state.value.busy)
    }

    @Test
    fun `creating a team lists it and closes the form`() = runTest {
        val vm = CompetitionDetailViewModel(DemoData.campusHack, FakeBackend(signedIn = true))

        vm.openTeamForm()
        vm.editTeamForm(TeamForm("Rocket", "We ship", "Mobile, Designer"))
        vm.createTeam()

        assertTrue(vm.state.value.listing is Listing.Team)
        assertNull(vm.state.value.teamForm)
        assertNull(vm.state.value.error)
    }
}
