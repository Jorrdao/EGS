package storm.os.UI

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Place

@Composable
expect fun MapView(modifier: Modifier)

@Composable
fun MapScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Chamada ao componente nativo
        MapView(modifier = Modifier.fillMaxSize())

        // UI Sobreposta (Overlay)
        Card(
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
        ) {
            Text("Explorar Itens Próximos", modifier = Modifier.padding(12.dp))
        }

        FloatingActionButton(
            onClick = { /* Centrar na minha posição */ },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(androidx.compose.material.icons.Icons.Default.Place, "Minha Localização")
        }
    }
}