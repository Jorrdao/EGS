package com.messaging.service.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG             = "MessagingService"
private const val NOTIFICATION_ID = 1001

class MessagingForegroundService : LifecycleService() {

    private lateinit var locationManager: LocationManager

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

        // ── NEW: Hook into the device GPS to feed Grafana ──
        startLocationTracking()

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

        // Stop requesting location updates when service dies
        if (::locationManager.isInitialized) {
            try {
                locationManager.removeUpdates(locationListener)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove location listener: ${e.message}")
            }
        }

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

    private fun startHealthReporting() {
        lifecycleScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000L)
                MessagingServiceLocator.kpiTracker.pushHealthSnapshot()
            }
        }
    }

    // ── NEW: Location Fetching & Reverse Geocoding ─────────────────────────────

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Geocoding requires network I/O, so we run it on the IO Dispatcher
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(this@MessagingForegroundService, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                    val city = addresses?.firstOrNull()?.locality ?: "unknown"
                    val country = addresses?.firstOrNull()?.countryCode ?: "unknown"

                    Log.d(TAG, "Location updated: $city, $country (${location.latitude}, ${location.longitude})")

                    // 1. Send City and Country to InfluxDB for Grafana Heatmaps
                    MessagingServiceLocator.kpiTracker.updateLocation(city, country)

                    // 2. Send Latitude/Longitude to BLE Mesh Advertiser
                    MessagingServiceLocator.bleAdvertiser.updatePosition(location.latitude, location.longitude)

                } catch (e: Exception) {
                    Log.w(TAG, "Geocoder failed: ${e.message}. Feeding raw coords only.")
                    // Fallback if Geocoder fails (e.g., phone is offline)
                    MessagingServiceLocator.kpiTracker.updateLocation("unknown", "unknown")
                    MessagingServiceLocator.bleAdvertiser.updatePosition(location.latitude, location.longitude)
                }
            }
        }
    }

    private fun startLocationTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission missing — Grafana geo-tagging disabled")
            return
        }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        try {
            // Request updates every 2 minutes (120,000 ms) or if the user moves 50 meters
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                120_000L,
                50f,
                locationListener
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                120_000L,
                50f,
                locationListener
            )
            Log.i(TAG, "Location tracking started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location tracking: ${e.message}")
        }
    }

    private fun hasBlePermissions(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.ACCESS_FINE_LOCATION // Added Location permission check
            ).all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
        } else {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
}