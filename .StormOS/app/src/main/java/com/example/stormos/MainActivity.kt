package com.example.stormos


import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stormos.ui.theme.StormOSTheme
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ItemRequest(
    val name: String,
    val price: Double,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val contact_info: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StormOSTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StormOSTester()
                }
            }
        }
    }
}

@Composable
fun StormOSTester() {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Pronto") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Painel StormOS", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = {
            scope.launch {
                status = "A enviar item..."
                status = callPythonApi()
            }
        }) {
            Text("Criar Item de Teste")
        }
        Text("Log: $status")
    }
}

suspend fun callPythonApi(): String {
    val client = HttpClient(Android) { install(ContentNegotiation) { json() } }
    return try {
        val response = client.post("http://10.0.2.2:8000/api/v1/items") {
            contentType(ContentType.Application.Json)
            // Agora enviamos um objeto tipado e serializável
            setBody(ItemRequest(
                name = "Teste StormOS",
                price = 15.0,
                description = "Enviado com Data Class",
                address = "Rua de Aveiro",
                contact_info = "910000000",
                latitude = 40.6,
                longitude = -8.6
            ))
        }
        "Sucesso: ${response.status}"
    } catch (e: Exception) {
        "Erro: ${e.localizedMessage}"
    } finally {
        client.close()
    }
}