package storm.os

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLLocationAccuracyBest

@OptIn(ExperimentalForeignApi::class)
actual suspend fun getCurrentLocation(): Pair<Double, Double>? {
    val manager = CLLocationManager()

    // Importante: No iOS, precisas de pedir autorização, senão 'location' será sempre null
    manager.requestWhenInUseAuthorization()

    val location = manager.location ?: return null

    // No Kotlin/Native, usamos useContents para ler structs do C
    return location.coordinate.useContents {
        Pair(latitude, longitude)
    }
}