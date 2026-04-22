package com.messaging.service.online.models

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────────────────────────────────────
// Request bodies
// ─────────────────────────────────────────────────────────────────────────────

data class SendMessageRequest(
    @SerializedName("chat_id")           val chatId: String,
    @SerializedName("recipient_id")      val recipientId: String,
    @SerializedName("content")           val content: String,
    @SerializedName("content_type")      val contentType: ContentType = ContentType.TEXT,
    @SerializedName("client_message_id") val clientMessageId: String? = null
)

enum class ContentType {
    @SerializedName("text")      TEXT,
    @SerializedName("image_url") IMAGE_URL,
    @SerializedName("location")  LOCATION
}

// ─────────────────────────────────────────────────────────────────────────────
// Response bodies
// ─────────────────────────────────────────────────────────────────────────────

data class SendMessageResponse(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("chat_id")    val chatId: String,
    @SerializedName("status")     val status: DeliveryStatus,
    @SerializedName("timestamp")  val timestamp: Long
)

enum class DeliveryStatus {
    @SerializedName("delivered") DELIVERED,
    @SerializedName("queued")    QUEUED,
    @SerializedName("failed")    FAILED
}

data class ChatHistoryResponse(
    @SerializedName("chat_id")  val chatId: String,
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("total")    val total: Int,
    @SerializedName("has_more") val hasMore: Boolean
)

data class Message(
    @SerializedName("message_id")   val messageId: String,
    @SerializedName("sender_id")    val senderId: String,
    @SerializedName("recipient_id") val recipientId: String,
    @SerializedName("content")      val content: String,
    @SerializedName("content_type") val contentType: ContentType,
    @SerializedName("timestamp")    val timestamp: Long,
    @SerializedName("delivered")    val delivered: Boolean,
    @SerializedName("read")         val read: Boolean
)

data class ErrorResponse(
    @SerializedName("error")     val error: String,
    @SerializedName("message")   val message: String,
    @SerializedName("timestamp") val timestamp: Long
)
