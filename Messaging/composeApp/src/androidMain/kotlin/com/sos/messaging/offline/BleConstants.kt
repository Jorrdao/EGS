package com.sos.messaging.offline

import java.util.UUID

object BleConstants {
    // Main service UUID for SOS Messaging
    val SERVICE_UUID: UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB")

    // Characteristic UUIDs — one per BLE operation type
    val CHAR_PRESENCE_UUID: UUID        = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB")
    val CHAR_MESH_MESSAGE_UUID: UUID    = UUID.fromString("0000FFF2-0000-1000-8000-00805F9B34FB")
    val CHAR_MARKETPLACE_REQ_UUID: UUID = UUID.fromString("0000FFF3-0000-1000-8000-00805F9B34FB")
    val CHAR_MARKETPLACE_RES_UUID: UUID = UUID.fromString("0000FFF4-0000-1000-8000-00805F9B34FB")

    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    const val MAX_BLE_PAYLOAD = 512      // bytes — negotiated MTU
    const val DEFAULT_TTL     = 5
}