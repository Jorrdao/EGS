package storm.os.UI

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import storm.os.MarketplaceItem
import storm.os.StormApi
import storm.os.getCurrentLocation

@Composable
fun CreateItemScreen() {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Estados dos campos
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Preencha os campos") }
    var contact by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Novo Anúncio StormOS", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome do Item") },
                modifier = Modifier.fillMaxWidth(),
                isError = name.isBlank()
            )

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Preço (€)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = price.toDoubleOrNull() == null
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Endereço Completo") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Contacto") },
                modifier = Modifier.fillMaxWidth(),
                isError = contact.isBlank()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && price.toDoubleOrNull() != null,
                onClick = {
                    scope.launch {
                        try {
                            statusText = "A enviar para o servidor..."
                            val item = MarketplaceItem(
                                name = name,
                                description = description,
                                price = price.toDoubleOrNull() ?: 0.0,
                                address = address,
                                contact_info = "910000000", // Placeholder
                                latitude = 0.0, longitude = 0.0
                            )
                            StormApi.createItem(item)
                            statusText = "Sucesso: Item criado!"
                        } catch (e: Exception) {
                            statusText = "Erro de Rede: Verifique o servidor Python"
                        }
                    }
                }
            ) {
                Text("Publicar no Marketplace")
            }

            Text(text = statusText, color = if (statusText.contains("Erro")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
    }
}