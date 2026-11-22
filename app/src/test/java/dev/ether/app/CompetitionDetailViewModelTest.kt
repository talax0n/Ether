package dev.ether.app

import dev.ether.app.data.fake.DemoData
import dev.ether.app.data.fake.FakeBackend
import dev.ether.app.domain.Listing
import dev.ether.app.ui.competitions.CompetitionDetailViewModel
import dev.ether.app.ui.competitions.Entry
import dev.ether.app.ui.competitions.TeamForm
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Duration

class CompetitionDetailViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `joining after the deadline shows the registration closed message`() = runTest {
        val backend = FakeBackend(signedIn = true, registrationClosed = true)
        val vm = CompetitionDetailViewModel(DemoData.campusHack, backend, backend) { DemoData.now }

        vm.joinSolo()

        assertEquals("Registration for this competition has closed.", vm.state.value.error)
        assertNull(vm.state.value.listing)
        assertFalse(vm.state.value.busy)
    }

    @Test
    fun `creating a team lists it and closes the form`() = runTest {
        val backend = FakeBackend(signedIn = true)
        val vm = CompetitionDetailViewModel(DemoData.campusHack, backend, backend) { DemoData.now }

        vm.openTeamForm()
        vm.editTeamForm(TeamForm("Rocket", "We ship", "Mobile, Designer"))
        vm.createTeam()

        assertTrue(vm.state.value.listing is Listing.Team)
        assertNull(vm.state.value.teamForm)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `a member of a team in the competition sees that team`() = runTest {
        val backend = FakeBackend(signedIn = true)
        val vm = CompetitionDetailViewModel(DemoData.dataChallenge, backend, backend) { DemoData.now }

        assertEquals("Night Owls", vm.state.value.team?.name)
        assertFalse(vm.state.value.registrationOpen)
    }

    @Test
    fun `entry moves from choosing to listed once the user joins solo`() = runTest {
        val backend = FakeBackend(signedIn = true)
        val vm = CompetitionDetailViewModel(DemoData.campusHack, backend, backend) { DemoData.now }
        assertEquals(Entry.Choose, vm.state.value.entry)

        vm.joinSolo()

        val entry = vm.state.value.entry
        assertTrue(entry is Entry.Listed && entry.listing is Listing.Solo)
    }

    @Test
    fun `entry is closed when there is no listing and the deadline passed`() = runTest {
        val backend = FakeBackend(signedIn = true)
        val vm = CompetitionDetailViewModel(DemoData.campusHack, backend, backend) { DemoData.now.plus(Duration.ofDays(30)) }

        assertEquals(Entry.RegistrationClosed, vm.state.value.entry)
    }

    @Test
    fun `entry is the team for someone who joined through a match`() = runTest {
        val backend = FakeBackend(signedIn = true)
        val vm = CompetitionDetailViewModel(DemoData.dataChallenge, backend, backend) { DemoData.now }

        assertEquals("Night Owls", (vm.state.value.entry as Entry.OnTeam).team.name)
    }

    @Test
    fun `a listing stops offering swipes once registration closes`() = runTest {
        val backend = FakeBackend(signedIn = true)
        val vm = CompetitionDetailViewModel(DemoData.gemastik, backend, backend) { DemoData.now }

        val entry = vm.state.value.entry as Entry.Listed
        assertFalse(entry.canSwipe)
    }
}
