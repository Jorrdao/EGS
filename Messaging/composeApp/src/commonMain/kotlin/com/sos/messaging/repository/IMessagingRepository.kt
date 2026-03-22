package com.sos.messaging.repository

import com.sos.messaging.model.Message

interface IMessagingRepository {
    suspend fun sendMessage(message: Message): Result<Unit>
    suspend fun getMessageHistory(chatId: String): Result<List<Message>>
}