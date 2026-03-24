package storm.os.UI

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import storm.os.AdItem
import storm.os.StormApi

@Composable
fun ListScreen(onAdClick: (String) -> Unit) {
    var radius by remember { mutableFloatStateOf(10f) }
    var ads by remember { mutableStateOf(emptyList<AdItem>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Dispara a busca sempre que o raio muda
    LaunchedEffect(radius) {
        isLoading = true

        try {
            val result = StormApi.fetchAds(radius)
            println("STORM_DEBUG: Recebi ${result.size} itens")
            ads = result
        } catch (e: Exception) {
            println("STORM_DEBUG: Erro -> ${e.message}")
            ads = emptyList()
        }
        isLoading = false
    }

    Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Text(
            text = "Anúncios",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleLarge
        )
        Slider(value = radius, onValueChange = { radius = it }, valueRange = 0f..100f)
        val radiusText = if (radius.toInt() == 0) "Sem restrição de distância" else "Raio: ${radius.toInt()} km"

        Text(text = radiusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)




        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            LazyColumn {
                // Dentro da LazyColumn na ListScreen
                items(ads) { ad ->
                    ListItem(
                        // 'ad.name' em vez de 'ad.title'
                        headlineContent = { Text(ad.name) },
                        // 'ad.address' ou 'ad.description' em vez de 'ad.location'
                        supportingContent = { Text(ad.address) },
                        trailingContent = {
                            Text("€${ad.price}", color = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.clickable { onAdClick(ad.id.toString()) }
                    )
                }
            }
        }
    }
}