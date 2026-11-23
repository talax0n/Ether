package dev.ether.app.ui.nearby

import dev.ether.app.domain.ListingId

/**
 * Decides which nearby peers shook at about the same time as this phone. Every time is a receipt time on this
 * phone's monotonic clock, so two phones' wall clocks never need to agree.
 *
 * A pair is mutual when this phone's shake and the peer's shake message arrive within [windowMs] of each other,
 * in either order. A peer pairs at most once per window, so a long shake on both sides gives one swipe, not many.
 * This only decides when to *try*: whether a match forms is still up to the swipe rules in Postgres.
 */
class ShakePairing(private val windowMs: Long = WINDOW_MS) {
    private var myShakeAt: Long? = null
    private val peerShakeAt = mutableMapOf<ListingId, Long>()
    private val pairedAt = mutableMapOf<ListingId, Long>()

    /** This phone shook at [now]. Returns the peers among [present] whose shake arrived in the last window. */
    fun onMyShake(now: Long, present: Collection<ListingId>): List<ListingId> {
        myShakeAt = now
        return present.filter { peer -> peerShakeAt[peer]?.let { now - it <= windowMs } == true && pair(peer, now) }
    }

    /** [peer]'s shake arrived at [now]. True when this phone shook in the last window, so the two are mutual. */
    fun onPeerShake(peer: ListingId, now: Long): Boolean {
        peerShakeAt[peer] = now
        val mine = myShakeAt ?: return false
        return now - mine <= windowMs && pair(peer, now)
    }

    private fun pair(peer: ListingId, now: Long): Boolean {
        if (pairedAt[peer]?.let { now - it <= windowMs } == true) return false
        pairedAt[peer] = now
        return true
    }

    companion object {
        const val WINDOW_MS = 3_000L
    }
}
