package dev.ether.app.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ether.app.data.CompetitionRepository
import dev.ether.app.data.DeckRepository
import dev.ether.app.data.MatchRepository
import dev.ether.app.data.ProfileRepository
import dev.ether.app.data.TeamRepository
import dev.ether.app.data.nearby.NearbyIdentity
import dev.ether.app.data.nearby.NearbyPeer
import dev.ether.app.data.nearby.NearbyPeers
import dev.ether.app.data.nearby.NearbyUnavailable
import dev.ether.app.data.nearby.ShakeDetector
import dev.ether.app.domain.CompetitionId
import dev.ether.app.domain.Contacts
import dev.ether.app.domain.Listing
import dev.ether.app.domain.ListingId
import dev.ether.app.domain.ListingKind
import dev.ether.app.domain.Match
import dev.ether.app.domain.SwipeDirection
import dev.ether.app.domain.SwipeResult
import dev.ether.app.ui.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

enum class NearbyPhase {
    LOADING,
    /** Nearby needs Bluetooth and Wi-Fi permissions; the screen explains why before asking. */
    NEEDS_PERMISSION,
    PERMISSION_DENIED,
    /** Advertising and discovering: shake to match. */
    SEARCHING,
    /** Can't run here: the listing no longer swipes, or Nearby failed to start. [NearbyUiState.error] says which. */
    UNAVAILABLE,
}

/** Someone on the nearby list. [shook] is true for a moment after their shake arrives. */
data class NearbyPerson(val peer: NearbyPeer, val shook: Boolean = false)

data class NearbyUiState(
    val phase: NearbyPhase = NearbyPhase.LOADING,
    val people: List<NearbyPerson> = emptyList(),
    /** This phone shook within the pairing window; the hint shows it's waiting for the other side. */
    val shookRecently: Boolean = false,
    /** Name of the person a swipe is being recorded with, while it's in flight. */
    val pairingWith: String? = null,
    /** The match to celebrate. More can queue behind it. */
    val match: Match? = null,
    /** One-off message for the snackbar: a rejected swipe, or no match. */
    val notice: String? = null,
    val error: String? = null,
)

/**
 * Shake to match nearby. When this phone and a nearby peer shake within [ShakePairing.WINDOW_MS], this phone
 * records a RIGHT swipe on the peer's listing through the normal swipe RPC, and the peer's phone does the same.
 * The second swipe to reach Postgres forms the match, under every usual rule (cap, deadline, team-to-team).
 *
 * The first swipe to land sees no match yet, so it retries: the RPC returns the current outcome for an existing
 * swipe without counting it again, so a retry turns into a match once the other side's swipe arrives.
 */
