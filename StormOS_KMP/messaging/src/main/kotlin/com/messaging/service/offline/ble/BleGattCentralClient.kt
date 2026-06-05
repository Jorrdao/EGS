package com.messaging.service.offline.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import com.messaging.service.kpi.KpiTracker
import com.messaging.service.proto.BleEnvelope
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import android.os.Handler
import android.os.Looper
import android.os.Build

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
class BleGattCentralClient constructor(
    private val context: Context,
    private val kpiTracker: KpiTracker
) {
    private val tag = "BleGattClient"

    // address → connected gatt
    private val pool = ConcurrentHashMap<String, BluetoothGatt>()

    // Continuations waiting for a specific operation to complete
    private val connectContinuations = ConcurrentHashMap<String, CancellableContinuation<Boolean>>()
    private val writeContinuations    = ConcurrentHashMap<String, Continuation<Boolean>>()
    private val mtuContinuations      = ConcurrentHashMap<String, Continuation<Int>>()

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
            val ok = withTimeoutOrNull(5_000L) {
                suspendCancellableCoroutine { cont ->
                    writeContinuations[address] = cont
                    writeChar.value = chunk
                    // ALWAYS use WRITE_TYPE_DEFAULT for reliable delivery
                    writeChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    val initiated = gatt.writeCharacteristic(writeChar)
                    if (!initiated) {
                        writeContinuations.remove(address)
                        cont.resume(false)
                    }
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
        pool.values.forEach {
            it.disconnect()
            it.close()
        }
        pool.clear()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.getConnectionState(device, BluetoothProfile.GATT) ==
                BluetoothProfile.STATE_CONNECTED
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun getOrConnect(device: BluetoothDevice): BluetoothGatt? {
        pool[device.address]?.let { existing ->
            if (isDeviceConnected(device)) {
                return existing
            }
            // Stale entry — aggressively close it to prevent leaks
            existing.close()
            pool.remove(device.address)
        }

        // Reduced to 5 seconds. If the radios miss their sync window,
        // failing fast allows the mesh queue to retry on the next scanner tick.
        val connected = withTimeoutOrNull(5_000L) {
            suspendCancellableCoroutine<Boolean> { cont ->
                connectContinuations[device.address] = cont

                // Use a Handler to post to the Main thread instantly.
                // This bypasses Huawei's ZeroHung watchdog because it doesn't suspend the dispatcher.
                Handler(Looper.getMainLooper()).post {
                    val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Force the 1M Physical Layer for maximum Huawei <-> Samsung compatibility
                        device.connectGatt(
                            context,
                            false,
                            buildCallback(),
                            BluetoothDevice.TRANSPORT_LE,
                            BluetoothDevice.PHY_LE_1M_MASK
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        device.connectGatt(context, false, buildCallback(), BluetoothDevice.TRANSPORT_LE)
                    } else {
                        device.connectGatt(context, false, buildCallback())
                    }

                    if (gatt == null) {
                        connectContinuations.remove(device.address)
                        cont.resume(false)
                        return@post
                    }

                    // Clean up if the 5s timeout triggers
                    cont.invokeOnCancellation {
                        Log.w(tag, "Connection to ${device.address} timed out, cleaning up GATT.")
                        gatt?.disconnect()
                        gatt?.close()
                        connectContinuations.remove(device.address)
                    }
                }
            }
        } ?: false

        return if (connected) pool[device.address] else null
    }

    private fun buildCallback() = object : BluetoothGattCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val address = gatt.device.address
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(tag, "Connected to $address, requesting MTU")
                gatt.requestMtu(BleConstants.REQUESTED_MTU)
            } else {
                Log.w(tag, "Disconnected from $address status=$status newState=$newState")
                // FIX 3: Always close the raw gatt instance passed to the callback on failure!
                gatt.close()
                pool.remove(address)?.close()
                connectContinuations.remove(address)?.resume(false)
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val address = gatt.device.address
            Log.d(tag, "MTU changed to $mtu for $address")
            mtuContinuations.remove(address)?.resume(mtu)
            gatt.discoverServices()
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val address = gatt.device.address
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connectContinuations.remove(address)?.resume(false)
                return
            }

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

            Handler(Looper.getMainLooper()).postDelayed({
                connectContinuations.remove(address)?.resume(true)
            }, 750)

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
            Log.d(tag, "Notification received from ${gatt.device.address}: ${characteristic.value?.size} bytes")
            kpiTracker.increment(KpiTracker.Key.BLE_MESSAGES_RECEIVED)
        }
    }
}