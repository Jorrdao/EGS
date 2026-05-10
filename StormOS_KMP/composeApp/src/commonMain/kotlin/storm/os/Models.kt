package storm.os

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Marketplace ───────────────────────────────────────────────────────────────

@Serializable
data class MarketplaceItem(
    val name: String,
    val description: String,
    val price: Double,
    val address: String,
    val contact_info: String,
    val latitude: Double,
    val longitude: Double
)

// ── Messaging ─────────────────────────────────────────────────────────────────

/**
 * A single message returned by the Messaging Service.
 * Matches the JSON shape of GET /api/v1/messages/{chat_id}.
 */
@Serializable
data class ChatMessage(
    @SerialName("message_id")   val messageId: String,
    @SerialName("sender_id")    val senderId: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("content")      val content: String,
    @SerialName("content_type") val contentType: String = "text",
    @SerialName("timestamp")    val timestamp: Long,
    @SerialName("delivered")    val delivered: Boolean = false,
    @SerialName("read")         val read: Boolean = false
)

@Serializable
data class ChatHistoryResponse(
    @SerialName("chat_id")  val chatId: String,
    @SerialName("messages") val messages: List<ChatMessage>,
    @SerialName("total")    val total: Int,
    @SerialName("has_more") val hasMore: Boolean
)

/**
 * One entry in the conversation list.
 * Returned by GET /api/v1/chats?user_id=xxx on the Messaging Service.
 */
@Serializable
data class ChatSummary(
    @SerialName("chat_id")       val chatId: String,
    @SerialName("other_user_id") val otherUserId: String,
    @SerialName("last_message")  val lastMessage: String,
    @SerialName("timestamp")     val timestamp: Long,
    @SerialName("unread")        val unread: Boolean = false
)

@Serializable
data class ChatsResponse(
    @SerialName("user_id") val userId: String,
    @SerialName("chats")   val chats: List<ChatSummary>,
    @SerialName("total")   val total: Int
)

@Serializable
data class SendMessageRequest(
    @SerialName("user_id")      val userId: String,
    @SerialName("chat_id")      val chatId: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("content")      val content: String,
    @SerialName("content_type") val contentType: String = "text"
)