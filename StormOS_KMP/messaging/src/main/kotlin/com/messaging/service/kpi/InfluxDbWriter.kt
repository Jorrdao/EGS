package com.messaging.service.kpi

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Sends metrics to InfluxDB 2.x using the line protocol HTTP API.
 *
 * Line protocol format:
 *   measurement,tag1=val1,tag2=val2 field1=val1,field2=val2 timestamp_ns
 *
 * Example:
 *   messages,transport=online,city=Aveiro,country=PT count=1i 1713123456789000000
 *
 * Points are buffered in memory and flushed every [flushIntervalMs] milliseconds
 * (default 30s) to avoid hammering the network on every metric event.
 *
 * Configuration — set these to match your InfluxDB instance:
 *   INFLUX_URL    = http://10.0.2.2:8086  (emulator → host machine)
 *   INFLUX_TOKEN  = your InfluxDB API token
 *   INFLUX_ORG    = your org name
 *   INFLUX_BUCKET = stormos
 */
class InfluxDbWriter(
    private val url:    String = DEFAULT_URL,
    private val token:  String = DEFAULT_TOKEN,
    private val org:    String = DEFAULT_ORG,
    private val bucket: String = DEFAULT_BUCKET,
    private val flushIntervalMs: Long = 30_000L
) {
    private val tag = "InfluxDbWriter"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val buffer = mutableListOf<String>()
    private val mutex  = Mutex()

    private val writerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        // Change these to match your InfluxDB setup.
        // For emulator testing: 10.0.2.2 maps to your host machine's localhost.
        const val DEFAULT_URL    = "http://193.136.82.35"
        const val DEFAULT_TOKEN  = "stormos-super-secret-token"
        const val DEFAULT_ORG    = "stormos"
        const val DEFAULT_BUCKET = "stormos"
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    fun start() {
        writerScope.launch {
            while (isActive) {
                delay(flushIntervalMs)
                flush()
            }
        }
        Log.i(tag, "InfluxDB writer started — flushing every ${flushIntervalMs / 1000}s to $url")
    }

    fun stop() {
        writerScope.launch { flush() }   // flush remaining points on shutdown
    }

    /**
     * Queues a message metric point.
     *
     * @param transport  "online" or "ble"
     * @param direction  "sent" or "received"
     * @param city       city name or "unknown"
     * @param country    ISO country code or "unknown"
     */
    fun recordMessage(
        transport: String,
        direction: String,
        city:      String = "unknown",
        country:   String = "unknown"
    ) {
        enqueue(buildPoint(
            measurement = "messages",
            tags   = mapOf(
                "transport" to transport,
                "direction" to direction,
                "city"      to sanitizeTag(city),
                "country"   to sanitizeTag(country)
            ),
            fields = mapOf("count" to "1i")
        ))
    }

    /**
     * Queues a BLE peer discovery event.
     */
    fun recordPeerDiscovered(city: String = "unknown", country: String = "unknown") {
        enqueue(buildPoint(
            measurement = "ble_peers",
            tags   = mapOf("city" to sanitizeTag(city), "country" to sanitizeTag(country)),
            fields = mapOf("count" to "1i")
        ))
    }

    /**
     * Queues a service health snapshot.
     * Called periodically (every 60s) from MessagingForegroundService.
     */
    fun recordHealthSnapshot(
        uptimeSeconds:    Long,
        bleConnections:   Long,
        peersDiscovered:  Long,
        messagesSent:     Long,
        messagesReceived: Long,
        onlineErrors:     Long,
        bleErrors:        Long,
        pendingMessages:  Long
    ) {
        enqueue(buildPoint(
            measurement = "service_health",
            tags   = emptyMap(),
            fields = mapOf(
                "uptime_seconds"    to "${uptimeSeconds}i",
                "ble_connections"   to "${bleConnections}i",
                "peers_discovered"  to "${peersDiscovered}i",
                "messages_sent"     to "${messagesSent}i",
                "messages_received" to "${messagesReceived}i",
                "online_errors"     to "${onlineErrors}i",
                "ble_errors"        to "${bleErrors}i",
                "pending_messages"  to "${pendingMessages}i"
            )
        ))
    }

    /**
     * Queues an HTTP latency sample.
     */
    fun recordHttpLatency(latencyMs: Long, endpoint: String, success: Boolean) {
        enqueue(buildPoint(
            measurement = "http_latency",
            tags   = mapOf(
                "endpoint" to sanitizeTag(endpoint),
                "success"  to success.toString()
            ),
            fields = mapOf("latency_ms" to "${latencyMs}i")
        ))
    }

    /**
     * Queues a BLE mesh TTL drop event.
     */
    fun recordTtlDrop(city: String = "unknown", country: String = "unknown") {
        enqueue(buildPoint(
            measurement = "ble_ttl_drops",
            tags   = mapOf("city" to sanitizeTag(city), "country" to sanitizeTag(country)),
            fields = mapOf("count" to "1i")
        ))
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun enqueue(line: String) {
        writerScope.launch {
            mutex.withLock { buffer.add(line) }
            // Flush immediately if buffer is large
            if (buffer.size >= 100) flush()
        }
    }

    private suspend fun flush() {
        val lines = mutex.withLock {
            if (buffer.isEmpty()) return
            val copy = buffer.toList()
            buffer.clear()
            copy
        }

        val body = lines.joinToString("\n")
        val request = Request.Builder()
            .url("$url/api/v2/write?org=$org&bucket=$bucket&precision=ns")
            .addHeader("Authorization", "Token $token")
            .addHeader("Content-Type", "text/plain; charset=utf-8")
            .addHeader("Host", "stormos-influxdb-103075.deti.ua.pt")
            .post(body.toRequestBody("text/plain".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(tag, "Flushed ${lines.size} points to InfluxDB")
                } else {
                    Log.w(tag, "InfluxDB write failed: ${response.code} ${response.message}")
                    // Re-queue on server errors (rate limit, etc.)
                    if (response.code >= 500) {
                        mutex.withLock { buffer.addAll(0, lines) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "InfluxDB unreachable — ${lines.size} points buffered: ${e.message}")
            // Re-queue on network errors so no data is lost
            mutex.withLock { buffer.addAll(0, lines) }
        }
    }

    private fun buildPoint(
        measurement: String,
        tags:        Map<String, String>,
        fields:      Map<String, String>
    ): String {
        val tagStr   = if (tags.isEmpty()) "" else "," + tags.entries.joinToString(",") { "${it.key}=${it.value}" }
        val fieldStr = fields.entries.joinToString(",") { "${it.key}=${it.value}" }
        val tsNs     = System.currentTimeMillis() * 1_000_000L  // millis → nanoseconds
        return "$measurement$tagStr $fieldStr $tsNs"
    }

    // InfluxDB tags cannot contain spaces, commas or equals signs
    private fun sanitizeTag(value: String): String =
        value.replace(" ", "_").replace(",", "").replace("=", "").ifBlank { "unknown" }
}
