// Em composeApp/src/androidMain/kotlin/storm/os/LocationProvider.android.kt
package storm.os

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationServices.getFusedLocationProviderClient
import kotlinx.coroutines.tasks.await

@SuppressLint("MissingPermission")
actual suspend fun getCurrentLocation(): Pair<Double, Double>? {
    return try {
        // Usa o contexto que guardámos no AppConfig
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(AppConfig.context)

        val location = fusedLocationClient.lastLocation.await()
        if (location != null) {
            Pair(location.latitude, location.longitude)
        } else null
    } catch (e: Exception) {
        e.printStackTrace() // Ajuda a ver o erro no Logcat se falhar
        null
    }
}