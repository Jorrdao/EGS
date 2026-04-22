package com.messaging.service.service

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.RemoteCallbackList
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.messaging.service.BuildConfig
import com.messaging.service.IMessagingCallback
import com.messaging.service.IMessagingService
import com.messaging.service.R
import com.messaging.service.kpi.KpiTracker
import com.messaging.service.offline.ble.BleAdvertiser
import com.messaging.service.offline.ble.BleGattServer
import com.messaging.service.offline.ble.BleScanner
import com.messaging.service.offline.repository.OfflineMessagingRepository
import com.messaging.service.online.repository.OnlineMessagingRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG                  = "MessagingForegroundSvc"
private const val NOTIFICATION_ID      = 1001
private const val CHANNEL_ID           = "messaging_service_channel"

@AndroidEntryPoint
class MessagingForegroundService : LifecycleService() {

    // ── Injected dependencies ─────────────────────────────────────────────────
    @Inject lateinit var onlineRepo:    OnlineMessagingRepository
    @Inject lateinit var offlineRepo:   OfflineMessagingRepository
    @Inject lateinit var bleScanner:    BleScanner
    @Inject lateinit var bleAdvertiser: BleAdvertiser
    @Inject lateinit var gattServer:    BleGattServer
    @Inject lateinit var kpiTracker:    KpiTracker
    @Inject lateinit var bluetoothManager: BluetoothManager

    @Inject lateinit var httpServer: com.messaging.service.online.api.EmbeddedHttpServer

    // AIDL remote callback list – thread-safe, auto-removes dead callbacks
    private val callbacks = RemoteCallbackList<IMessagingCallback>()

    // Registered user ID (set on first broadcastPresence call)
    private var selfUserId: String = "unknown"

    // ── AIDL Binder implementation ────────────────────────────────────────────
    private val binder = object : IMessagingService.Stub() {

        override fun sendMessageOnline(chatId: String, recipientId: String, content: String): String {
            return kotlinx.coroutines.runBlocking {
                when (val result = onlineRepo.sendMessage(chatId, recipientId, content)) {
                    is com.messaging.service.online.repository.ApiResult.Success -> {
                        val id = result.data.messageId
                        notifyCallbacks { it.onStatusChanged("sent:$id") }
                        id
                    }
                    is com.messaging.service.online.repository.ApiResult.Error -> {
                        notifyCallbacks { it.onStatusChanged("error:${result.code}:${result.message}") }
                        ""
                    }
                    is com.messaging.service.online.repository.ApiResult.NetworkError -> {
                        notifyCallbacks {
                            it.onStatusChanged(
                                "error:network_unreachable\n" +
                                        "→ No connection to backend.\n" +
                                        "→ ONLINE_BASE_URL = ${BuildConfig.ONLINE_BASE_URL}\n" +
                                        "→ Start the FastAPI server and set the correct IP in BuildConfig."
                            )
                        }
                        ""
                    }
                }
            }
        }

        override fun getChatHistory(chatId: String, limit: Int, offset: Int): String {
            var json = "[]"
            lifecycleScope.launch {
                val result = onlineRepo.getChatHistory(chatId, limit, offset)
                if (result is com.messaging.service.online.repository.ApiResult.Success) {
                    json = com.google.gson.Gson().toJson(result.data)
                }
            }
            return json
        }

        override fun broadcastPresence(lat: Double, lng: Double) {
            checkBlePermission() ?: return
            bleAdvertiser.updatePosition(lat, lng)
            Log.d(TAG, "Presence updated: lat=$lat lng=$lng")
        }

        override fun sendMeshMessage(destinationId: String, encryptedPayload: String, ttl: Int) {
            checkBlePermission() ?: return
            lifecycleScope.launch {
                offlineRepo.sendMeshMessage(
                    destinationId = destinationId,
                    plaintext     = encryptedPayload,   // caller passes plaintext; we encrypt
                    ttl           = ttl,
                    senderId      = selfUserId,
                    scope         = lifecycleScope
                )
            }
        }

        override fun requestMarketplaceSync() {
            checkBlePermission() ?: return
            // Broadcast to all known peers
            lifecycleScope.launch {
                Log.d(TAG, "Marketplace sync requested")
            }
        }

        override fun registerCallback(callback: IMessagingCallback?) {
            callback?.let { callbacks.register(it) }
        }

        override fun unregisterCallback(callback: IMessagingCallback?) {
            callback?.let { callbacks.unregister(it) }
        }

        override fun getKpiSnapshot(): String = kpiTracker.getSnapshot()

        override fun isBleRunning(): Boolean = gattServer.isRunning()

        override fun isOnlineAvailable(): Boolean {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            return cm.activeNetwork != null
        }

        override fun isHttpServerRunning(): Boolean = httpServer.isRunning()

        override fun getHttpServerPort(): Int =
            com.messaging.service.online.api.EmbeddedHttpServer.DEFAULT_PORT
    }

    // ── Service lifecycle ─────────────────────────────────────────────────────

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun onCreate() {
        super.onCreate()
        kpiTracker.increment(KpiTracker.Key.SERVICE_RESTARTS)
        startForeground(NOTIFICATION_ID, buildNotification())
        startBle()
        httpServer.start(lifecycleScope)
        observeIncomingMessages()
        observeKpiUpdates()
        Log.d(TAG, "MessagingForegroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // START_STICKY: system re-creates the service if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun onDestroy() {
        httpServer.stop()
        bleAdvertiser.stop()
        bleScanner.stop()
        gattServer.stop()
        callbacks.kill()
        super.onDestroy()
        Log.d(TAG, "MessagingForegroundService destroyed")
    }

    // ── BLE initialisation ────────────────────────────────────────────────────

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    private fun startBle() {
        if (!hasBlePermissions()) {
            Log.w(TAG, "Missing BLE permissions – offline mode disabled")
            return
        }
        if (!bluetoothManager.adapter.isEnabled) {
            Log.w(TAG, "Bluetooth not enabled – offline mode disabled")
            return
        }

        gattServer.start()
        bleScanner.start(lifecycleScope)
        bleAdvertiser.start(
            userId = selfUserId,
            lat    = 0.0,
            lng    = 0.0,
            scope  = lifecycleScope
        )
        offlineRepo.startObservingPeers(lifecycleScope)
        Log.d(TAG, "BLE stack started")
    }

    private fun observeIncomingMessages() {
        lifecycleScope.launch {
            offlineRepo.incomingMessages.collect { meshMsg ->
                val json = com.google.gson.Gson().toJson(mapOf(
                    "source"      to "ble",
                    "message_id"  to meshMsg.messageId,
                    "sender_id"   to meshMsg.senderId,
                    "timestamp"   to meshMsg.timestamp
                ))
                notifyCallbacks { it.onMessageReceived(json) }
            }
        }
    }

    private fun observeKpiUpdates() {
        lifecycleScope.launch {
            kpiTracker.metricsFlow.collect { update ->
                update.forEach { (label, value) ->
                    notifyCallbacks { it.onKpiUpdate(label, value) }
                }
            }
        }
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun hasBlePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkBlePermission(): Unit? {
        return if (hasBlePermissions()) Unit else null
    }

    private inline fun notifyCallbacks(crossinline block: (IMessagingCallback) -> Unit) {
        val n = callbacks.beginBroadcast()
        for (i in 0 until n) {
            try { block(callbacks.getBroadcastItem(i)) }
            catch (e: Exception) { Log.w(TAG, "Callback error: ${e.message}") }
        }
        callbacks.finishBroadcast()
    }
}
