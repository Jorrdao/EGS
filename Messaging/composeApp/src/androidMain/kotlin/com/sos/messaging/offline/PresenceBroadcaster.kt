package com.sos.messaging.offline

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.sos.messaging.offline.proto.BlePresence
import kotlinx.coroutines.*

// BLE_BROADCAST /p2p/presence — emits {user_id, lat, lng, timestamp} in a loop
@SuppressLint("MissingPermission")
class PresenceBroadcaster(
    private val context: Context,
    private val userId: String,
    private val intervalMs: Long = 5_000L
) {
    private val tag = "PresenceBroadcaster"
    private var job: Job? = null
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    fun start(scope: CoroutineScope, getLocation: () -> Pair<Double, Double>) {
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val (lat, lng) = getLocation()
                broadcast(lat, lng)
                delay(intervalMs)
            }
        }
    }

    private fun broadcast(lat: Double, lng: Double) {
        val presence = BlePresence.newBuilder()
            .setUserId(userId)
            .setLat(lat)
            .setLng(lng)
            .setTimestamp(System.currentTimeMillis())
            .build()

        val bytes = presence.toByteArray()
        if (bytes.size > 26) {   // BLE advertising payload hard limit
            Log.w(tag, "Presence payload too large for advertising; skipping non-connectable broadcast")
            return
        }

        val advertiser = bluetoothManager.adapter.bluetoothLeAdvertiser ?: return
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setConnectable(false)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .addServiceData(ParcelUuid(BleConstants.CHAR_PRESENCE_UUID), bytes)
            .build()

        advertiser.startAdvertising(settings, data, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.d(tag, "Presence broadcast lat=$lat lng=$lng")
            }
            override fun onStartFailure(errorCode: Int) {
                Log.e(tag, "Presence broadcast failed: $errorCode")
            }
        })
    }

    fun stop() { job?.cancel() }
}