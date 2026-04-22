package com.messaging.service.offline.repository

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.annotation.RequiresPermission
import com.messaging.service.db.MessageDao
import com.messaging.service.db.MessageEntity
import com.messaging.service.kpi.KpiTracker
import com.messaging.service.offline.ble.BleConstants
import com.messaging.service.offline.ble.BleGattCentralClient
import com.messaging.service.offline.ble.BleScanner
import com.messaging.service.offline.ble.DiscoveredPeer
import com.messaging.service.offline.crypto.MessageEncryption
import com.messaging.service.proto.BleEnvelope
import com.messaging.service.proto.BleMeshMessage
import com.messaging.service.proto.BleMarketplaceSyncRequest
import com.messaging.service.proto.BleMarketplaceSyncResponse
import com.messaging.service.proto.BlePresence
import com.messaging.service.proto.MarketplaceItemProto
import com.messaging.service.proto.MessageType
import com.google.protobuf.ByteString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dispatches all offline BLE operations and implements the mesh routing logic.
 *
 * Routing strategy:
 *  1. Check if destination is a directly connected/recently-seen peer → send direct.
 *  2. Otherwise broadcast to all connected peers with TTL decremented.
 *     Each hop repeats this logic until TTL=0 or destination reached.
 *  3. Store-and-forward: if no peer is available, persist locally and retry
 *     when a new peer is discovered.
 */
