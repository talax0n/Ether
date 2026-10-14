package dev.ether.app

import dev.ether.app.data.fake.DemoData
import dev.ether.app.data.fake.FakeBackend
import dev.ether.app.domain.ListingId
import dev.ether.app.domain.SwipeDirection
import dev.ether.app.ui.deck.DeckPhase
import dev.ether.app.ui.deck.DeckViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        assertEquals(ListingId("l-byte-brigade"), state.cards.first().listingId)
        assertEquals(4, state.cards.size)

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
}
