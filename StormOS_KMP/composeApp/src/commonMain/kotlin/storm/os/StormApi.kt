package storm.os

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

@Serializable
data class AdItem(
    val id: Int,
    val name: String,
    val price: Double,
    val contact_info: String,
    val description: String,
    val address: String,
    val location: String
)

object StormApi {
    private val client = HttpClient {
        install(ContentNegotiation) { json() }
    }

    suspend fun createItem(item: MarketplaceItem) {
        // 10.0.2.2 é o IP para o emulador Android ver o teu PC (onde está o main.py)
        client.post("http://10.0.2.2:8000/api/v1/sync/items") {
            contentType(ContentType.Application.Json)
            setBody(listOf(item)) // O teu backend espera uma lista
        }
    }

    suspend fun getAllAds(): List<AdItem> {
        return try {
            client.get("http://10.0.2.2:8000/api/v1/items").body()
        } catch (e: Exception) {
            emptyList() // Retorna lista vazia se o servidor estiver desligado
        }
    }

    suspend fun fetchAds(radius: Float): List<AdItem> {
        return try {
            // Relembra: 10.0.2.2 para emulador Android
            client.get("http://10.0.2.2:8000/api/v1/items") {
                parameter("radius", radius) // Passa o raio como query param
            }.body()
        } catch (e: Exception) {
            println("Erro na API: ${e.message}")
            emptyList()
        }
    }
}