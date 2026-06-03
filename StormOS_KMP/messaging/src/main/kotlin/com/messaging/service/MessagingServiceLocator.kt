package com.messaging.service

import android.bluetooth.BluetoothManager
import android.content.Context
import com.messaging.service.db.MessageDatabase
import com.messaging.service.kpi.KpiTracker
import com.messaging.service.kpi.InfluxDbWriter
import com.messaging.service.offline.ble.BleAdvertiser
import com.messaging.service.offline.ble.BleGattCentralClient
import com.messaging.service.offline.ble.BleGattServer
import com.messaging.service.offline.ble.BleScanner
import com.messaging.service.offline.crypto.MessageEncryption
import com.messaging.service.offline.repository.OfflineMessagingRepository
import com.messaging.service.online.api.EmbeddedHttpServer

object MessagingServiceLocator {

    lateinit var kpiTracker:     KpiTracker                  private set
    lateinit var embeddedServer: EmbeddedHttpServer           private set
    lateinit var bleAdvertiser:  BleAdvertiser                private set
    lateinit var bleScanner:     BleScanner                   private set
    lateinit var gattServer:     BleGattServer                private set
    lateinit var offlineRepo:    OfflineMessagingRepository   private set
    lateinit var myUserId:       String                       private set

    private const val PREFS_NAME  = "storm_os_prefs"
    private const val USER_ID_KEY = "user_id"

    private var initialised = false

    fun init(context: Context) {
        if (initialised) return
        initialised = true

        val appContext = context.applicationContext

        myUserId = appContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(USER_ID_KEY, null)
            ?: "unknown"

        val db  = MessageDatabase.getInstance(appContext)
        val dao = db.messageDao()

        val influxWriter = InfluxDbWriter()
        influxWriter.start()
        kpiTracker    = KpiTracker(influxWriter)

        val encryption = MessageEncryption()
        val btManager  = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val btAdapter  = btManager.adapter

        bleScanner    = BleScanner(btAdapter, kpiTracker)
        bleAdvertiser = BleAdvertiser(btAdapter, kpiTracker)
        val gattClient = BleGattCentralClient(appContext, kpiTracker)

        offlineRepo = OfflineMessagingRepository(
            bleScanner, gattClient, encryption, dao, kpiTracker, btAdapter,
            myUserId = myUserId
        )

        gattServer = BleGattServer(appContext, btManager, offlineRepo, kpiTracker)

        // Pass myUserId so the batch endpoint knows whose cache this is
        embeddedServer = EmbeddedHttpServer(
            context     = appContext,
            messageDao  = dao,
            offlineRepo = offlineRepo,
            kpiTracker  = kpiTracker,
            myUserId    = myUserId
        )
    }
}