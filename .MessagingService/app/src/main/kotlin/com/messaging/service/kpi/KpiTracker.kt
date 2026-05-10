package com.messaging.service.kpi

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe KPI (Key Performance Indicator) tracker.
 *
 * Counters and latency histograms are kept in memory and exposed via:
 *   - [getSnapshot] → JSON string (for AIDL / debugging)
 *   - [metricsFlow]  → StateFlow<Map<String, Long>> (for internal observers)
 *
 * Metrics are also logged via Logcat at INFO level so they appear
 * in Android Studio's profiler / Logcat filter "KpiTracker".
 */
@Singleton
class KpiTracker @Inject constructor() {

    // ── Metric keys ───────────────────────────────────────────────────────────
    enum class Key(val label: String) {
        // Online
        ONLINE_MESSAGES_SENT       ("online.messages.sent"),
        ONLINE_MESSAGES_RECEIVED   ("online.messages.received"),
        ONLINE_REQUESTS_SUCCESS    ("online.requests.success"),
        ONLINE_REQUEST_ERRORS      ("online.requests.errors"),
        ONLINE_LATENCY_MS          ("online.latency.last_ms"),
        ONLINE_LATENCY_TOTAL_MS    ("online.latency.total_ms"),
        ONLINE_LATENCY_COUNT       ("online.latency.count"),

        // BLE / Offline
        BLE_PRESENCE_BROADCASTS    ("ble.presence.broadcasts"),
        BLE_PEERS_DISCOVERED       ("ble.peers.discovered"),
        BLE_CONNECTIONS            ("ble.connections"),
        BLE_MESSAGES_SENT          ("ble.messages.sent"),
        BLE_MESSAGES_RECEIVED      ("ble.messages.received"),
        BLE_MESSAGES_FORWARDED     ("ble.messages.forwarded"),
        BLE_MARKETPLACE_SYNCS      ("ble.marketplace.syncs"),
        BLE_TTL_DROPS              ("ble.ttl.drops"),
        BLE_ERRORS                 ("ble.errors"),

        // Service health
        SERVICE_UPTIME_SECONDS     ("service.uptime.seconds"),
        SERVICE_RESTARTS           ("service.restarts"),
    }

    private val counters = ConcurrentHashMap<String, AtomicLong>().also { map ->
        Key.entries.forEach { key -> map[key.label] = AtomicLong(0L) }
    }

    private val _metricsFlow = MutableStateFlow<Map<String, Long>>(emptyMap())
    val metricsFlow: StateFlow<Map<String, Long>> = _metricsFlow

    private val startTimeMs = System.currentTimeMillis()
    private val gson = Gson()

    // ── Public API ─────────────────────────────────────────────────────────────

    fun increment(key: Key, amount: Long = 1L) {
        counters[key.label]?.addAndGet(amount)
        publish(key.label, counters[key.label]?.get() ?: 0)
        Log.i("KpiTracker", "${key.label} += $amount")
    }

    fun recordLatency(key: Key, latencyMs: Long) {
        counters[key.label]?.set(latencyMs)
        counters[Key.ONLINE_LATENCY_TOTAL_MS.label]?.addAndGet(latencyMs)
        counters[Key.ONLINE_LATENCY_COUNT.label]?.incrementAndGet()
        publish(key.label, latencyMs)
        Log.i("KpiTracker", "${key.label} = ${latencyMs}ms")
    }

    fun get(key: Key): Long = counters[key.label]?.get() ?: 0L

    fun averageLatencyMs(): Long {
        val count = get(Key.ONLINE_LATENCY_COUNT)
        return if (count > 0) get(Key.ONLINE_LATENCY_TOTAL_MS) / count else 0L
    }

    fun uptimeSeconds(): Long = (System.currentTimeMillis() - startTimeMs) / 1000

    /**
     * Returns a JSON string of all current metric values.
     * Called by IMessagingService.getKpiSnapshot().
     */
    fun getSnapshot(): String {
        val snapshot = mutableMapOf<String, Long>()
        counters.forEach { (label, counter) -> snapshot[label] = counter.get() }
        snapshot["service.uptime.seconds"] = uptimeSeconds()
        snapshot["online.latency.avg_ms"]  = averageLatencyMs()
        return gson.toJson(snapshot)
    }

    private fun publish(label: String, value: Long) {
        _metricsFlow.tryEmit(mapOf(label to value))
    }
}
