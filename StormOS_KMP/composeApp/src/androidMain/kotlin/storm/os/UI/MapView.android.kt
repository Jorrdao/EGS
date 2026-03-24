package storm.os.UI

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.GoogleMap

@Composable
actual fun MapView(modifier: Modifier) {
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                onCreate(Bundle()) // Agora o Bundle() é reconhecido
                getMapAsync { googleMap: GoogleMap ->
                    googleMap.uiSettings.isZoomControlsEnabled = true
                }
            }
        },
        modifier = modifier
    )
}