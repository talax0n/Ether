package dev.ether.app.data.fake

import dev.ether.app.data.nearby.NearbyIdentity
import dev.ether.app.data.nearby.NearbyPeer
import dev.ether.app.data.nearby.NearbyPeers
import dev.ether.app.domain.ListingId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Demo stand-in for Nearby, so the flow can be seen on one emulator. Two people show up after starting:
 * Ana, who shakes back a moment after you shake (and already liked you, so it's a match), and Budi, who never
 * shakes, to show that only people who shake count.
 */
class FakeNearbyPeers(
    private val appearAfterMs: Long = 1_200,
    private val shakeBackAfterMs: Long = 700,
) : NearbyPeers {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _peers = MutableStateFlow<List<NearbyPeer>>(emptyList())
    override val peers: StateFlow<List<NearbyPeer>> = _peers.asStateFlow()
    private val _shakes = MutableSharedFlow<ListingId>(extraBufferCapacity = 16)
    override val shakes: Flow<ListingId> = _shakes.asSharedFlow()
    private var job: Job? = null

    override suspend fun start(me: NearbyIdentity) {
        job?.cancel()
        job = scope.launch {
            delay(appearAfterMs)
            _peers.value = listOf(ANA)
            delay(appearAfterMs)
            _peers.value = listOf(ANA, BUDI)
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        _peers.value = emptyList()
    }

    override fun sendShake() {
        if (ANA !in _peers.value) return
        scope.launch {
            delay(shakeBackAfterMs)
            _shakes.tryEmit(ANA.listingId)
        }
    }

    private companion object {
        val ANA = NearbyPeer(ListingId("l-ana"), "Ana Putri", "Designer")
        val BUDI = NearbyPeer(ListingId("l-budi"), "Budi Santoso", "Data")
    }
}
