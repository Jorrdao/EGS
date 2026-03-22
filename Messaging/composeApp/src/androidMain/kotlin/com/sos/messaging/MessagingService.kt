package com.sos.messaging

import android.app.*
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sos.messaging.offline.*
import com.sos.messaging.online.MessagingRestClient
import kotlinx.coroutines.*

class MessagingService : Service() {

    private val tag = "MessagingService"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var gattServer: BleGattServer
    private lateinit var gattClient: BleGattClient
    private lateinit var meshRouter: MeshMessageRouter
    private lateinit var presenceBroadcaster: PresenceBroadcaster
    private lateinit var repository: MessagingRepository

    companion object {
        const val CHANNEL_ID = "sos_messaging_channel"
        const val NOTIFICATION_ID = 1
        const val BASE_URL = "https://your-api-host.com"  // ← replace
        const val LOCAL_USER_ID = "local_user_id"         // ← inject from session
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        gattServer  = BleGattServer(this)
        gattClient  = BleGattClient(this)
        meshRouter  = MeshMessageRouter(LOCAL_USER_ID, gattServer, gattClient)
        presenceBroadcaster = PresenceBroadcaster(this, LOCAL_USER_ID)

        val restClient = MessagingRestClient(BASE_URL)

        repository = MessagingRepository(
            restClient   = restClient,
            gattServer   = gattServer,
            gattClient   = gattClient,
            meshRouter   = meshRouter,
            isOnline     = ::isNetworkAvailable
        )

        gattServer.start()
        gattClient.startScan()

        // Start mesh routing — delivered messages logged here; surface via your own event bus
        meshRouter.start(scope) { delivered ->
            Log.d(tag, "Received mesh message from ${delivered.senderId}")
        }

        // BLE_BROADCAST /p2p/presence loop
        presenceBroadcaster.start(scope) {
            Pair(0.0, 0.0) // replace with real location from GeoLocation module
        }

        Log.d(tag, "MessagingService started")
    }

    override fun onDestroy() {
        scope.cancel()
        gattServer.stop()
        gattClient.stopScan()
        presenceBroadcaster.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "SOS Messaging", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SOS Messaging")
            .setContentText("Messaging service running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
}