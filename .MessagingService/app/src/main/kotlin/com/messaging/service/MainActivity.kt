package com.messaging.service

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.messaging.service.IMessagingCallback
import com.messaging.service.IMessagingService
import com.messaging.service.databinding.ActivityMainBinding
import com.messaging.service.service.MessagingForegroundService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var messagingService: IMessagingService? = null
    private var bound = false

    // ── Permission launcher ───────────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) startMessagingService()
        else Toast.makeText(this, "Some permissions denied — BLE offline mode may be unavailable", Toast.LENGTH_LONG).show()
    }

    // ── Service connection ────────────────────────────────────────────────────
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            messagingService = IMessagingService.Stub.asInterface(binder)
            bound = true
            messagingService?.registerCallback(remoteCallback)
            val port = messagingService?.httpServerPort ?: 8080
            updateStatus("Service connected ✓  |  HTTP server: http://127.0.0.1:$port/api/v1/")
            appendLog("🌐 HTTP API ready at http://127.0.0.1:$port/api/v1/")
            appendLog("   POST /messages/send")
            appendLog("   GET  /messages/{chat_id}")
            appendLog("   GET  /health  |  GET /kpi")
            startKpiPolling()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            messagingService = null
            bound = false
            updateStatus("Service disconnected")
        }
    }

    // ── AIDL callback ─────────────────────────────────────────────────────────
    private val remoteCallback = object : IMessagingCallback.Stub() {
        override fun onMessageReceived(messageJson: String) {
            runOnUiThread { appendLog("📨 Message: $messageJson") }
        }
        override fun onPeerDiscovered(userId: String, lat: Double, lng: Double) {
            runOnUiThread { appendLog("📡 Peer: $userId @ ($lat, $lng)") }
        }
        override fun onKpiUpdate(metricName: String, value: Long) {
            // Handled by the polling loop instead – avoids UI spam
        }
        override fun onStatusChanged(status: String) {
            runOnUiThread { appendLog("ℹ️ $status") }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartService.setOnClickListener { requestPermissionsAndStart() }
        binding.btnStopService.setOnClickListener  { stopMessagingService() }
        binding.btnSendTest.setOnClickListener     { sendTestMessage() }

        updateStatus("Service not started")
    }

    override fun onStart() {
        super.onStart()
        // Try to bind if already running
        bindToService()
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            messagingService?.unregisterCallback(remoteCallback)
            unbindService(connection)
            bound = false
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun requestPermissionsAndStart() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isEmpty()) startMessagingService()
        else permissionLauncher.launch(needed.toTypedArray())
    }

    private fun startMessagingService() {
        val intent = Intent(this, MessagingForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindToService()
    }

    private fun stopMessagingService() {
        if (bound) {
            messagingService?.unregisterCallback(remoteCallback)
            unbindService(connection)
            bound = false
        }
        stopService(Intent(this, MessagingForegroundService::class.java))
        updateStatus("Service stopped")
        binding.tvKpi.text = ""
    }

    private fun bindToService() {
        val intent = Intent(this, MessagingForegroundService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun sendTestMessage() {
        val svc = messagingService ?: run {
            Toast.makeText(this, "Service not connected", Toast.LENGTH_SHORT).show()
            return
        }
        val id = svc.sendMessageOnline("chat_test", "user_demo", "Hello from MainActivity!")
        appendLog("📤 Sent test message, id=$id")
    }

    private fun startKpiPolling() {
        lifecycleScope.launch {
            while (isActive && bound) {
                val snapshot = messagingService?.kpiSnapshot ?: break
                runOnUiThread { binding.tvKpi.text = formatKpi(snapshot) }
                delay(3_000L)
            }
        }
    }

    private fun updateStatus(msg: String) {
        binding.tvStatus.text = msg
    }

    private fun appendLog(line: String) {
        val current = binding.tvLog.text.toString()
        val lines   = current.lines().takeLast(19)   // keep last 20 lines
        binding.tvLog.text = (lines + line).joinToString("\n")
    }

    private fun formatKpi(json: String): String {
        // Simple pretty-print: one "key: value" per line
        return json
            .removePrefix("{").removeSuffix("}")
            .split(",")
            .joinToString("\n") { pair ->
                val (k, v) = pair.split(":", limit = 2)
                "${k.trim().removeSurrounding("\"")}: ${v.trim().removeSurrounding("\"")}"
            }
    }
}
