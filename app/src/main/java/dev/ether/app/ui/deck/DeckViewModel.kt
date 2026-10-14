package dev.ether.app.ui.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ether.app.data.DeckRepository
import dev.ether.app.data.MatchRepository
import dev.ether.app.domain.CompetitionId
import dev.ether.app.domain.DeckCard
import dev.ether.app.domain.ListingId
import dev.ether.app.domain.ListingKind
import dev.ether.app.domain.Match
import dev.ether.app.domain.SwipeCapReachedException
import dev.ether.app.domain.SwipeDirection
import dev.ether.app.domain.SwipeResult
import dev.ether.app.ui.userMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

enum class DeckPhase { LOADING, SWIPING, OUT_OF_CARDS, CAP_REACHED, FAILED }

data class DeckUiState(
    val phase: DeckPhase = DeckPhase.LOADING,
    val cards: List<DeckCard> = emptyList(),
    /** Swipes left today, as reported by the database. Null until first loaded. */
    val remaining: Int? = null,
    /** Set when a swipe produced a match; cleared when the user dismisses the dialog. */
    val match: Match? = null,
    val error: String? = null,
)

class DeckViewModel(
    private val competitionId: CompetitionId,
    private val from: ListingId,
    private val deck: DeckRepository,
    private val matches: MatchRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DeckUiState())
    val state: StateFlow<DeckUiState> = _state.asStateFlow()

    // Swipes run one at a time so a deck refill never races a swipe that hasn't reached the DB yet.
    private val swipeLock = Mutex()

    init { load() }

    fun load() {
        _state.update { it.copy(phase = DeckPhase.LOADING, error = null) }
        viewModelScope.launch {
            runCatching {
                val cards = async { deck.deck(from) }
                val remaining = async { deck.remainingSwipes() }
                cards.await() to remaining.await()
            }.onSuccess { (cards, remaining) ->
                _state.update { it.copy(phase = phaseFor(cards, remaining), cards = cards, remaining = remaining) }
            }.onFailure { e ->
                _state.update { it.copy(phase = DeckPhase.FAILED, error = e.userMessage()) }
            }
        }
    }

    fun swipe(card: DeckCard, direction: SwipeDirection) {
        val current = _state.value
        if (current.phase != DeckPhase.SWIPING || current.cards.firstOrNull() != card) return
        _state.update { it.copy(cards = it.cards.drop(1), error = null) }
        viewModelScope.launch {
            swipeLock.withLock {
                try {
                    onSwiped(card, deck.swipe(from, card.listingId, direction))
                } catch (e: SwipeCapReachedException) {
                    _state.update { it.copy(phase = DeckPhase.CAP_REACHED, remaining = 0, cards = listOf(card) + it.cards) }
                } catch (e: Exception) {
                    _state.update { it.copy(cards = listOf(card) + it.cards, error = e.userMessage()) }
                }
            }
        }
    }

    fun dismissMatch() = _state.update { it.copy(match = null) }

    private suspend fun onSwiped(card: DeckCard, result: SwipeResult) {
        val match = if (result.matched) findMatch(card, result) else null
        var cards = _state.value.cards
        if (cards.isEmpty() && result.remaining > 0) cards = runCatching { deck.deck(from) }.getOrDefault(emptyList())
        _state.update {
            it.copy(
                phase = phaseFor(cards, result.remaining),
                cards = cards,
                remaining = result.remaining,
                match = match ?: it.match,
            )
        }
    }

    /** The swipe RPC only returns the match id; the contact link comes from `my_matches`. */
    private suspend fun findMatch(card: DeckCard, result: SwipeResult): Match {
        val found = runCatching { matches.matches(competitionId) }.getOrNull()?.firstOrNull { it.id == result.matchId }
        return found ?: Match(
            id = checkNotNull(result.matchId) { "matched swipe without match_id" },
            competitionId = competitionId,
            otherListingId = card.listingId,
            otherKind = if (card is DeckCard.Team) ListingKind.TEAM else ListingKind.SOLO,
            name = card.name,
            avatarUrl = card.avatarUrl,
            contactUrl = null,
            createdAt = Instant.now(),
        )
    }

    private fun phaseFor(cards: List<DeckCard>, remaining: Int) = when {
        remaining <= 0 -> DeckPhase.CAP_REACHED
        cards.isEmpty() -> DeckPhase.OUT_OF_CARDS
        else -> DeckPhase.SWIPING
    }
}
