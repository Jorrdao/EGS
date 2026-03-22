package com.sos.messaging.offline

import android.bluetooth.BluetoothDevice
import android.util.Log
import com.sos.messaging.model.Message
import com.sos.messaging.offline.proto.BleMeshMessage
import com.google.protobuf.ByteString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// Handles BLE_SEND /p2p/mesh/message with Ad-hoc + Store-and-Forward routing
class MeshMessageRouter(
    private val localUserId: String,
    private val gattServer: BleGattServer,
    private val gattClient: BleGattClient
) {
    private val tag = "MeshRouter"
    private val seenMessages = mutableSetOf<String>() // deduplication

    fun start(scope: CoroutineScope, onDelivered: (BleMeshMessage) -> Unit) {
        gattServer.incomingMessages
            .onEach { msg -> handleIncoming(msg, scope, onDelivered) }
            .launchIn(scope)
    }

    private fun handleIncoming(
        msg: BleMeshMessage,
        scope: CoroutineScope,
        onDelivered: (BleMeshMessage) -> Unit
    ) {
        if (msg.messageId in seenMessages) return   // already processed
        seenMessages += msg.messageId

        if (msg.destinationId == localUserId) {
            Log.d(tag, "Message ${msg.messageId} delivered locally")
            onDelivered(msg)
        } else if (msg.ttl > 1) {
            // Store-and-Forward: relay to all known peers with TTL-1
            val relayed = msg.toBuilder().setTtl(msg.ttl - 1).build()
            gattClient.discoveredDevices.value.forEach { peer ->
                gattClient.sendMeshMessage(peer, relayed) { success ->
                    Log.d(tag, "Relayed ${msg.messageId} to ${peer.address}: $success")
                }
            }
        } else {
            Log.d(tag, "Message ${msg.messageId} TTL expired, dropping")
        }
    }

    // Build and send a new outgoing mesh message
    fun send(message: Message, peers: Set<BluetoothDevice>) {
        val proto = BleMeshMessage.newBuilder()
            .setMessageId(message.messageId)
            .setSenderId(message.senderId)
            .setDestinationId(message.destinationId)
            .setPayload(ByteString.copyFrom(message.payload.toByteArray()))
            .setTtl(message.ttl)
            .build()

        peers.forEach { peer ->
            gattClient.sendMeshMessage(peer, proto) { success ->
                Log.d(tag, "Sent ${message.messageId} to ${peer.address}: $success")
            }
        }
    }
}