@Singleton
class OfflineMessagingRepository @Inject constructor(
    private val bleScanner: BleScanner,
    private val gattClient: BleGattCentralClient,
    private val encryption: MessageEncryption,
    private val messageDao: MessageDao,
    private val kpiTracker: KpiTracker,
    private val bluetoothAdapter: BluetoothAdapter
) {
    private val tag = "OfflineRepo"

    // Emits decrypted incoming mesh messages for the service layer to consume
    private val _incomingMessages = MutableSharedFlow<BleMeshMessage>(replay = 10)
    val incomingMessages: SharedFlow<BleMeshMessage> = _incomingMessages

    // Peer address → presence snapshot (updated by scanner)
    private val knownPeers = HashMap<String, DiscoveredPeer>()

    // Pending store-and-forward queue (destinationId → list of envelopes)
    private val pendingQueue = HashMap<String, MutableList<BleEnvelope>>()

    fun startObservingPeers(scope: CoroutineScope) {
        scope.launch {
            bleScanner.peers.collect { peer ->
                knownPeers[peer.address] = peer
                Log.d(tag, "Peer updated: ${peer.userId} @ ${peer.address}")
                // Flush any pending messages for this peer
                flushPendingFor(peer, scope)
            }
        }
    }

    // ── BLE_SEND /p2p/mesh/message ────────────────────────────────────────────

    /**
     * Sends an encrypted mesh message.
     * @param destinationId  Final recipient user_id
     * @param plaintext      Raw message text – will be AES-256-GCM encrypted
     * @param ttl            Max hops (default = BleConstants.DEFAULT_TTL)
     */
    suspend fun sendMeshMessage(
        destinationId: String,
        plaintext: String,
        ttl: Int = BleConstants.DEFAULT_TTL,
        senderId: String,
        scope: CoroutineScope
    ) {
        val encryptedBytes = encryption.encrypt(plaintext.toByteArray(Charsets.UTF_8))
        val meshMsg = BleMeshMessage.newBuilder()
            .setMessageId(UUID.randomUUID().toString())
            .setSenderId(senderId)
            .setDestinationId(destinationId)
            .setPayload(ByteString.copyFrom(encryptedBytes))
            .setTtl(ttl)
            .setTimestamp(System.currentTimeMillis())
            .setType(MessageType.CHAT)
            .build()

        val envelope = BleEnvelope.newBuilder()
            .setOperation(BleEnvelope.OperationType.MESH_MESSAGE)
            .setData(meshMsg.toByteString())
            .build()

        routeEnvelope(destinationId, envelope, scope)
    }

    // ── BLE_BROADCAST /p2p/presence (called by BleAdvertiser) ────────────────

    fun buildPresenceEnvelope(userId: String, lat: Double, lng: Double): BleEnvelope {
        val presence = BlePresence.newBuilder()
            .setUserId(userId)
            .setLat(lat)
            .setLng(lng)
            .setTimestamp(System.currentTimeMillis())
            .build()
        return BleEnvelope.newBuilder()
            .setOperation(BleEnvelope.OperationType.PRESENCE)
            .setData(presence.toByteString())
            .build()
    }

    // ── BLE_REQ /p2p/marketplace/sync ─────────────────────────────────────────

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun requestMarketplaceSync(
        requesterId: String,
        lastSyncTs: Long,
        targetAddress: String,
        scope: CoroutineScope
    ) {
        val req = BleMarketplaceSyncRequest.newBuilder()
            .setRequesterId(requesterId)
            .setLastSyncTs(lastSyncTs)
            .build()
        val envelope = BleEnvelope.newBuilder()
            .setOperation(BleEnvelope.OperationType.MARKETPLACE_REQ)
            .setData(req.toByteString())
            .build()

        val device = bluetoothAdapter.getRemoteDevice(targetAddress)
        gattClient.sendEnvelope(device, envelope, scope)
        kpiTracker.increment(KpiTracker.Key.BLE_MARKETPLACE_SYNCS)
    }

    // ── Incoming envelope dispatcher (called by BleGattServer) ───────────────

    fun handleIncomingEnvelope(fromAddress: String, envelope: BleEnvelope) {
        Log.d(tag, "Incoming envelope op=${envelope.operation} from=$fromAddress")
        when (envelope.operation) {
            BleEnvelope.OperationType.PRESENCE -> handlePresence(fromAddress, envelope)
            BleEnvelope.OperationType.MARKETPLACE_REQ -> handleMarketplaceReq(fromAddress, envelope)
            BleEnvelope.OperationType.MARKETPLACE_RES -> handleMarketplaceRes(envelope)
            BleEnvelope.OperationType.MESH_MESSAGE -> handleMeshMessage(fromAddress, envelope)
            else -> Log.w(tag, "Unknown operation: ${envelope.operation}")
        }
    }

    // ── Private routing ────────────────────────────────────────────────────────

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun routeEnvelope(
        destinationId: String,
        envelope: BleEnvelope,
        scope: CoroutineScope
    ) {
        // Find directly visible peer by userId
        val directPeer = knownPeers.values.firstOrNull { it.userId == destinationId }

        if (directPeer != null) {
            val device = bluetoothAdapter.getRemoteDevice(directPeer.address)
            val result = gattClient.sendEnvelope(device, envelope, scope)
            if (!result.success) {
                Log.w(tag, "Direct send failed, queueing: ${result.error}")
                queueForLater(destinationId, envelope)
            }
        } else {
            // Ad-hoc: flood to all known peers with TTL decremented
            if (knownPeers.isEmpty()) {
                Log.d(tag, "No peers available, storing for later: $destinationId")
                queueForLater(destinationId, envelope)
            } else {
                val meshMsg = BleMeshMessage.parseFrom(envelope.data)
                if (meshMsg.ttl <= 0) {
                    Log.d(tag, "TTL exhausted, dropping message")
                    kpiTracker.increment(KpiTracker.Key.BLE_TTL_DROPS)
                    return
                }
                // Rebuild envelope with decremented TTL
                val forwardMsg = meshMsg.toBuilder().setTtl(meshMsg.ttl - 1).build()
                val forwardEnvelope = envelope.toBuilder()
                    .setData(forwardMsg.toByteString()).build()

                knownPeers.values.forEach { peer ->
                    scope.launch {
                        val device = bluetoothAdapter.getRemoteDevice(peer.address)
                        gattClient.sendEnvelope(device, forwardEnvelope, scope)
                    }
                }
                kpiTracker.increment(KpiTracker.Key.BLE_MESSAGES_FORWARDED)
            }
        }
    }

    private fun queueForLater(destinationId: String, envelope: BleEnvelope) {
        pendingQueue.getOrPut(destinationId) { mutableListOf() }.add(envelope)
    }
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    private fun flushPendingFor(peer: DiscoveredPeer, scope: CoroutineScope) {
        val pending = pendingQueue.remove(peer.userId) ?: return
        Log.d(tag, "Flushing ${pending.size} pending messages to ${peer.userId}")
        val device = bluetoothAdapter.getRemoteDevice(peer.address)
        pending.forEach { envelope ->
            scope.launch { gattClient.sendEnvelope(device, envelope, scope) }
        }
    }

    // ── Incoming handlers ──────────────────────────────────────────────────────

    private fun handlePresence(fromAddress: String, envelope: BleEnvelope) {
        try {
            val presence = BlePresence.parseFrom(envelope.data)
            knownPeers[fromAddress] = DiscoveredPeer(
                address   = fromAddress,
                userId    = presence.userId,
                lat       = presence.lat,
                lng       = presence.lng,
                rssi      = 0,
                timestamp = presence.timestamp
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse presence: ${e.message}")
        }
    }

    private fun handleMarketplaceReq(fromAddress: String, envelope: BleEnvelope) {
        // The actual marketplace data is served by the Jordão service.
        // Here we just log the request; Jordão's sync endpoint handles the response.
        Log.d(tag, "Marketplace sync request from $fromAddress")
        kpiTracker.increment(KpiTracker.Key.BLE_MARKETPLACE_SYNCS)
    }

    private fun handleMarketplaceRes(envelope: BleEnvelope) {
        try {
            val res = BleMarketplaceSyncResponse.parseFrom(envelope.data)
            Log.d(tag, "Received ${res.itemsCount} marketplace items (chunk ${res.chunk}/${res.totalChunks})")
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse marketplace response: ${e.message}")
        }
    }

    private fun handleMeshMessage(fromAddress: String, envelope: BleEnvelope) {
        try {
            val msg = BleMeshMessage.parseFrom(envelope.data)
            val myUserId = "SELF" // Replace with actual user ID from preferences

            if (msg.destinationId == myUserId) {
                // This message is for us – decrypt and emit
                val plaintext = encryption.decrypt(msg.payload.toByteArray())
                Log.d(tag, "Message received from ${msg.senderId}: ${plaintext.size} bytes")
                _incomingMessages.tryEmit(msg)
                kpiTracker.increment(KpiTracker.Key.BLE_MESSAGES_RECEIVED)
            } else if (msg.ttl > 0) {
                // Not for us; forward if TTL allows
                Log.d(tag, "Forwarding mesh message to ${msg.destinationId}, TTL=${msg.ttl}")
                kpiTracker.increment(KpiTracker.Key.BLE_MESSAGES_FORWARDED)
                // Re-routing is handled asynchronously via the service layer
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to handle mesh message: ${e.message}")
        }
    }
}
