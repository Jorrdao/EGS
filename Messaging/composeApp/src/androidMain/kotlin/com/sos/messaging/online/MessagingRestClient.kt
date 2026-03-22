package com.sos.messaging.online

import com.sos.messaging.model.Message
import com.sos.messaging.online.dto.MessageResponse
import com.sos.messaging.online.dto.SendMessageRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class MessagingRestClient(private val baseUrl: String) {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }

    // POST /api/v1/messages/send
    suspend fun sendMessage(message: Message): Result<Unit> = runCatching {
        val request = SendMessageRequest(
            senderId      = message.senderId,
            destinationId = message.destinationId,
            chatId        = message.chatId,
            payload       = message.payload,
            timestamp     = message.timestamp
        )
        client.post("$baseUrl/api/v1/messages/send") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        Unit
    }

    // GET /api/v1/messages/{chat_id}
    suspend fun getMessageHistory(chatId: String): Result<List<Message>> = runCatching {
        val responses = client.get("$baseUrl/api/v1/messages/$chatId").body<List<MessageResponse>>()
        responses.map { r ->
            Message(
                messageId     = r.messageId,
                senderId      = r.senderId,
                destinationId = r.destinationId,
                chatId        = r.chatId,
                payload       = r.payload,
                timestamp     = r.timestamp
            )
        }
    }

    fun close() = client.close()
}