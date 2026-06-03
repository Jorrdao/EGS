package com.messaging.service.offline.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import com.messaging.service.kpi.KpiTracker
import com.messaging.service.proto.BlePresence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DiscoveredPeer(
    val address: String,
    val userId: String,
    val lat: Double,
    val lng: Double,
    val rssi: Int,
    val timestamp: Long
)

/**
 * Scans for nearby BLE devices that advertise our Messaging Service UUID.
 * Parses the manufacturer-specific data to extract BlePresence proto.
 *
 * Implements duty-cycling (scan/pause) to preserve battery.
 */
class BleScanner constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val kpiTracker: KpiTracker
) {
    private val tag = "BleScanner"

    private val _peers = MutableSharedFlow<DiscoveredPeer>(replay = 20)
    val peers: SharedFlow<DiscoveredPeer> = _peers

    private var scanJob: Job? = null
    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            processScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { processScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(tag, "Scan failed: $errorCode")
            kpiTracker.increment(KpiTracker.Key.BLE_ERRORS)
            isScanning = false
        }
    }
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    fun start(scope: CoroutineScope) {
        scanJob?.cancel()
        scanJob = scope.launch {
            while (isActive) {
                startScan()
                delay(BleConstants.SCAN_WINDOW_MS)
                stopScan()
                delay(BleConstants.SCAN_PAUSE_MS)
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stop() {
        scanJob?.cancel()
        stopScan()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun startScan() {
        if (isScanning) return
        val scanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            Log.e(tag, "BLE scanner not available")
            return
        }

        // Filter by manufacturer ID — present in the main advertising packet
        // No service UUID filter since we moved it out to make room
        val filter = ScanFilter.Builder()
            .setManufacturerData(
                BleConstants.MANUFACTURER_ID,
                ByteArray(1) { 0 },   // 1-byte pattern
                ByteArray(1) { 0 }    // zero mask = match any data with our manufacturer ID
            )
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)
        isScanning = true
        Log.d(tag, "BLE scan started")
    }

    private fun processScanResult(result: ScanResult) {
        val manufacturerData = result.scanRecord
            ?.getManufacturerSpecificData(BleConstants.MANUFACTURER_ID)
            ?: return

        // Decode userId from raw 16-byte UUID
        val userId = try {
            if (manufacturerData.size >= 16) {
                val bb = java.nio.ByteBuffer.wrap(manufacturerData)
                java.util.UUID(bb.long, bb.long).toString()
            } else {
                String(manufacturerData, Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to decode userId from ${result.device.address}")
            return
        }

        val peer = DiscoveredPeer(
            address   = result.device.address,
            userId    = userId,
            lat       = 0.0,
            lng       = 0.0,
            rssi      = result.rssi,
            timestamp = System.currentTimeMillis()
        )
        _peers.tryEmit(peer)
        kpiTracker.increment(KpiTracker.Key.BLE_PEERS_DISCOVERED)
        Log.d(tag, "Peer discovered: $userId @ ${result.device.address} rssi=${result.rssi}")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopScan() {
        if (!isScanning) return
        bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        isScanning = false
        Log.d(tag, "BLE scan stopped")
    }
}
