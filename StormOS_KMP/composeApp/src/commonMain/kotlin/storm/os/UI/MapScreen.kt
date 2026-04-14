package storm.os.UI

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Place
import storm.os.AdItem
import storm.os.StormApi



@Composable
expect fun MapView(
    modifier: Modifier,
    items: List<AdItem>,
    onMarkerClick: (String) -> Unit
)

@Composable
fun MapScreen(onAdClick: (String) -> Unit) {
    // Estado para os itens que vêm do Python
    var mapItems by remember { mutableStateOf(emptyList<AdItem>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Carrega todos os anúncios ao iniciar o ecrã
    LaunchedEffect(Unit) {
        isLoading = true
        mapItems = StormApi.getAllAds()
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Chamada ao componente que será implementado no androidMain
        MapView(
            modifier = Modifier.fillMaxSize(),
            items = mapItems,
            onMarkerClick = { adName -> onAdClick(adName) }
        )

        // Overlay: Barra de estado superior
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Explorar ${mapItems.size} itens próximos")
            }
        }

        // Botão para centrar (Lógica de GPS a implementar)
        FloatingActionButton(
            onClick = { /* Lógica para mover a câmara para a posição do utilizador */ },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Place, "Minha Localização")
        }
    }
}