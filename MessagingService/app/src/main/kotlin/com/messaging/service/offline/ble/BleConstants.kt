package com.messaging.service.offline.ble

import java.util.UUID

/**
 * All UUIDs and constants used by the BLE GATT server and client.
 *
 * Convention: custom 128-bit UUIDs in the form
 *   MSG-XXXX-0000-1000-8000-00805f9b34fb
 */
object BleConstants {

    // ── Service ───────────────────────────────────────────────────────────────
    /** Primary GATT service advertising the Messaging Service */
    val SERVICE_UUID: UUID = UUID.fromString("0000AA00-0000-1000-8000-00805f9b34fb")

    // ── Characteristics ───────────────────────────────────────────────────────

    /** Write: BleEnvelope proto bytes → server dispatches by OperationType */
    val CHAR_WRITE_UUID: UUID = UUID.fromString("0000AA01-0000-1000-8000-00805f9b34fb")

    /** Notify/Read: server pushes BleEnvelope proto bytes to connected centrals */
    val CHAR_NOTIFY_UUID: UUID = UUID.fromString("0000AA02-0000-1000-8000-00805f9b34fb")

    /** Standard Client Characteristic Configuration Descriptor (enable notify) */
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // ── BLE Advertising ───────────────────────────────────────────────────────

    /** 2-byte manufacturer ID embedded in the advertising packet */
    const val MANUFACTURER_ID = 0x05AF

    // ── Mesh defaults ─────────────────────────────────────────────────────────

    /** Default TTL for a mesh message (max hops) */
    const val DEFAULT_TTL = 5

    /** MTU negotiated size. Android default is 23; we request 512 */
    const val REQUESTED_MTU = 512

    /** Max bytes that fit in a single BLE characteristic write without chunking */
    const val MAX_CHUNK_BYTES = 500

    // ── Scan/Advertise intervals ──────────────────────────────────────────────

    /** How often (ms) to re-broadcast presence */
    const val PRESENCE_BROADCAST_INTERVAL_MS = 5_000L

    /** BLE scan window (ms) */
    const val SCAN_WINDOW_MS = 10_000L

    /** BLE scan pause (ms) – battery optimisation */
    const val SCAN_PAUSE_MS = 5_000L
}
