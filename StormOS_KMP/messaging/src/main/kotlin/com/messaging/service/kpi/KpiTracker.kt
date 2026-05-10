package com.messaging.service.kpi

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe KPI tracker.
 *
 * Two responsibilities:
 *   1. In-memory atomic counters — for the /api/v1/kpi HTTP endpoint
 *      and the MainActivity KPI panel.
 *   2. InfluxDB emission — every meaningful event is forwarded to
 *      InfluxDbWriter which batches and flushes to InfluxDB for Grafana.
 *
 * Location context (city/country) is set by MessagingForegroundService
 * whenever a location update arrives, and attached to every metric point.
 */
class KpiTracker(private val influx: InfluxDbWriter) {

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
    private val gson        = Gson()

    // Current location context — updated by the foreground service
    @Volatile var currentCity:    String = "unknown"
    @Volatile var currentCountry: String = "unknown"

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
    }

    /**
     * Records a message event in both the local counter and InfluxDB.
     * Call this instead of increment() for message-related events so
     * location tags are included.
     *
     * @param transport "online" or "ble"
     * @param direction "sent" or "received"
     */
    fun recordMessage(transport: String, direction: String) {
        val key = if (transport == "online") {
            if (direction == "sent") Key.ONLINE_MESSAGES_SENT else Key.ONLINE_MESSAGES_RECEIVED
        } else {
            if (direction == "sent") Key.BLE_MESSAGES_SENT else Key.BLE_MESSAGES_RECEIVED
        }
        increment(key)

        // Emit to InfluxDB with location tags for Grafana geo-analysis
        influx.recordMessage(
            transport = transport,
            direction = direction,
            city      = currentCity,
            country   = currentCountry
        )
    }

    /**
     * Records a BLE peer discovery with location context.
     */
    fun recordPeerDiscovered() {
        increment(Key.BLE_PEERS_DISCOVERED)
        influx.recordPeerDiscovered(currentCity, currentCountry)
    }

    /**
     * Records an HTTP latency sample.
     */
    fun recordHttpLatency(latencyMs: Long, endpoint: String, success: Boolean) {
        recordLatency(Key.ONLINE_LATENCY_MS, latencyMs)
        influx.recordHttpLatency(latencyMs, endpoint, success)
    }

    /**
     * Records a TTL drop with location context.
     */
    fun recordTtlDrop() {
        increment(Key.BLE_TTL_DROPS)
        influx.recordTtlDrop(currentCity, currentCountry)
    }

    /**
     * Pushes a full health snapshot to InfluxDB.
     * Called every 60s by MessagingForegroundService.
     */
    fun pushHealthSnapshot(pendingMessages: Long = 0L) {
        influx.recordHealthSnapshot(
            uptimeSeconds    = uptimeSeconds(),
            bleConnections   = get(Key.BLE_CONNECTIONS),
            peersDiscovered  = get(Key.BLE_PEERS_DISCOVERED),
            messagesSent     = get(Key.ONLINE_MESSAGES_SENT) + get(Key.BLE_MESSAGES_SENT),
            messagesReceived = get(Key.ONLINE_MESSAGES_RECEIVED) + get(Key.BLE_MESSAGES_RECEIVED),
            onlineErrors     = get(Key.ONLINE_REQUEST_ERRORS),
            bleErrors        = get(Key.BLE_ERRORS),
            pendingMessages  = pendingMessages
        )
    }

    /**
     * Updates the current location context.
     * Called by MessagingForegroundService on location updates.
     */
    fun updateLocation(city: String, country: String) {
        currentCity    = city
        currentCountry = country
        Log.d("KpiTracker", "Location updated: $city, $country")
    }

    fun get(key: Key): Long = counters[key.label]?.get() ?: 0L

    fun averageLatencyMs(): Long {
        val count = get(Key.ONLINE_LATENCY_COUNT)
        return if (count > 0) get(Key.ONLINE_LATENCY_TOTAL_MS) / count else 0L
    }

    fun uptimeSeconds(): Long = (System.currentTimeMillis() - startTimeMs) / 1000

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