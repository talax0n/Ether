package dev.ether.app.nearby

import android.Manifest
import dev.ether.app.data.nearby.NearbyMessage
import dev.ether.app.data.nearby.ShakeFilter
import dev.ether.app.domain.ListingId
import dev.ether.app.ui.nearby.ShakePairing
import dev.ether.app.ui.nearby.nearbyPermissions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakePairingTest {
    private val ana = ListingId("l-ana")
    private val budi = ListingId("l-budi")

    @Test
    fun `my shake pairs with peers whose shake arrived in the last three seconds`() {
        val pairing = ShakePairing()
        assertFalse(pairing.onPeerShake(ana, now = 1_000))
        assertFalse(pairing.onPeerShake(budi, now = 500))

        assertEquals(listOf(ana), pairing.onMyShake(now = 3_900, present = listOf(ana, budi)))
    }

    @Test
    fun `a peer shake pairs when mine came first`() {
        val pairing = ShakePairing()
        assertEquals(emptyList<ListingId>(), pairing.onMyShake(now = 10_000, present = listOf(ana)))

        assertTrue(pairing.onPeerShake(ana, now = 13_000))
        assertFalse(pairing.onPeerShake(budi, now = 13_001))
    }

    @Test
    fun `peers who left the list don't pair`() {
        val pairing = ShakePairing()
        pairing.onPeerShake(ana, now = 0)

        assertEquals(emptyList<ListingId>(), pairing.onMyShake(now = 100, present = emptyList()))
    }

    @Test
    fun `a peer pairs once per window, then again after it`() {
        val pairing = ShakePairing()
        pairing.onMyShake(now = 0, present = listOf(ana))
        assertTrue(pairing.onPeerShake(ana, now = 100))
        assertFalse(pairing.onPeerShake(ana, now = 900))
        assertEquals(emptyList<ListingId>(), pairing.onMyShake(now = 1_000, present = listOf(ana)))

        pairing.onMyShake(now = 10_000, present = listOf(ana))
        assertTrue(pairing.onPeerShake(ana, now = 10_500))
    }
}

class ShakeFilterTest {
    @Test
    fun `a phone at rest is not a shake, a hard jolt is`() {
        val filter = ShakeFilter()
        assertFalse(filter.onSample(0f, 9.8f, 0f, atMs = 0))
        assertFalse(filter.onSample(20f, 9.8f, 0f, atMs = 10)) // about 2.3g
        assertTrue(filter.onSample(30f, 0f, 0f, atMs = 20)) // about 3.1g
    }

    @Test
    fun `jolts within a second are one shake`() {
        val filter = ShakeFilter()
        assertTrue(filter.onSample(30f, 0f, 0f, atMs = 0))
        assertFalse(filter.onSample(-30f, 0f, 0f, atMs = 300))
        assertFalse(filter.onSample(30f, 0f, 0f, atMs = 999))
        assertTrue(filter.onSample(-30f, 0f, 0f, atMs = 1_000))
    }
}

class NearbyMessageTest {
    @Test
    fun `messages survive the trip as bytes`() {
        val hello = NearbyMessage.Hello("c-1", "l-ana", "Ana Putri", "Designer", team = false)
        assertEquals(hello, NearbyMessage.decode(NearbyMessage.encode(hello)))
        assertEquals(NearbyMessage.Shake(42), NearbyMessage.decode(NearbyMessage.encode(NearbyMessage.Shake(42))))
    }

    @Test
    fun `payloads stay small and readable`() {
        assertEquals("""{"t":"shake","sentAtMs":42}""", NearbyMessage.encode(NearbyMessage.Shake(42)).decodeToString())
    }

    @Test
    fun `junk or unknown messages are ignored, not a crash`() {
        assertNull(NearbyMessage.decode("not json".encodeToByteArray()))
        assertNull(NearbyMessage.decode("""{"t":"wave"}""".encodeToByteArray()))
    }
}

class NearbyPermissionsTest {
    @Test
    fun `each API level asks for what Nearby needs there`() {
        assertEquals(listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), nearbyPermissions(26))
        assertEquals(
            listOf(
                Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            nearbyPermissions(31),
        )
        assertEquals(
            listOf(
                Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.NEARBY_WIFI_DEVICES,
            ),
            nearbyPermissions(36),
        )
    }
}
