package com.messaging.service.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.messaging.service.MessagingServiceLocator
import com.messaging.service.R
import com.messaging.service.online.api.EmbeddedHttpServer
import kotlinx.coroutines.launch

private const val TAG             = "MessagingService"
private const val NOTIFICATION_ID = 1001

class MessagingForegroundService : LifecycleService() {

   @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
override fun onCreate() {
    super.onCreate()
    MessagingServiceLocator.init(this)
    createNotificationChannel()

    if (hasBlePermissions()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
    } else {
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    MessagingServiceLocator.embeddedServer.start(lifecycleScope)
    startBle()
    observeIncomingMessages()
    startHealthReporting()

    Log.i(TAG, "Started — HTTP on :${EmbeddedHttpServer.DEFAULT_PORT}")
}
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun onDestroy() {
        MessagingServiceLocator.embeddedServer.stop()
        MessagingServiceLocator.bleAdvertiser.stop()
        MessagingServiceLocator.bleScanner.stop()
        MessagingServiceLocator.gattServer.stop()
        super.onDestroy()
        Log.i(TAG, "Destroyed")
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE])
    private fun startBle() {
        if (!hasBlePermissions()) {
            Log.w(TAG, "BLE permissions missing — offline mode disabled")
            return
        }
        val btAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (!btAdapter.isEnabled) {
            Log.w(TAG, "Bluetooth not enabled — offline mode disabled")
            return
        }
        MessagingServiceLocator.gattServer.start()
        MessagingServiceLocator.bleScanner.start(lifecycleScope)
        MessagingServiceLocator.bleAdvertiser.start("self", 0.0, 0.0, lifecycleScope)
        MessagingServiceLocator.offlineRepo.startObservingPeers(lifecycleScope)
        Log.i(TAG, "BLE stack started")
    }

    private fun observeIncomingMessages() {
        lifecycleScope.launch {
            MessagingServiceLocator.offlineRepo.incomingMessages.collect { meshMsg ->
                Log.d(TAG, "BLE message from ${meshMsg.senderId}")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.notification_channel_id),
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()

    /**
     * Pushes a health snapshot to InfluxDB every 60 seconds.
     * Also updates location context when available.
     */
    private fun startHealthReporting() {
        lifecycleScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000L)
                MessagingServiceLocator.kpiTracker.pushHealthSnapshot()
            }
        }
    }

    /**
     * Call this from the UI or a LocationProvider when the device location changes.
     * Attaches city/country to all subsequent InfluxDB metric points.
     */
    fun updateLocation(city: String, country: String) {
        MessagingServiceLocator.kpiTracker.updateLocation(city, country)
        MessagingServiceLocator.bleAdvertiser.updatePosition(0.0, 0.0) // lat/lng set separately
    }

    private fun hasBlePermissions(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ).all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
        } else {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
}