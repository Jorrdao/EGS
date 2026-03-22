package com.sos.messaging.offline

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.sos.messaging.offline.proto.BleMeshMessage
import com.sos.messaging.offline.proto.BleMarketplaceRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("MissingPermission")
class BleGattClient(private val context: Context) {

    private val tag = "BleGattClient"
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val connectedGatts = mutableMapOf<String, BluetoothGatt>()

    private val _discoveredDevices = MutableStateFlow<Set<BluetoothDevice>>(emptySet())
    val discoveredDevices: StateFlow<Set<BluetoothDevice>> = _discoveredDevices

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            _discoveredDevices.value += result.device
        }
    }

    fun startScan() {
        val scanner = bluetoothManager.adapter.bluetoothLeScanner ?: return
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(listOf(filter), settings, scanCallback)
        Log.d(tag, "Scan started")
    }

    fun stopScan() {
        bluetoothManager.adapter.bluetoothLeScanner?.stopScan(scanCallback)
    }

    // Write a BLE_SEND /p2p/mesh/message packet to a discovered peer
    fun sendMeshMessage(device: BluetoothDevice, message: BleMeshMessage, onResult: (Boolean) -> Unit) {
        connectAndWrite(device, BleConstants.CHAR_MESH_MESSAGE_UUID, message.toByteArray(), onResult)
    }

    // Write BLE_REQ /p2p/marketplace/sync
    fun requestMarketplaceSync(device: BluetoothDevice, req: BleMarketplaceRequest, onResult: (Boolean) -> Unit) {
        connectAndWrite(device, BleConstants.CHAR_MARKETPLACE_REQ_UUID, req.toByteArray(), onResult)
    }

    private fun connectAndWrite(
        device: BluetoothDevice,
        charUuid: java.util.UUID,
        data: ByteArray,
        onResult: (Boolean) -> Unit
    ) {
        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connectedGatts[device.address] = gatt
                    gatt.discoverServices()
                } else {
                    gatt.close()
                    connectedGatts.remove(device.address)
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                val char = gatt.getService(BleConstants.SERVICE_UUID)
                    ?.getCharacteristic(charUuid)
                if (char == null) {
                    onResult(false)
                    return
                }
                char.value = data
                val written = gatt.writeCharacteristic(char)
                if (!written) onResult(false)
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                onResult(status == BluetoothGatt.GATT_SUCCESS)
                gatt.disconnect()
            }
        }
        device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
    }
}