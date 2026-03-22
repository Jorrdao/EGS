package com.sos.messaging.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val messageId: String,
    val senderId: String,
    val destinationId: String,
    val payload: String,       // plaintext on this side; encrypted on wire
    val timestamp: Long,
    val chatId: String,
    val ttl: Int = 5
)