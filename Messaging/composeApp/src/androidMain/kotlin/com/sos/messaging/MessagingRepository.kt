package com.sos.messaging

import android.bluetooth.BluetoothDevice
import com.sos.messaging.model.Message
import com.sos.messaging.offline.BleGattClient
import com.sos.messaging.offline.BleGattServer
import com.sos.messaging.offline.MeshMessageRouter
import com.sos.messaging.offline.PresenceBroadcaster
import com.sos.messaging.online.MessagingRestClient
import com.sos.messaging.repository.IMessagingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

class MessagingRepository(
    private val restClient: MessagingRestClient,
    private val gattServer: BleGattServer,
    private val gattClient: BleGattClient,
    private val meshRouter: MeshMessageRouter,
    private val isOnline: () -> Boolean
) : IMessagingRepository {

    // Online: POST /api/v1/messages/send
    // Offline: BLE_SEND /p2p/mesh/message via MeshRouter
    override suspend fun sendMessage(message: Message): Result<Unit> {
        return if (isOnline()) {
            restClient.sendMessage(message)
        } else {
            runCatching {
                meshRouter.send(message, gattClient.discoveredDevices.value)
            }
        }
    }

    // Online only: GET /api/v1/messages/{chat_id}
    override suspend fun getMessageHistory(chatId: String): Result<List<Message>> {
        return if (isOnline()) {
            restClient.getMessageHistory(chatId)
        } else {
            Result.failure(IllegalStateException("Message history requires online connectivity"))
        }
    }

    val nearbyPeers: StateFlow<Set<BluetoothDevice>> get() = gattClient.discoveredDevices
}