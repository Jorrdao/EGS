package com.messaging.service.offline.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import com.messaging.service.kpi.KpiTracker
import com.messaging.service.proto.BleEnvelope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

data class SendResult(val success: Boolean, val error: String? = null)

/**
 * GATT Central (Client) role.
 *
 * Connects to a discovered peripheral (by BLE address), negotiates MTU,
 * enables notifications on CHAR_NOTIFY, and writes BleEnvelope bytes
 * to CHAR_WRITE.
 *
 * A connection pool is maintained so repeated sends to the same peer
 * reuse the existing BluetoothGatt object.
 */
@Singleton
class BleGattCentralClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val kpiTracker: KpiTracker
) {
    private val tag = "BleGattClient"

    // address → connected gatt
    private val pool = ConcurrentHashMap<String, BluetoothGatt>()

    // Continuations waiting for a specific operation to complete
    private val connectContinuations  = ConcurrentHashMap<String, kotlin.coroutines.Continuation<Boolean>>()
    private val writeContinuations    = ConcurrentHashMap<String, kotlin.coroutines.Continuation<Boolean>>()
    private val mtuContinuations      = ConcurrentHashMap<String, kotlin.coroutines.Continuation<Int>>()

    /**
     * Send an envelope to a specific peer by BLE MAC address.
     * Connects (or reuses) a GATT connection, then writes the serialised bytes.
     * Large payloads are automatically chunked as WRITE_NO_RESPONSE.
     */
    
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun sendEnvelope(
        device: BluetoothDevice,
        envelope: BleEnvelope,
        scope: CoroutineScope
    ): SendResult {
        val address = device.address
        val gatt = getOrConnect(device) ?: return SendResult(false, "Connection failed")

        val writeChar = gatt.getService(BleConstants.SERVICE_UUID)
            ?.getCharacteristic(BleConstants.CHAR_WRITE_UUID)
            ?: return SendResult(false, "Characteristic not found")

        val bytes = envelope.toByteArray()
        val chunks = bytes.toList().chunked(BleConstants.MAX_CHUNK_BYTES) { it.toByteArray() }

        for ((index, chunk) in chunks.withIndex()) {
            val isLast = index == chunks.lastIndex
            val ok = withTimeoutOrNull(5_000L) {
                suspendCancellableCoroutine { cont ->
                    writeContinuations[address] = cont
                    writeChar.value = chunk
                    writeChar.writeType = if (isLast)
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT          // with response on last chunk
                    else
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    gatt.writeCharacteristic(writeChar)
                }
            } ?: false

            if (!ok) {
                kpiTracker.increment(KpiTracker.Key.BLE_ERRORS)
                return SendResult(false, "Write timeout on chunk $index")
            }
        }

        kpiTracker.increment(KpiTracker.Key.BLE_MESSAGES_SENT)
        return SendResult(true)
    }
    
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnectAll() {
        pool.values.forEach { it.disconnect(); it.close() }
        pool.clear()
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun getOrConnect(device: BluetoothDevice): BluetoothGatt? {
        pool[device.address]?.let { return it }

        return withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine { cont ->
                connectContinuations[device.address] = cont as Continuation<Boolean>
                device.connectGatt(context, false, buildCallback(), BluetoothDevice.TRANSPORT_LE)
            }
        }
    }

    private fun buildCallback() = object : BluetoothGattCallback() {
        
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val address = gatt.device.address
            if (newState == BluetoothGatt.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(tag, "Connected to $address, requesting MTU")
                gatt.requestMtu(BleConstants.REQUESTED_MTU)
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d(tag, "Disconnected from $address")
                pool.remove(address)
                connectContinuations.remove(address)?.resume(false)
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val address = gatt.device.address
            Log.d(tag, "MTU changed to $mtu for $address")
            mtuContinuations.remove(address)?.resume(mtu)
            // Discover services after MTU negotiation
            gatt.discoverServices()
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val address = gatt.device.address
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connectContinuations.remove(address)?.resume(false)
                return
            }

            // Enable notifications on CHAR_NOTIFY
            val notifyChar = gatt.getService(BleConstants.SERVICE_UUID)
                ?.getCharacteristic(BleConstants.CHAR_NOTIFY_UUID)

            if (notifyChar != null) {
                gatt.setCharacteristicNotification(notifyChar, true)
                val descriptor = notifyChar.getDescriptor(BleConstants.CCCD_UUID)
                descriptor?.let {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(it)
                }
            }

            pool[address] = gatt
            connectContinuations.remove(address)?.resume(true)
            Log.d(tag, "Services discovered for $address")
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val address = gatt.device.address
            writeContinuations.remove(address)?.resume(status == BluetoothGatt.GATT_SUCCESS)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            // Incoming notification from peripheral (inbound mesh message)
            Log.d(tag, "Notification received from ${gatt.device.address}: ${characteristic.value?.size} bytes")
            kpiTracker.increment(KpiTracker.Key.BLE_MESSAGES_RECEIVED)
        }
    }
}
