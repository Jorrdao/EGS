package storm.os

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.messaging.service.service.MessagingForegroundService

class MainActivity : ComponentActivity() {

    // ── Runtime permission launcher ───────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            Log.i("MainActivity", "All permissions granted — starting service")
        } else {
            val denied = results.filterValues { !it }.keys
            Log.w("MainActivity", "Permissions denied: $denied — BLE offline mode will be unavailable")
        }
        // Start the service regardless — it runs in degraded mode without BLE
        startMessagingService()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppConfig.context = applicationContext

        requestRequiredPermissions()

        setContent { App() }
    }

    // ── Permission request ────────────────────────────────────────────────────

    private fun requestRequiredPermissions() {
        val needed = buildList {
            // BLE permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ — new granular BLE permissions
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                // Android < 12 — location required for BLE scan
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            // Notification permission (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.filter { permission ->
            // Only request permissions that haven't been granted yet
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isEmpty()) {
            // All already granted — start service directly
            startMessagingService()
        } else {
            // Ask the user — service starts in the launcher callback above
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    // ── Service start ─────────────────────────────────────────────────────────

    private fun startMessagingService() {
        val intent = Intent(this, MessagingForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Log.i("MainActivity", "Messaging Service started")
    }
}

@Preview
@Composable
fun AppAndroidPreview() { App() }