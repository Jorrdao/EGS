package com.messaging.service.online.repository

import com.messaging.service.db.MessageDao
import com.messaging.service.db.MessageEntity
import com.messaging.service.kpi.KpiTracker
import com.messaging.service.online.api.NetworkClient
import com.messaging.service.online.models.ChatHistoryResponse
import com.messaging.service.online.models.Message
import com.messaging.service.online.models.SendMessageRequest
import com.messaging.service.online.models.SendMessageResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int?, val message: String) : ApiResult<Nothing>()
    data object NetworkError : ApiResult<Nothing>()
}

@Singleton
class OnlineMessagingRepository @Inject constructor(
    private val networkClient: NetworkClient,
    private val messageDao: MessageDao,
    private val kpiTracker: KpiTracker
) {

    /**
     * POST /api/v1/messages/send
     * Sends a message via cloud. On success, persists it in the local Room DB.
     */
    suspend fun sendMessage(
        chatId: String,
        recipientId: String,
        content: String
    ): ApiResult<SendMessageResponse> {
        val request = SendMessageRequest(
            chatId = chatId,
            recipientId = recipientId,
            content = content,
            clientMessageId = UUID.randomUUID().toString()
        )
        return try {
            val response = networkClient.messagingApi.sendMessage(request)
            if (response.isSuccessful) {
                val body = response.body()!!
                kpiTracker.increment(KpiTracker.Key.ONLINE_MESSAGES_SENT)
                // Persist locally
                messageDao.insert(
                    MessageEntity.fromOnlineSend(chatId, recipientId, content, body.messageId)
                )
                ApiResult.Success(body)
            } else {
                ApiResult.Error(response.code(), response.message())
            }
        } catch (e: Exception) {
            ApiResult.NetworkError
        }
    }

    /**
     * GET /api/v1/messages/{chat_id}
     * Fetches remote history and merges with local cache.
     */
    suspend fun getChatHistory(
        chatId: String,
        limit: Int = 50,
        offset: Int = 0,
        sinceTimestamp: Long? = null
    ): ApiResult<ChatHistoryResponse> {
        return try {
            val response = networkClient.messagingApi.getChatHistory(
                chatId, limit, offset, sinceTimestamp
            )
            if (response.isSuccessful) {
                val body = response.body()!!
                // Persist all fetched messages locally
                body.messages.forEach { msg ->
                    messageDao.insertOrIgnore(MessageEntity.fromRemoteMessage(chatId, msg))
                }
                kpiTracker.increment(KpiTracker.Key.ONLINE_MESSAGES_RECEIVED, body.messages.size.toLong())
                ApiResult.Success(body)
            } else {
                ApiResult.Error(response.code(), response.message())
            }
        } catch (e: Exception) {
            ApiResult.NetworkError
        }
    }

    /** Local-only stream of messages for a chat (Room Flow). */
    fun observeLocalMessages(chatId: String): Flow<List<Message>> =
        messageDao.observeByChatId(chatId).map { entities ->
            entities.map { it.toOnlineMessage() }
        }
}