class NearbyViewModel(
    private val competitionId: CompetitionId,
    private val from: ListingId,
    private val nearby: NearbyPeers,
    private val shakeDetector: ShakeDetector,
    private val deck: DeckRepository,
    private val matches: MatchRepository,
    private val competitions: CompetitionRepository,
    private val profiles: ProfileRepository,
    private val teams: TeamRepository,
    /** Monotonic milliseconds, e.g. SystemClock.elapsedRealtime. */
    private val clock: () -> Long,
    private val matchPollMs: Long = 1_000,
    private val matchPolls: Int = 8,
) : ViewModel() {
    private val _state = MutableStateFlow(NearbyUiState())
    val state: StateFlow<NearbyUiState> = _state.asStateFlow()

    private var me: NearbyIdentity? = null
    private var permitted: Boolean? = null
    private var visible = false
    private var session: Job? = null
    private val pairing = ShakePairing()
    private val inFlight = mutableSetOf<ListingId>()
    private val matchQueue = ArrayDeque<Match>()
    private var shookTimer: Job? = null
    private val peerTimers = mutableMapOf<ListingId, Job>()

    init { load() }

    fun retry() {
        _state.update { it.copy(error = null) }
        if (me == null) load() else sync()
    }

    /** From the screen: whether every permission Nearby needs is granted. */
    fun onPermissions(granted: Boolean) {
        permitted = granted
        sync()
    }

    /** The screen is started (true) or stopped (false). Nearby only runs while it's visible. */
    fun onVisible(isVisible: Boolean) {
        visible = isVisible
        sync()
    }

    /** The accessible alternative to shaking: same effect as a shake of this phone. */
    fun shakeByTap() = onMyShake()

    fun dismissMatch() {
        matchQueue.removeFirstOrNull()
        _state.update { it.copy(match = matchQueue.firstOrNull()) }
    }

    fun noticeShown() = _state.update { it.copy(notice = null) }

    override fun onCleared() {
        stopSession()
    }

    private fun load() {
        _state.update { it.copy(phase = NearbyPhase.LOADING) }
        viewModelScope.launch {
            try {
                val identity = loadIdentity()
                if (identity == null) {
                    _state.update {
                        it.copy(phase = NearbyPhase.UNAVAILABLE, error = "Your listing here isn't taking swipes anymore, so there's nothing to match.")
                    }
                } else {
                    me = identity
                    sync()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(phase = NearbyPhase.UNAVAILABLE, error = e.userMessage()) }
            }
        }
    }

    /** Null when [from] isn't the user's active listing in this competition. */
    private suspend fun loadIdentity(): NearbyIdentity? {
        val listing = competitions.myListing(competitionId)?.takeIf { it.id == from && it.active } ?: return null
        val profile = profiles.myProfile()
        return when (listing) {
            is Listing.Solo -> NearbyIdentity(competitionId, from, profile?.name ?: "Someone", profile?.roles?.firstOrNull(), ListingKind.SOLO)
            is Listing.Team -> {
                val team = teams.myTeams().firstOrNull { it.competition.id == competitionId }
                NearbyIdentity(
                    competitionId, from, team?.name ?: profile?.name ?: "A team",
                    team?.let { "Team of ${it.teammates.size + 1}" } ?: "Team", ListingKind.TEAM,
                )
            }
        }
    }

    /** Moves to the phase the inputs call for, starting or stopping Nearby to match. */
    private fun sync() {
        val identity = me ?: return
        if (_state.value.phase == NearbyPhase.UNAVAILABLE && _state.value.error != null) return stopSession()
        val phase = when (permitted) {
            null -> NearbyPhase.NEEDS_PERMISSION
            false -> NearbyPhase.PERMISSION_DENIED
            true -> NearbyPhase.SEARCHING
        }
        _state.update { it.copy(phase = phase) }
        if (phase == NearbyPhase.SEARCHING && visible) startSession(identity) else stopSession()
    }

    private fun startSession(identity: NearbyIdentity) {
        // A session that failed to start has completed, so a retry starts a fresh one.
        if (session?.isActive == true) return
        session = viewModelScope.launch {
            try {
                nearby.start(identity)
            } catch (e: NearbyUnavailable) {
                _state.update { it.copy(phase = NearbyPhase.UNAVAILABLE, error = e.message) }
                return@launch
            }
            launch { nearby.peers.collect(::onPeers) }
            launch { nearby.shakes.collect(::onPeerShake) }
            launch { shakeDetector.shakes().collect { onMyShake() } }
        }
    }

    private fun stopSession() {
        session?.cancel()
        session = null
        nearby.stop()
    }

    private fun onPeers(peers: List<NearbyPeer>) = _state.update { s ->
        val shook = s.people.filter { it.shook }.map { it.peer.listingId }.toSet()
        s.copy(people = peers.filter { it.listingId != from }.map { NearbyPerson(it, it.listingId in shook) })
    }

    private fun onMyShake() {
        if (_state.value.phase != NearbyPhase.SEARCHING) return
        val now = clock()
        nearby.sendShake()
        _state.update { it.copy(shookRecently = true) }
        shookTimer?.cancel()
        shookTimer = viewModelScope.launch {
            delay(ShakePairing.WINDOW_MS)
            _state.update { it.copy(shookRecently = false) }
        }
        val present = _state.value.people.map { it.peer }
        pairing.onMyShake(now, present.map { it.listingId }).forEach { id -> present.firstOrNull { it.listingId == id }?.let(::swipeRight) }
    }

    private fun onPeerShake(id: ListingId) {
        // Only people on the list count: same competition, said hello, and not this phone.
        val peer = _state.value.people.firstOrNull { it.peer.listingId == id }?.peer ?: return
        markShook(id, true)
        peerTimers.remove(id)?.cancel()
        peerTimers[id] = viewModelScope.launch {
            delay(ShakePairing.WINDOW_MS)
            markShook(id, false)
        }
        if (pairing.onPeerShake(id, clock())) swipeRight(peer)
    }

    private fun markShook(id: ListingId, shook: Boolean) = _state.update { s ->
        s.copy(people = s.people.map { if (it.peer.listingId == id) it.copy(shook = shook) else it })
    }

    private fun swipeRight(peer: NearbyPeer) {
        if (!inFlight.add(peer.listingId)) return
        _state.update { it.copy(pairingWith = peer.name) }
        viewModelScope.launch {
            try {
                var result = deck.swipe(from, peer.listingId, SwipeDirection.RIGHT)
                var polls = 0
                // Their swipe may land after ours. Retrying returns the current outcome and isn't counted again.
                while (!result.matched && polls++ < matchPolls) {
                    delay(matchPollMs)
                    result = deck.swipe(from, peer.listingId, SwipeDirection.RIGHT)
                }
                if (result.matched) {
                    matchQueue.addLast(findMatch(peer, result))
                    _state.update { it.copy(match = matchQueue.first()) }
                } else {
                    _state.update {
                        it.copy(notice = "No match with ${peer.name}. You both have to shake, and if either of you passed on the other in the deck, it can't match.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(notice = e.userMessage()) }
            } finally {
                inFlight.remove(peer.listingId)
                _state.update { s -> s.copy(pairingWith = s.people.firstOrNull { it.peer.listingId in inFlight }?.peer?.name) }
            }
        }
    }

    /** The swipe RPC only returns the match id; the contacts come from `my_matches`. */
    private suspend fun findMatch(peer: NearbyPeer, result: SwipeResult): Match {
        val found = try {
            matches.matches(competitionId).firstOrNull { it.id == result.matchId }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        return found ?: Match(
            id = checkNotNull(result.matchId) { "matched swipe without match_id" },
            competitionId = competitionId,
            otherListingId = peer.listingId,
            otherKind = peer.kind,
            name = peer.name,
            avatarUrl = null,
            contacts = Contacts.NONE,
            createdAt = Instant.now(),
        )
    }
}
