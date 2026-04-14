package storm.os.UI

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import storm.os.AdItem
import storm.os.StormApi


@Composable
fun AdDetailScreen(
    adId: String, // Só precisas do ID que vem da rota
    onContactClick: (String) -> Unit
) {
    // 1. Criar o estado para o anúncio (começa vazio)
    var ad by remember { mutableStateOf<AdItem?>(null) }

    // 2. Procurar o anúncio assim que o ecrã abre
    LaunchedEffect(adId) {
        // Procuramos na lista global o anúncio que tem este ID
        ad = StormApi.getAllAds().find { it.id.toString() == adId }
    }

    // 3. UI condicional: Se ainda não encontrou, mostra a carregar. Se encontrou, mostra os dados.
    if (ad == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // Criamos uma variável segura (Smart Cast) para não usar !!
        val safeAd = ad!!

        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Default.Place, null, modifier = Modifier.size(64.dp))
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(safeAd.name, style = MaterialTheme.typography.headlineMedium)
                Text("€${safeAd.price}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)

                Spacer(modifier = Modifier.height(16.dp))
                Text("Descrição", style = MaterialTheme.typography.titleMedium)
                Text(safeAd.description ?: "Sem descrição.", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))
                Text("Localização", style = MaterialTheme.typography.titleMedium)
                Text(safeAd.address, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))
                Text("Contacto", style = MaterialTheme.typography.titleMedium)
                Text(safeAd.contact_info, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))
                Text("ID do anúncio", style = MaterialTheme.typography.titleMedium)
                Text(safeAd.id.toString(), style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))
                Text("Dados Geográficos", style = MaterialTheme.typography.titleMedium)

                val coordsRaw = safeAd.location
                    .replace("POINT(", "")
                    .replace(")", "")
                    .trim()
                    .split(" ")

                val longitude = coordsRaw.getOrNull(0) ?: "0.0"
                val latitude = coordsRaw.getOrNull(1) ?: "0.0"

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Lat: $latitude", // Já não dá erro porque usamos a variável local
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Long: $longitude",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                }


                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { onContactClick(safeAd.name) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Chat, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contactar Vendedor")
                }
            }
        }
    }
