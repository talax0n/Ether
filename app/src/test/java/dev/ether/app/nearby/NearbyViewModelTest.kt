package dev.ether.app.nearby

import dev.ether.app.MainDispatcherRule
import dev.ether.app.data.DeckRepository
import dev.ether.app.data.fake.DemoData
import dev.ether.app.data.fake.FakeBackend
import dev.ether.app.data.nearby.NearbyIdentity
import dev.ether.app.data.nearby.NearbyPeer
import dev.ether.app.data.nearby.NearbyPeers
import dev.ether.app.data.nearby.NearbyUnavailable
import dev.ether.app.data.nearby.ShakeDetector
import dev.ether.app.domain.Contacts
import dev.ether.app.domain.ListingId
import dev.ether.app.domain.ListingKind
import dev.ether.app.domain.MatchId
import dev.ether.app.domain.SwipeDirection
import dev.ether.app.domain.SwipeResult
import dev.ether.app.ui.nearby.NearbyPhase
import dev.ether.app.ui.nearby.NearbyViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Stands in for Nearby Connections: the test decides who's nearby and when they shake. */
private class TestNearby : NearbyPeers {
    override val peers = MutableStateFlow<List<NearbyPeer>>(emptyList())
    override val shakes = MutableSharedFlow<ListingId>(extraBufferCapacity = 16)
    var started: NearbyIdentity? = null
    var stops = 0
    var shakesSent = 0
    var failure: NearbyUnavailable? = null

    override suspend fun start(me: NearbyIdentity) {
        failure?.let { throw it }
        started = me
    }

    override fun stop() {
        stops++
        started = null
    }

