package com.messaging.service.offline.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import com.messaging.service.kpi.KpiTracker
import com.messaging.service.offline.repository.OfflineMessagingRepository
import com.messaging.service.proto.BleEnvelope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GATT Peripheral (Server) role.
 *
 * Exposes two characteristics under SERVICE_UUID:
 *   CHAR_WRITE  – remote centrals write BleEnvelope proto bytes here
 *   CHAR_NOTIFY – server pushes incoming messages back to subscribed centrals
 *
 * Write flow:  Central writes → onCharacteristicWriteRequest → repository dispatches
 * Notify flow: repository.incoming → notifyConnectedDevices()
 */
@Singleton
class BleGattServer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothManager: BluetoothManager,
    private val offlineRepo: OfflineMessagingRepository,
    private val kpiTracker: KpiTracker
) {
    private val tag = "BleGattServer"
    private var gattServer: BluetoothGattServer? = null
    private val connectedDevices = CopyOnWriteArraySet<BluetoothDevice>()

    // Buffer for reassembling chunked writes from a single device
    private val writeBuffers = HashMap<String, ByteArray>()

    private val serverCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevices.add(device)
                    kpiTracker.increment(KpiTracker.Key.BLE_CONNECTIONS)
                    Log.d(tag, "Device connected: ${device.address}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedDevices.remove(device)
                    writeBuffers.remove(device.address)
                    Log.d(tag, "Device disconnected: ${device.address}")
                }
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (characteristic.uuid != BleConstants.CHAR_WRITE_UUID) {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                }
                return
            }

            // Reassemble chunks: preparedWrite=true means more chunks coming
            val buffer = if (preparedWrite) {
                val existing = writeBuffers[device.address] ?: ByteArray(0)
                existing + value
            } else {
                value
            }
            writeBuffers[device.address] = buffer

            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }

            // If not a prepared write, process immediately
            if (!preparedWrite) {
                processWrite(device, buffer)
                writeBuffers.remove(device.address)
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
            if (execute) {
                val buffer = writeBuffers[device.address]
                if (buffer != null) {
                    processWrite(device, buffer)
                }
            }
            writeBuffers.remove(device.address)
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            // Client enabling/disabling notifications
            if (descriptor.uuid == BleConstants.CCCD_UUID) {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun start() {
        gattServer = bluetoothManager.openGattServer(context, serverCallback)?.also { server ->
            server.addService(buildGattService())
            Log.d(tag, "GATT server started")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun stop() {
        gattServer?.close()
        gattServer = null
        connectedDevices.clear()
    }

    fun isRunning() = gattServer != null

    /**
     * Push a BleEnvelope to all subscribed connected centrals.
     * Automatically splits payloads larger than MAX_CHUNK_BYTES.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun notifyAll(envelope: BleEnvelope, scope: CoroutineScope) {
        val bytes = envelope.toByteArray()
        val chunks = bytes.toList().chunked(BleConstants.MAX_CHUNK_BYTES) { it.toByteArray() }
        val notifyChar = gattServer
            ?.getService(BleConstants.SERVICE_UUID)
            ?.getCharacteristic(BleConstants.CHAR_NOTIFY_UUID) ?: return

        scope.launch {
            connectedDevices.forEach { device ->
                chunks.forEach { chunk ->
                    notifyChar.value = chunk
                    gattServer?.notifyCharacteristicChanged(device, notifyChar, false)
                }
            }
            kpiTracker.increment(KpiTracker.Key.BLE_MESSAGES_FORWARDED)
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildGattService(): BluetoothGattService {
        val service = BluetoothGattService(
            BleConstants.SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // Write characteristic – central → peripheral
        val writeChar = BluetoothGattCharacteristic(
            BleConstants.CHAR_WRITE_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        // Notify characteristic – peripheral → central
        val notifyChar = BluetoothGattCharacteristic(
            BleConstants.CHAR_NOTIFY_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                    BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        ).also { char ->
            // CCCD descriptor is mandatory for notifications
            char.addDescriptor(
                BluetoothGattDescriptor(
                    BleConstants.CCCD_UUID,
                    BluetoothGattDescriptor.PERMISSION_READ or
                            BluetoothGattDescriptor.PERMISSION_WRITE
                ).apply {
                    value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                }
            )
        }

        service.addCharacteristic(writeChar)
        service.addCharacteristic(notifyChar)
        return service
    }

    private fun processWrite(device: BluetoothDevice, bytes: ByteArray) {
        try {
            val envelope = BleEnvelope.parseFrom(bytes)
            kpiTracker.increment(KpiTracker.Key.BLE_MESSAGES_RECEIVED)
            offlineRepo.handleIncomingEnvelope(device.address, envelope)
            Log.d(tag, "Processed write from ${device.address}: op=${envelope.operation}")
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse envelope from ${device.address}: ${e.message}")
            kpiTracker.increment(KpiTracker.Key.BLE_ERRORS)
        }
    }
}
