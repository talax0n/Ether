package dev.ether.app.data.nearby

import dev.ether.app.domain.CompetitionId
import dev.ether.app.domain.ListingId
import dev.ether.app.domain.ListingKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Who this phone says it is to nearby phones: the listing it swipes from, and what to show on their list. */
data class NearbyIdentity(
    val competitionId: CompetitionId,
    val listingId: ListingId,
    val name: String,
    val role: String?,
    val kind: ListingKind,
)

/** Someone nearby, in the same competition, who has the Match nearby screen open. */
data class NearbyPeer(val listingId: ListingId, val name: String, val role: String?, val kind: ListingKind = ListingKind.SOLO)

/**
 * Phones close by that have Match nearby open for the same competition. Implementations only carry
 * messages; who matches is decided by the swipe flow in Postgres.
 */
interface NearbyPeers {
    /** Connected peers in the same competition, in the order they appeared. */
    val peers: StateFlow<List<NearbyPeer>>

    /** A connected peer reports that they just shook their phone. Emitted on receipt. */
    val shakes: Flow<ListingId>

    /** Starts advertising and discovering as [me]. Throws [NearbyUnavailable] when Nearby can't run. */
    suspend fun start(me: NearbyIdentity)

    /** Stops advertising and discovery and drops every connection. Safe to call when not started. */
    fun stop()

    /** Tells every connected peer that this phone was just shaken. */
    fun sendShake()
}

/** A shake of this phone, already debounced. */
fun interface ShakeDetector {
    /** Cold: collecting listens to the sensor, cancelling stops listening. */
    fun shakes(): Flow<Unit>
}

class NearbyUnavailable(message: String) : Exception(message)

/** What nearby phones send each other, as small JSON byte payloads. */
@Serializable
sealed interface NearbyMessage {
    /** Sent once on connection. The endpoint name is the listing id; this adds what the list shows. */
    @Serializable @SerialName("hello")
    data class Hello(
        val competitionId: String,
        val listingId: String,
        val name: String,
        val role: String? = null,
        val team: Boolean = false,
    ) : NearbyMessage {
        fun toPeer() = NearbyPeer(ListingId(listingId), name, role, if (team) ListingKind.TEAM else ListingKind.SOLO)

        companion object {
            fun of(me: NearbyIdentity) =
                Hello(me.competitionId.value, me.listingId.value, me.name, me.role, me.kind == ListingKind.TEAM)
        }
    }

    /** "I just shook." [sentAtMs] is the sender's own monotonic clock, for logs only: receivers time it on arrival. */
    @Serializable @SerialName("shake")
    data class Shake(val sentAtMs: Long) : NearbyMessage

    companion object {
        private val json = Json { ignoreUnknownKeys = true; classDiscriminator = "t" }

        fun encode(message: NearbyMessage): ByteArray = json.encodeToString(serializer(), message).encodeToByteArray()

        /** Null for anything this version doesn't understand, so an older or newer peer can't crash us. */
        fun decode(bytes: ByteArray): NearbyMessage? =
            runCatching { json.decodeFromString(serializer(), bytes.decodeToString()) }.getOrNull()
    }
}
