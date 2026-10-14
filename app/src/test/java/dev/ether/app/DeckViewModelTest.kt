package dev.ether.app

import dev.ether.app.data.fake.DemoData
import dev.ether.app.data.fake.FakeBackend
import dev.ether.app.data.DeckRepository
import dev.ether.app.domain.EtherError
import dev.ether.app.domain.ListingId
import dev.ether.app.domain.SwipeDirection
import dev.ether.app.ui.deck.DeckPhase
import dev.ether.app.ui.deck.DeckViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class DeckViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun deckVm(backend: FakeBackend) =
        DeckViewModel(DemoData.gemastik, ListingId("l-me-gemastik"), backend, backend)

    @Test
    fun `swiping right on someone who liked you shows the match with their contact`() = runTest {
        val vm = deckVm(FakeBackend(signedIn = true))
        assertEquals(DeckPhase.SWIPING, vm.state.value.phase)
        assertEquals(30, vm.state.value.remaining)

        vm.swipe(vm.state.value.cards.first(), SwipeDirection.RIGHT)

        val state = vm.state.value
        assertEquals("Ana Putri", state.match?.name)
        assertEquals("https://wa.me/6281234567890", state.match?.contactUrl)
        assertEquals(29, state.remaining)
        // The match put this solo on a team, so their solo listing is closed.
        assertEquals(DeckPhase.LISTING_CLOSED, state.phase)

        vm.dismissMatch()
        assertNull(vm.state.value.match)
    }

    @Test
    fun `swiping right on someone who has not liked you is not a match`() = runTest {
        val vm = deckVm(FakeBackend(signedIn = true))
        vm.swipe(vm.state.value.cards[0], SwipeDirection.LEFT)
        vm.swipe(vm.state.value.cards[0], SwipeDirection.LEFT)

        vm.swipe(vm.state.value.cards[0], SwipeDirection.RIGHT)

        assertNull(vm.state.value.match)
        assertEquals(27, vm.state.value.remaining)
    }

    @Test
    fun `using the last swipe of the day moves to cap reached`() = runTest {
        val vm = deckVm(FakeBackend(signedIn = true, remaining = 1))

        vm.swipe(vm.state.value.cards.first(), SwipeDirection.LEFT)

        assertEquals(DeckPhase.CAP_REACHED, vm.state.value.phase)
        assertEquals(0, vm.state.value.remaining)
    }

    @Test
    fun `opening the deck with no swipes left starts at cap reached`() = runTest {
        val vm = deckVm(FakeBackend(signedIn = true, remaining = 0))

        assertEquals(DeckPhase.CAP_REACHED, vm.state.value.phase)
        assertEquals(0, vm.state.value.remaining)
    }

    @Test
    fun `swiping after registration closed keeps the card and explains why`() = runTest {
        val vm = deckVm(FakeBackend(signedIn = true, registrationClosed = true))
        val first = vm.state.value.cards.first()

        vm.swipe(first, SwipeDirection.RIGHT)

        assertEquals("Registration for this competition has closed.", vm.state.value.error)
        assertEquals(first, vm.state.value.cards.first())
        assertEquals(DeckPhase.SWIPING, vm.state.value.phase)
    }

    @Test
    fun `a second swipe while the first is pending is ignored and a match closes the deck`() = runTest {
        val backend = FakeBackend(signedIn = true)
        val gate = CompletableDeferred<Unit>()
        var swipes = 0
        val slow = object : DeckRepository by backend {
            override suspend fun swipe(from: ListingId, to: ListingId, direction: SwipeDirection) =
                gate.await().let { swipes++; backend.swipe(from, to, direction) }
        }
        val vm = DeckViewModel(DemoData.gemastik, ListingId("l-me-gemastik"), slow, backend)
        val (ana, byteBrigade) = vm.state.value.cards

        vm.swipe(ana, SwipeDirection.RIGHT)
        assertTrue(vm.state.value.swiping)
        vm.swipe(byteBrigade, SwipeDirection.LEFT)
        gate.complete(Unit)

        assertEquals(1, swipes)
        assertEquals("Ana Putri", vm.state.value.match?.name)
        assertEquals(DeckPhase.LISTING_CLOSED, vm.state.value.phase)
        assertFalse(vm.state.value.swiping)
    }

    @Test
    fun `a failed refill shows the error with retry, not an empty deck`() = runTest {
        val backend = FakeBackend(signedIn = true)
        var deckCalls = 0
        val flaky = object : DeckRepository by backend {
            override suspend fun deck(from: ListingId, limit: Int) =
                if (++deckCalls > 1) throw EtherError.Offline() else backend.deck(from, limit).drop(4)
        }
        val vm = DeckViewModel(DemoData.gemastik, ListingId("l-me-gemastik"), flaky, backend)

        vm.swipe(vm.state.value.cards.single(), SwipeDirection.LEFT)

        assertEquals(DeckPhase.FAILED, vm.state.value.phase)
        assertEquals("Can't reach Ether. Check your connection and try again.", vm.state.value.error)
    }
}
