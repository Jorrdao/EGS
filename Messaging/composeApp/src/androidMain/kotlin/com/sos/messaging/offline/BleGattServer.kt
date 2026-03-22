package com.sos.messaging.offline

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.sos.messaging.offline.proto.BleMarketplaceRequest
import com.sos.messaging.offline.proto.BleMeshMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@SuppressLint("MissingPermission")
class BleGattServer(private val context: Context) {

    private val tag = "BleGattServer"
    private var gattServer: BluetoothGattServer? = null

    private val _incomingMessages = MutableSharedFlow<BleMeshMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<BleMeshMessage> = _incomingMessages

    private val _marketplaceRequests = MutableSharedFlow<BleMarketplaceRequest>(extraBufferCapacity = 16)
    val marketplaceRequests: SharedFlow<BleMarketplaceRequest> = _marketplaceRequests

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.d(tag, "Device ${device.address} state=$newState")
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            when (characteristic.uuid) {
                BleConstants.CHAR_MESH_MESSAGE_UUID -> {
                    val msg = runCatching { BleMeshMessage.parseFrom(value) }.getOrNull()
                    if (msg != null) _incomingMessages.tryEmit(msg)
                }
                BleConstants.CHAR_MARKETPLACE_REQ_UUID -> {
                    val req = runCatching { BleMarketplaceRequest.parseFrom(value) }.getOrNull()
                    if (req != null) _marketplaceRequests.tryEmit(req)
                }
            }
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
        }
    }

    fun start() {
        val service = BluetoothGattService(BleConstants.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        fun makeWritableChar(uuid: java.util.UUID) = BluetoothGattCharacteristic(
            uuid,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        ).also { char ->
            char.addDescriptor(
                BluetoothGattDescriptor(BleConstants.CCCD_UUID, BluetoothGattDescriptor.PERMISSION_WRITE)
            )
        }

        service.addCharacteristic(makeWritableChar(BleConstants.CHAR_MESH_MESSAGE_UUID))
        service.addCharacteristic(makeWritableChar(BleConstants.CHAR_MARKETPLACE_REQ_UUID))
        service.addCharacteristic(makeWritableChar(BleConstants.CHAR_MARKETPLACE_RES_UUID))
        service.addCharacteristic(makeWritableChar(BleConstants.CHAR_PRESENCE_UUID))

        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        gattServer?.addService(service)
        Log.d(tag, "GATT Server started")

        startAdvertising()
    }

    // Notify a connected device with a marketplace response
    fun notifyMarketplaceResponse(device: BluetoothDevice, data: ByteArray) {
        val char = gattServer
            ?.getService(BleConstants.SERVICE_UUID)
            ?.getCharacteristic(BleConstants.CHAR_MARKETPLACE_RES_UUID) ?: return
        char.value = data
        gattServer?.notifyCharacteristicChanged(device, char, false)
    }

    private fun startAdvertising() {
        val advertiser = bluetoothManager.adapter.bluetoothLeAdvertiser ?: return
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .setIncludeDeviceName(false)
            .build()
        advertiser.startAdvertising(settings, data, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.d(tag, "Advertising started")
            }
            override fun onStartFailure(errorCode: Int) {
                Log.e(tag, "Advertising failed: $errorCode")
            }
        })
    }

    fun stop() {
        gattServer?.close()
        bluetoothManager.adapter.bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
    }
}