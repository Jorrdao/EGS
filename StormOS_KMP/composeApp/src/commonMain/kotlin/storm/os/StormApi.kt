package storm.os

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

    // No StormApi.kt
    suspend fun getSyncData(lastSync: String): List<AdItem> {
        return try {
            client.get("http://10.0.2.2:8000/api/v1/sync") {
                parameter("last_sync", lastSync)
            }.body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // No StormApi.kt
    suspend fun getMapData(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<AdItem> {
        return try {
            client.get("http://10.0.2.2:8000/api/v1/map/data") {
                parameter("min_lat", minLat)
                parameter("max_lat", maxLat)
                parameter("min_lon", minLon)
                parameter("max_lon", maxLon)
            }.body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // No StormApi.kt
    /*suspend fun syncLocations(locations: List<LocationUpdate>) {
        client.post("http://10.0.2.2:8000/api/v1/sync/locations") {
            contentType(ContentType.Application.Json)
            setBody(locations)
        }
    }*/
}

// ─────────────────────────────────────────────────────────────────────────────
// Messaging API  (port 8080 — Messaging Service embedded HTTP server)
//
// Calls go to 127.0.0.1:8080 — the Messaging Service running locally on
// the same Android device. On iOS this will fail gracefully (no service).
// ─────────────────────────────────────────────────────────────────────────────

object MessagingApi {

    private const val BASE_URL = "http://127.0.0.1:8080"

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /**
     * Sends a message to the Messaging Service.
     * Stored locally as PENDING — works offline.
     * Returns true on success, false if the service is unreachable.
     */
    suspend fun sendMessage(
        userId: String,
        chatId: String,
        recipientId: String,
        content: String
    ): Boolean {
        return try {
            client.post("$BASE_URL/api/v1/messages/send") {
                contentType(ContentType.Application.Json)
                setBody(SendMessageRequest(
                    userId      = userId,
                    chatId      = chatId,
                    recipientId = recipientId,
                    content     = content
                ))
            }
            true
        } catch (e: Exception) {
            println("MessagingApi.sendMessage error: ${e.message}")
            false
        }
    }

    /**
     * Returns the list of conversations the user has participated in,
     * each with the last message and the other participant's ID.
     * Used to build the dynamic conversation list screen.
     */
    suspend fun getChats(userId: String): List<ChatSummary> {
        return try {
            client.get("$BASE_URL/api/v1/chats") {
                parameter("user_id", userId)
            }.body<ChatsResponse>().chats
        } catch (e: Exception) {
            println("MessagingApi.getChats error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Returns the message history for a chat from the local DB.
     * Works fully offline — the Messaging Service serves from Room.
     */
    suspend fun getHistory(chatId: String): List<ChatMessage> {
        return try {
            client.get("$BASE_URL/api/v1/messages/$chatId")
                .body<ChatHistoryResponse>()
                .messages
                .sortedBy { it.timestamp }  // oldest first for chat display
        } catch (e: Exception) {
            println("MessagingApi.getHistory error: ${e.message}")
            emptyList()
        }
    }
}