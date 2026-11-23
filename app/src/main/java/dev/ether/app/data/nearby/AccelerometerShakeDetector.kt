package dev.ether.app.data.nearby

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

/**
 * Turns accelerometer samples into shakes: total acceleration above [thresholdG] (gravity included, so a phone at
 * rest reads 1g), at most one shake per [debounceMs]. Pure, so the thresholds are unit-tested.
 */
class ShakeFilter(private val thresholdG: Float = 2.7f, private val debounceMs: Long = 1_000) {
    private var lastShakeAt: Long? = null

    /** [x], [y], [z] in m/s², [atMs] on a monotonic clock. True when this sample is a new shake. */
    fun onSample(x: Float, y: Float, z: Float, atMs: Long): Boolean {
        val g = sqrt(x * x + y * y + z * z) / GRAVITY
        if (g < thresholdG) return false
        if (lastShakeAt?.let { atMs - it < debounceMs } == true) return false
        lastShakeAt = atMs
        return true
    }

    private companion object {
        /** SensorManager.GRAVITY_EARTH, spelled out so the filter runs in plain JVM tests. */
        const val GRAVITY = 9.80665f
    }
}

/** [ShakeDetector] on the device accelerometer. A device without one never shakes; the tap fallback still works. */
class AccelerometerShakeDetector(private val context: Context) : ShakeDetector {
    override fun shakes(): Flow<Unit> = callbackFlow {
        val sensors = context.getSystemService(SensorManager::class.java)
        val accelerometer = sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensors == null || accelerometer == null) {
            awaitClose()
            return@callbackFlow
        }
        val filter = ShakeFilter()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                // Event timestamps are elapsedRealtimeNanos.
                if (filter.onSample(x, y, z, event.timestamp / 1_000_000)) trySend(Unit)
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        sensors.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensors.unregisterListener(listener) }
    }
}
