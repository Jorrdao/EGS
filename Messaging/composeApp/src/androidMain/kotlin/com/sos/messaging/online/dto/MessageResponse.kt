package com.sos.messaging.online.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageResponse(
    val messageId: String,
    val senderId: String,
    val destinationId: String,
    val chatId: String,
    val payload: String,
    val timestamp: Long
)