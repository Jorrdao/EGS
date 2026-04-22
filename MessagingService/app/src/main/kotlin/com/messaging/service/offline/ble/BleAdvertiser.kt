package com.messaging.service.offline.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import android.util.Log
import com.messaging.service.kpi.KpiTracker
import com.messaging.service.proto.BlePresence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BLE_BROADCAST /p2p/presence
 *
 * Periodically advertises this device's presence (user_id, lat, lng, timestamp)
 * as a BLE advertising packet. The presence proto is serialised and embedded
 * in the manufacturer-specific data field.
 *
 * NOTE: On Android, advertising packets are limited to ~31 bytes for legacy
 * advertising. We encode only the first 26 bytes of the proto; for full
 * presence data we rely on the GATT NOTIFY characteristic.
 */
@Singleton
class BleAdvertiser @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val kpiTracker: KpiTracker
) {
    private val tag = "BleAdvertiser"
    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseJob: Job? = null
    private var currentCallback: AdvertiseCallback? = null

    private var userId: String = ""
    private var lat: Double = 0.0
    private var lng: Double = 0.0

    fun start(
        userId: String,
        lat: Double,
        lng: Double,
        scope: CoroutineScope
    ) {
        this.userId = userId
        this.lat = lat
        this.lng = lng

        advertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.e(tag, "Device does not support BLE advertising")
            return
        }

        advertiseJob?.cancel()
        advertiseJob = scope.launch {
            while (isActive) {
                broadcastPresence()
                delay(BleConstants.PRESENCE_BROADCAST_INTERVAL_MS)
            }
        }
    }

    fun updatePosition(lat: Double, lng: Double) {
        this.lat = lat
        this.lng = lng
    }
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE)
    fun stop() {
        advertiseJob?.cancel()
        currentCallback?.let { advertiser?.stopAdvertising(it) }
        currentCallback = null
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun broadcastPresence() {
        val presence = BlePresence.newBuilder()
            .setUserId(userId)
            .setLat(lat)
            .setLng(lng)
            .setTimestamp(System.currentTimeMillis())
            .build()

        // Trim to MAX 26 bytes for legacy advertising manufacturer data
        val raw = presence.toByteArray().take(26).toByteArray()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)           // peripheral must be connectable for GATT
            .setTimeout(0)                  // advertise indefinitely
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .addManufacturerData(BleConstants.MANUFACTURER_ID, raw)
            .setIncludeDeviceName(false)    // saves bytes
            .build()

        currentCallback?.let { advertiser?.stopAdvertising(it) }

        val cb = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                kpiTracker.increment(KpiTracker.Key.BLE_PRESENCE_BROADCASTS)
                Log.d(tag, "Presence advertised: lat=$lat lng=$lng")
            }
            override fun onStartFailure(errorCode: Int) {
                Log.e(tag, "Advertise failed: $errorCode")
                kpiTracker.increment(KpiTracker.Key.BLE_ERRORS)
            }
        }
        currentCallback = cb
        advertiser?.startAdvertising(settings, data, cb)
    }
}
