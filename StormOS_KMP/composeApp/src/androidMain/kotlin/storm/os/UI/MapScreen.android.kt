package storm.os.UI

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import storm.os.AdItem

@Composable
actual fun MapView(
    modifier: Modifier,
    items: List<AdItem>,
    onMarkerClick: (String) -> Unit
) {
    // Coordenadas padrão (Aveiro) conforme o teu Python
    val aveiro = LatLng(40.6405, -8.6538)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(aveiro, 11f)
    }

    // No teu actual fun MapView em androidMain
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        onMapLoaded = {
            println("MAP_DEBUG: Mapa carregado com sucesso!")
        }
    ) {
        items.forEach { item ->
            // Garante que este parsing não está a falhar
            val cleanLocation = item.location.replace("POINT(", "").replace(")", "")
            val parts = cleanLocation.split(" ")
            if (parts.size >= 2) {
                val lon = parts[0].toDoubleOrNull() ?: 0.0
                val lat = parts[1].toDoubleOrNull() ?: 0.0
                Marker(
                    state = MarkerState(position = LatLng(lat, lon)),
                    title = item.name
                )
            }
        }
    }
}