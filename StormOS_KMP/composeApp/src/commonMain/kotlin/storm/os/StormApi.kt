package storm.os

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

object StormApi {
    val client = HttpClient {
        install(ContentNegotiation) { json() }
    }

    suspend fun createItem(item: MarketplaceItem) {
        // 10.0.2.2 é o IP para o emulador Android ver o teu PC (onde está o main.py)
        client.post("http://10.0.2.2:8000/api/v1/sync/items") {
            contentType(ContentType.Application.Json)
            setBody(listOf(item)) // O teu backend espera uma lista
        }
    }
}