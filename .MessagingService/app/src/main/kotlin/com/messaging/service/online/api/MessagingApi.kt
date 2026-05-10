package com.messaging.service.online.api

import com.messaging.service.online.models.ChatHistoryResponse
import com.messaging.service.online.models.SendMessageRequest
import com.messaging.service.online.models.SendMessageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Online Messaging REST API.
 *
 * Matches the OpenAPI spec in res/raw/openapi.yaml.
 * Swagger annotations are compile-only and have no runtime cost on Android.
 */
@Tag(name = "messages", description = "Cloud messaging operations")
interface MessagingApi {

    /**
     * POST /api/v1/messages/send
     * Sends a message directly via cloud.
     */
    @Operation(
        summary = "Send a message directly via cloud",
        operationId = "sendMessage",
        responses = [
            ApiResponse(responseCode = "200", description = "Message accepted",
                content = [Content(schema = Schema(implementation = SendMessageResponse::class))]),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "503", description = "Service unavailable")
        ]
    )
    @POST("messages/send")
    suspend fun sendMessage(
        @Body request: SendMessageRequest
    ): Response<SendMessageResponse>

    /**
     * GET /api/v1/messages/{chat_id}
     * Returns the message history for the given chat.
     */
    @Operation(
        summary = "Get message history for a chat",
        operationId = "getChatHistory",
        responses = [
            ApiResponse(responseCode = "200", description = "Chat history",
                content = [Content(schema = Schema(implementation = ChatHistoryResponse::class))]),
            ApiResponse(responseCode = "404", description = "Chat not found"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    @GET("messages/{chat_id}")
    suspend fun getChatHistory(
        @Parameter(description = "Unique chat room identifier")
        @Path("chat_id") chatId: String,
        @Query("limit")  limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("since")  sinceTimestamp: Long? = null
    ): Response<ChatHistoryResponse>
}
