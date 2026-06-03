package com.messaging.service.online.models

import com.google.gson.annotations.SerializedName

data class SendMessageRequest(
    @SerializedName("user_id")           val userId: String,
    @SerializedName("chat_id")           val chatId: String,
    @SerializedName("recipient_id")      val recipientId: String,
    @SerializedName("content")           val content: String,
    @SerializedName("content_type")      val contentType: ContentType? = ContentType.TEXT,
    @SerializedName("client_message_id") val clientMessageId: String? = null
)

enum class ContentType {
    @SerializedName("text")      TEXT,
    @SerializedName("image_url") IMAGE_URL,
    @SerializedName("location")  LOCATION
}

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
    @SerializedName("content_type") val contentType: ContentType? = ContentType.TEXT,
    @SerializedName("timestamp")    val timestamp: Long,
    @SerializedName("delivered")    val delivered: Boolean,
    @SerializedName("read")         val read: Boolean
)

data class ErrorResponse(
    @SerializedName("error")     val error: String,
    @SerializedName("message")   val message: String,
    @SerializedName("timestamp") val timestamp: Long
)

/**
 * Used by POST /api/v1/messages/batch — receives messages from the UI
 * that were pulled from the cloud sync and stores them in Room DB.
 */
data class IngestMessageRequest(
    @SerializedName("message_id")   val messageId:   String,
    @SerializedName("sender_id")    val senderId:    String,
    @SerializedName("recipient_id") val recipientId: String,
    @SerializedName("chat_id")      val chatId:      String,
    @SerializedName("content")      val content:     String,
    @SerializedName("content_type") val contentType: String?  = "text",
    @SerializedName("timestamp")    val timestamp:   Long,
    @SerializedName("delivered")    val delivered:   Boolean? = true,
    @SerializedName("source")       val source:      String?  = "online"
)

/**
 * Used to push a sent message to the cloud PostGIS via
 * POST /api/v1/sync/messages on the GeoLocation service.
 */
data class CloudSyncPayload(
    @SerializedName("message_id")   val messageId:   String,
    @SerializedName("user_id")      val userId:      String,   // sender
    @SerializedName("chat_id")      val chatId:      String,
    @SerializedName("recipient_id") val recipientId: String,
    @SerializedName("content")      val content:     String,
    @SerializedName("content_type") val contentType: String = "text",
    @SerializedName("latitude")     val latitude:    Double = 0.0,
    @SerializedName("longitude")    val longitude:   Double = 0.0
)