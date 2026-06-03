package storm.os

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
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

// ── Sync models ───────────────────────────────────────────────────────────────

@Serializable
data class SyncMessage(
    val message_id:   String,
    val sender_id:    String,
    val recipient_id: String,
    val chat_id:      String,
    val content:      String,
    val content_type: String? = "text",
    val latitude:     Double  = 0.0,
    val longitude:    Double  = 0.0,
    val timestamp:    Long,
    val delivered:    Boolean? = true,
    val read:         Boolean? = false
)

@Serializable
data class SyncResponse(
    val timestamp: String                  = "",
    val items:     List<AdItem>            = emptyList(),
    val messages:  List<SyncMessage>       = emptyList()
)

// ─────────────────────────────────────────────────────────────────────────────
// GeoLocation API
// ─────────────────────────────────────────────────────────────────────────────

object StormApi {

    private const val BASE_URL = "http://stormos-103075.duckdns.org"

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun createItem(item: MarketplaceItem) {
        client.post("$BASE_URL/api/v1/items") {
            contentType(ContentType.Application.Json)
            setBody(item)
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

    /**
     * Pulls messages from the cloud DB for the given user.
     * Returns all messages where the user is sender or recipient
     * created after [lastSync].
     *
     * On every fresh app start [lastSync] defaults to epoch so all
     * messages are fetched. Duplicates are silently ignored by
     * the local Room DB (insertOrIgnore).
     */
    suspend fun syncMessages(
        userId: String,
        lastSync: String = "1970-01-01T00:00:00"
    ): List<SyncMessage> {
        return try {
            client.get("$BASE_URL/api/v1/sync") {
                parameter("user_id",   userId)
                parameter("last_sync", lastSync)
            }.body<SyncResponse>().messages
        } catch (e: Exception) {
            println("StormApi.syncMessages error: ${e.message}")
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
// ─────────────────────────────────────────────────────────────────────────────

// Request model for the batch-ingest endpoint on the embedded server
@Serializable
data class IngestMessageRequest(
    @SerialName("message_id")   val messageId:   String,
    @SerialName("sender_id")    val senderId:    String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("chat_id")      val chatId:      String,
    @SerialName("content")      val content:     String,
    @SerialName("content_type") val contentType: String  = "text",
    @SerialName("timestamp")    val timestamp:   Long,
    @SerialName("delivered")    val delivered:   Boolean = true,
    @SerialName("source")       val source:      String  = "online"
)

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

    /**
     * Sends a batch of messages fetched from the cloud sync to the
     * local embedded HTTP server for storage in Room DB.
     * The server uses insertOrIgnore so duplicates are safe.
     */
    suspend fun ingestMessages(messages: List<IngestMessageRequest>): Boolean {
        if (messages.isEmpty()) return true
        return try {
            client.post("$BASE_URL/api/v1/messages/batch") {
                contentType(ContentType.Application.Json)
                setBody(messages)
            }
            true
        } catch (e: Exception) {
            println("MessagingApi.ingestMessages error: ${e.message}")
            false
        }
    }
}