package storm.os

import android.annotation.SuppressLint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import android.content.Context

// Precisamos de uma forma de obter o Context.
// O KMP Wizard geralmente cria uma variável global ou injeta via Activity.
lateinit var androidContext: Context

@SuppressLint("MissingPermission")
actual suspend fun getCurrentLocation(): Pair<Double, Double>? {
    val client = LocationServices.getFusedLocationProviderClient(androidContext)
    return try {
        val location = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        location?.let { Pair(it.latitude, it.longitude) }
    } catch (e: Exception) {
        null
    }
}