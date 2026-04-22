package storm.os.UI

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation
import platform.CoreLocation.CLLocationCoordinate2DMake
import storm.os.AdItem

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapView(
    modifier: Modifier,
    items: List<AdItem>,
    onMarkerClick: (String) -> Unit
) {
    // O UIKitView permite usar componentes nativos do iOS (Swift/Obj-C) no Compose
    UIKitView(
        factory = {
            val mapView = MKMapView()

            // Adiciona os pins no mapa do iOS
            items.forEach { item ->
                // Parsing das coordenadas "POINT(lon lat)"
                val coords = item.location
                    .replace("POINT(", "")
                    .replace(")", "")
                    .split(" ")

                if (coords.size >= 2) {
                    val lon = coords[0].toDoubleOrNull() ?: 0.0
                    val lat = coords[1].toDoubleOrNull() ?: 0.0

                    val annotation = MKPointAnnotation()
                    annotation.setCoordinate(CLLocationCoordinate2DMake(lat, lon))
                    annotation.setTitle(item.name)
                    annotation.setSubtitle("€${item.price}")
                    mapView.addAnnotation(annotation)
                }
            }
            mapView
        },
        modifier = modifier
    )
}