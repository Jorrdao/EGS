import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import platform.MapKit.MKMapView
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
@Composable
fun MapView(modifier: Modifier) {
    UIKitView(
        factory = { MKMapView() },
        modifier = modifier,
        update = { view ->
            // Configurações do mapa da Apple aqui
        }
    )
}