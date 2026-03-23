package storm.os.UI

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ListScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var radius by remember { mutableFloatStateOf(10f) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Explorar Marketplace", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        // Barra de Pesquisa
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("O que procuras?") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Filtro de Distância (Raio)
        Text("Distância máxima: ${radius.toInt()} km", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = radius,
            onValueChange = { radius = it },
            valueRange = 1f..100f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()

        // Lista de Resultados
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Simulação de 10 itens
            items(10) { index ->
                ListItem(
                    headlineContent = { Text("Artigo Marketplace #$index") },
                    supportingContent = { Text("Vendedor a ${index + 1} km de distância") },
                    trailingContent = { Text("€${(index + 5) * 10}") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
            }
        }
    }
}