    override fun sendShake() {
        shakesSent++
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NearbyViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val me = ListingId("l-me-gemastik")
    private val ana = NearbyPeer(ListingId("l-ana"), "Ana Putri", "Designer")
    private val budi = NearbyPeer(ListingId("l-budi"), "Budi Santoso", "Data")
    private val nearby = TestNearby()
    private val myShakes = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    private val swipes = mutableListOf<Pair<ListingId, SwipeDirection>>()

    private fun recording(deck: DeckRepository) = object : DeckRepository by deck {
        override suspend fun swipe(from: ListingId, to: ListingId, direction: SwipeDirection): SwipeResult {
            swipes += to to direction
            return deck.swipe(from, to, direction)
        }
    }

    private fun vm(backend: FakeBackend = FakeBackend(signedIn = true), deck: DeckRepository = backend) = NearbyViewModel(
        DemoData.gemastik, me, nearby, ShakeDetector { myShakes }, recording(deck), backend, backend, backend, backend,
        clock = { main.dispatcher.scheduler.currentTime },
    )

    /** Permission granted, screen visible, [peers] nearby. */
    private fun searching(vm: NearbyViewModel, vararg peers: NearbyPeer) = vm.also {
        it.onPermissions(true)
        it.onVisible(true)
        nearby.peers.value = peers.toList()
    }

    private fun TestScope.shakeMe() {
        myShakes.tryEmit(Unit)
        runCurrent()
    }

    @Test
    fun `starting advertises this listing with the user's name and role`() = runTest {
        val vm = searching(vm())

        assertEquals(NearbyPhase.SEARCHING, vm.state.value.phase)
        assertEquals(NearbyIdentity(DemoData.gemastik, me, "Theo", "Mobile", ListingKind.SOLO), nearby.started)
    }

    @Test
    fun `both shaking within the window swipes right on them and shows the match with their contacts`() = runTest {
        val vm = searching(vm(), ana, budi)

        shakeMe()
        assertEquals(1, nearby.shakesSent)
        advanceTimeBy(1_500)
        nearby.shakes.tryEmit(ana.listingId)
        advanceUntilIdle()

        assertEquals(listOf(ana.listingId to SwipeDirection.RIGHT), swipes)
        val match = vm.state.value.match
        assertEquals("Ana Putri", match?.name)
        assertEquals(Contacts(phone = "6281234567890", instagram = "ana.designs"), match?.contacts)
        assertNull(vm.state.value.pairingWith)
    }

    @Test
    fun `it doesn't matter who shakes first`() = runTest {
        val vm = searching(vm(), ana)

        nearby.shakes.tryEmit(ana.listingId)
        advanceTimeBy(2_000)
        shakeMe()
        advanceUntilIdle()

        assertEquals(listOf(ana.listingId to SwipeDirection.RIGHT), swipes)
        assertEquals("Ana Putri", vm.state.value.match?.name)
    }

    @Test
    fun `shakes more than three seconds apart don't count`() = runTest {
        val vm = searching(vm(), ana)

        shakeMe()
        advanceTimeBy(3_500)
        nearby.shakes.tryEmit(ana.listingId)
        advanceUntilIdle()

        assertTrue(swipes.isEmpty())
        assertNull(vm.state.value.match)
    }

    @Test
    fun `only the nearby people who also shook get a swipe`() = runTest {
        searching(vm(), ana, budi)

        nearby.shakes.tryEmit(ana.listingId)
        shakeMe()
        advanceUntilIdle()

        assertEquals(listOf(ana.listingId), swipes.map { it.first })
    }

    @Test
    fun `a shake from someone not on the list is ignored`() = runTest {
        searching(vm(), ana)

        nearby.shakes.tryEmit(budi.listingId)
        nearby.shakes.tryEmit(me)
        shakeMe()
        advanceUntilIdle()

        assertTrue(swipes.isEmpty())
    }

    @Test
    fun `shaking for a while gives one swipe, not one per shake`() = runTest {
        searching(vm(), ana)

        repeat(3) {
            shakeMe()
            nearby.shakes.tryEmit(ana.listingId)
            advanceTimeBy(900)
        }
        advanceUntilIdle()

        assertEquals(1, swipes.size)
    }

    @Test
    fun `when their swipe lands after ours, retrying finds the match`() = runTest {
        val backend = FakeBackend(signedIn = true)
        var calls = 0
        // The DB answers a retried swipe with its current outcome: no match until the other phone's swipe arrives.
        val late = object : DeckRepository by backend {
            override suspend fun swipe(from: ListingId, to: ListingId, direction: SwipeDirection) =
                if (++calls < 3) SwipeResult(false, null, 29) else SwipeResult(true, MatchId("m-late"), 29)
        }
        val vm = searching(vm(backend, late), budi)

        shakeMe()
        nearby.shakes.tryEmit(budi.listingId)
        runCurrent()
        assertEquals("Budi Santoso", vm.state.value.pairingWith)
        advanceUntilIdle()

        assertEquals(3, calls)
        assertEquals(MatchId("m-late"), vm.state.value.match?.id)
        assertEquals("Budi Santoso", vm.state.value.match?.name)
        assertEquals(ListingKind.SOLO, vm.state.value.match?.otherKind)
    }

    @Test
    fun `no match after waiting says why it might not have matched`() = runTest {
        // Budi never liked this user, so the DB never forms a match.
        val backend = FakeBackend(signedIn = true)
        val vm = searching(vm(backend), budi)

        shakeMe()
        nearby.shakes.tryEmit(budi.listingId)
        advanceUntilIdle()

        assertNull(vm.state.value.match)
        assertTrue(vm.state.value.notice!!.startsWith("No match with Budi Santoso."))
        // One counted swipe; the retries after it are free, like in the DB.
        assertEquals(29, backend.remainingSwipes())
        assertTrue(swipes.size > 1)
        vm.noticeShown()
        assertNull(vm.state.value.notice)
    }

    @Test
    fun `a swipe the database rejects shows its message`() = runTest {
        val vm = searching(vm(FakeBackend(signedIn = true, registrationClosed = true)), ana)

        shakeMe()
        nearby.shakes.tryEmit(ana.listingId)
        advanceUntilIdle()

        assertNull(vm.state.value.match)
        assertEquals("Registration for this competition has closed.", vm.state.value.notice)
    }

    @Test
    fun `out of swipes for today is the cap message`() = runTest {
        val vm = searching(vm(FakeBackend(signedIn = true, remaining = 0)), ana)

        shakeMe()
        nearby.shakes.tryEmit(ana.listingId)
        advanceUntilIdle()

        assertEquals("You've used all 30 swipes for today. Come back tomorrow.", vm.state.value.notice)
    }

    @Test
    fun `asks for permission first and doesn't start nearby until granted`() = runTest {
        val vm = vm()
        vm.onVisible(true)
        assertEquals(NearbyPhase.NEEDS_PERMISSION, vm.state.value.phase)
        assertNull(nearby.started)

        vm.onPermissions(false)
        assertEquals(NearbyPhase.PERMISSION_DENIED, vm.state.value.phase)
        assertNull(nearby.started)

        vm.onPermissions(true)
        assertEquals(NearbyPhase.SEARCHING, vm.state.value.phase)
        assertNotNull(nearby.started)
    }

    @Test
    fun `leaving the screen stops nearby and the shake sensor`() = runTest {
        val vm = searching(vm(), ana)

        vm.onVisible(false)
        assertNull(nearby.started)
        assertTrue(nearby.stops > 0)
        shakeMe()
        assertEquals(0, nearby.shakesSent)

        vm.onVisible(true)
        assertNotNull(nearby.started)
    }

    @Test
    fun `nearby failing to start explains and can retry`() = runTest {
        nearby.failure = NearbyUnavailable("Bluetooth isn't working. Turn it on, then try again.")
        val vm = searching(vm())
        assertEquals(NearbyPhase.UNAVAILABLE, vm.state.value.phase)
        assertEquals("Bluetooth isn't working. Turn it on, then try again.", vm.state.value.error)

        nearby.failure = null
        vm.retry()

        assertEquals(NearbyPhase.SEARCHING, vm.state.value.phase)
        assertNotNull(nearby.started)
    }

    @Test
    fun `a listing that isn't the user's active one can't match nearby`() = runTest {
        val backend = FakeBackend(signedIn = true)
        val vm = NearbyViewModel(
            DemoData.gemastik, ListingId("l-someone-else"), nearby, ShakeDetector { myShakes }, backend, backend, backend, backend, backend,
            clock = { main.dispatcher.scheduler.currentTime },
        )
        searching(vm)

        assertEquals(NearbyPhase.UNAVAILABLE, vm.state.value.phase)
        assertNull(nearby.started)
    }

    @Test
    fun `tapping the hint counts as a shake`() = runTest {
        val vm = searching(vm(), ana)

        vm.shakeByTap()
        nearby.shakes.tryEmit(ana.listingId)
        advanceUntilIdle()

        assertEquals("Ana Putri", vm.state.value.match?.name)
    }

    @Test
    fun `the people list marks who just shook, for a moment`() = runTest {
        val vm = searching(vm(), ana, budi)

        nearby.shakes.tryEmit(ana.listingId)
        runCurrent()
        assertEquals(listOf(true, false), vm.state.value.people.map { it.shook })

        advanceTimeBy(3_100)
        assertFalse(vm.state.value.people.any { it.shook })
    }
}
