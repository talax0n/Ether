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
import dev.ether.app.domain.EtherError
import dev.ether.app.domain.SwipeDirection
import dev.ether.app.domain.SwipeResult
import dev.ether.app.ui.userMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/** LISTING_CLOSED: the user's own listing went inactive, usually because a match just put them on a team. */
enum class DeckPhase { LOADING, SWIPING, OUT_OF_CARDS, CAP_REACHED, LISTING_CLOSED, FAILED }

data class DeckUiState(
    val phase: DeckPhase = DeckPhase.LOADING,
    val cards: List<DeckCard> = emptyList(),
    /** Swipes left today, as reported by the database. Null until first loaded. */
    val remaining: Int? = null,
    /** Set when a swipe produced a match; cleared when the user dismisses the dialog. */
    val match: Match? = null,
    /** A swipe is on its way to the database; further swipes are ignored until it settles. */
    val swiping: Boolean = false,
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
                _state.update {
                    if (e is EtherError.ListingInactive) it.copy(phase = DeckPhase.LISTING_CLOSED, cards = emptyList())
                    else it.copy(phase = DeckPhase.FAILED, error = e.userMessage())
                }
            }
        }
    }

    fun swipe(card: DeckCard, direction: SwipeDirection) {
        val current = _state.value
        if (current.phase != DeckPhase.SWIPING || current.swiping || current.cards.firstOrNull() != card) return
        _state.update { it.copy(cards = it.cards.drop(1), swiping = true, error = null) }
        viewModelScope.launch {
            try {
                val result = deck.swipe(from, card.listingId, direction)
                val match = if (result.matched) findMatch(card, result) else null
                _state.update { it.copy(remaining = result.remaining, match = match ?: it.match) }
                // A match can close the caller's own listing (a solo just joined a team, or a team filled up),
                // so re-read the deck from the DB instead of trusting the local cards.
                if (match != null || _state.value.cards.isEmpty()) refresh(result.remaining) else settle()
            } catch (e: EtherError.SwipeCapReached) {
                _state.update { it.copy(phase = DeckPhase.CAP_REACHED, remaining = 0, cards = listOf(card) + it.cards, swiping = false) }
            } catch (e: EtherError.ListingInactive) {
                // Either the card filled up or matched elsewhere, or our own listing closed; the DB knows which.
                _state.update { it.copy(error = e.userMessage()) }
                refresh(_state.value.remaining ?: 1)
            } catch (e: Exception) {
                _state.update { it.copy(cards = listOf(card) + it.cards, swiping = false, error = e.userMessage()) }
            }
        }
    }

    fun dismissMatch() = _state.update { it.copy(match = null) }

    private fun settle() = _state.update { it.copy(phase = phaseFor(it.cards, it.remaining ?: 1), swiping = false) }

    private suspend fun refresh(remaining: Int) {
        if (remaining <= 0) return _state.update { it.copy(phase = DeckPhase.CAP_REACHED, swiping = false) }
        try {
            val cards = deck.deck(from)
            _state.update { it.copy(phase = phaseFor(cards, remaining), cards = cards, swiping = false) }
        } catch (e: EtherError.ListingInactive) {
            _state.update { it.copy(phase = DeckPhase.LISTING_CLOSED, cards = emptyList(), swiping = false) }
        } catch (e: Exception) {
            _state.update { it.copy(phase = DeckPhase.FAILED, cards = emptyList(), swiping = false, error = e.userMessage()) }
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
