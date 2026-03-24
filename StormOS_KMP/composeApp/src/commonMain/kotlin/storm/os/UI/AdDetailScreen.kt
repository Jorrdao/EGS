package storm.os.UI

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdDetailScreen(adName: String, onContactClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Placeholder para Imagem
        Surface(
            modifier = Modifier.fillMaxWidth().height(250.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(Icons.Default.Place, null, modifier = Modifier.size(64.dp))
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(adName, style = MaterialTheme.typography.headlineMedium)
            Text("€150.00", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Descrição", style = MaterialTheme.typography.titleMedium)
            Text("Este é um item de teste para o StormOS. Localizado perto de Aveiro.",
                style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onContactClick("Vendedor do $adName") },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.Chat, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Contactar Vendedor")
            }
        }
    }
}