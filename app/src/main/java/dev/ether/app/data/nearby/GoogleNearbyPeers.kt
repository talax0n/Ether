package dev.ether.app.data.nearby

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.gms.tasks.Task
import dev.ether.app.domain.ListingId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [NearbyPeers] on Google Nearby Connections, P2P_CLUSTER: every phone advertises and discovers at once.
 *
 * The endpoint name is the listing id. When two phones find each other, the one with the smaller listing id asks
 * to connect, so they don't both request at once. Connections are accepted without a token: all a connection can
 * do is offer a swipe, which still needs both people to shake and passes every rule in Postgres.
 * Callbacks arrive on the main thread, like every call into this class.
 */
class GoogleNearbyPeers(context: Context, private val clock: () -> Long) : NearbyPeers {
    private val client = Nearby.getConnectionsClient(context)
    private val main = Handler(Looper.getMainLooper())

    private val _peers = MutableStateFlow<List<NearbyPeer>>(emptyList())
    override val peers: StateFlow<List<NearbyPeer>> = _peers.asStateFlow()
    private val _shakes = MutableSharedFlow<ListingId>(extraBufferCapacity = 16)
    override val shakes: Flow<ListingId> = _shakes.asSharedFlow()

    private var me: NearbyIdentity? = null
    private val connected = mutableSetOf<String>()
    /** Endpoint id → peer, once its hello arrived and it's in our competition. Insertion-ordered. */
    private val known = linkedMapOf<String, NearbyPeer>()

    private val payloads = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val self = me ?: return
            when (val message = payload.asBytes()?.let(NearbyMessage::decode)) {
                is NearbyMessage.Hello ->
                    if (message.competitionId == self.competitionId.value && message.listingId != self.listingId.value) {
                        known[endpointId] = message.toPeer()
                        publish()
                    } else {
                        // Another competition's deck: nothing to match here.
                        client.disconnectFromEndpoint(endpointId)
                    }
                is NearbyMessage.Shake -> known[endpointId]?.let { _shakes.tryEmit(it.listingId) }
                null -> Unit
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            client.acceptConnection(endpointId, payloads)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            val self = me ?: return
            if (resolution.status.statusCode != ConnectionsStatusCodes.STATUS_OK) return
            connected += endpointId
            client.sendPayload(endpointId, Payload.fromBytes(NearbyMessage.encode(NearbyMessage.Hello.of(self))))
        }

        override fun onDisconnected(endpointId: String) {
            connected -= endpointId
            if (known.remove(endpointId) != null) publish()
        }
    }

    private val discovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val self = me ?: return
            if (info.serviceId != SERVICE_ID || info.endpointName == self.listingId.value) return
            if (self.listingId.value < info.endpointName) connect(endpointId, attempt = 1)
        }

        override fun onEndpointLost(endpointId: String) = Unit
    }

    private fun connect(endpointId: String, attempt: Int) {
        val self = me ?: return
        if (endpointId in connected) return
        client.requestConnection(self.listingId.value, endpointId, lifecycle).addOnFailureListener { e ->
            val code = (e as? ApiException)?.statusCode
            if (code == ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT || attempt >= MAX_CONNECT_ATTEMPTS) return@addOnFailureListener
            main.postDelayed({ connect(endpointId, attempt + 1) }, RETRY_MS * attempt)
        }
    }

    override suspend fun start(me: NearbyIdentity) {
        this.me = me
        val strategy = Strategy.P2P_CLUSTER
        try {
            client.startAdvertising(me.listingId.value, SERVICE_ID, lifecycle, AdvertisingOptions.Builder().setStrategy(strategy).build())
                .awaitOrAlready(ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING)
            client.startDiscovery(SERVICE_ID, discovery, DiscoveryOptions.Builder().setStrategy(strategy).build())
                .awaitOrAlready(ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING)
        } catch (e: ApiException) {
            stop()
            throw NearbyUnavailable(
                when (e.statusCode) {
                    ConnectionsStatusCodes.STATUS_BLUETOOTH_ERROR -> "Bluetooth isn't working. Turn it on, then try again."
                    ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH,
                    ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_ADMIN,
                    ConnectionsStatusCodes.MISSING_PERMISSION_ACCESS_WIFI_STATE,
                    ConnectionsStatusCodes.MISSING_PERMISSION_CHANGE_WIFI_STATE,
                    ConnectionsStatusCodes.MISSING_PERMISSION_ACCESS_COARSE_LOCATION,
                    -> "Ether is missing a nearby-devices permission. Allow it in Settings, then try again."
                    else -> "Couldn't look for people nearby. Turn on Bluetooth and Wi-Fi, then try again."
                },
            )
        }
    }

    override fun stop() {
        me = null
        main.removeCallbacksAndMessages(null)
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        connected.clear()
        known.clear()
        publish()
    }

    override fun sendShake() {
        if (connected.isEmpty()) return
        client.sendPayload(connected.toList(), Payload.fromBytes(NearbyMessage.encode(NearbyMessage.Shake(clock()))))
    }

    private fun publish() {
        _peers.value = known.values.toList()
    }

    private suspend fun Task<Void>.awaitOrAlready(already: Int) = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(Unit) }
        addOnFailureListener { e ->
            if ((e as? ApiException)?.statusCode == already) cont.resume(Unit) else cont.resumeWithException(e)
        }
    }

    companion object {
        const val SERVICE_ID = "dev.ether.app.nearby"
        private const val MAX_CONNECT_ATTEMPTS = 3
        private const val RETRY_MS = 1_500L
    }
}
