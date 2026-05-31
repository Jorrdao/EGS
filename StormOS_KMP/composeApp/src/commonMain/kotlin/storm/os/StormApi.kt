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

    private const val BASE_URL = "http://stormos-103075.duckdns.org"

    private val client = HttpClient {
        install(ContentNegotiation) { json() }
    }

    suspend fun createItem(item: MarketplaceItem) {
        client.post("$BASE_URL/api/v1/items") {
            contentType(ContentType.Application.Json)
            setBody(item) // objeto simples, não lista
        }
    }

    suspend fun getAllAds(): List<AdItem> {
        return try {
            client.get("$BASE_URL/api/v1/items").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchAds(radius: Float): List<AdItem> {
        return try {
            client.get("$BASE_URL/api/v1/items") {
                parameter("radius", radius)
            }.body()
        } catch (e: Exception) {
            println("Erro na API: ${e.message}")
            emptyList()
        }
    }

    suspend fun getSyncData(lastSync: String): List<AdItem> {
        return try {
            client.get("$BASE_URL/api/v1/sync") {
                parameter("last_sync", lastSync)
            }.body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMapData(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<AdItem> {
        return try {
            client.get("$BASE_URL/api/v1/map/data") {
                parameter("min_lat", minLat)
                parameter("max_lat", maxLat)
                parameter("min_lon", minLon)
                parameter("max_lon", maxLon)
            }.body()
        } catch (e: Exception) {
            emptyList()
        }
    }
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

    suspend fun getHistory(chatId: String): List<ChatMessage> {
        return try {
            client.get("$BASE_URL/api/v1/messages/$chatId")
                .body<ChatHistoryResponse>()
                .messages
                .sortedBy { it.timestamp }
        } catch (e: Exception) {
            println("MessagingApi.getHistory error: ${e.message}")
            emptyList()
        }
    }
}