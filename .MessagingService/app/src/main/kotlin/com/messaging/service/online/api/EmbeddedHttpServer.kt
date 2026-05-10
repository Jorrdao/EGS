package com.messaging.service.online.api

import android.util.Log
import com.messaging.service.db.MessageDao
import com.messaging.service.db.MessageEntity
import com.messaging.service.kpi.KpiTracker
import com.messaging.service.online.models.ChatHistoryResponse
import com.messaging.service.online.models.DeliveryStatus
import com.messaging.service.online.models.SendMessageRequest
import com.messaging.service.online.models.SendMessageResponse
import io.ktor.http.ContentType as KtorContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Embedded Ktor/CIO HTTP server.
 *
 * Endpoints:
 *   POST /api/v1/messages/send          — store a message locally
 *   GET  /api/v1/messages/{chat_id}     — read chat history from local DB
 *   GET  /api/v1/health                 — liveness probe
 *   GET  /api/v1/kpi                    — metrics snapshot
 */
@Singleton
class EmbeddedHttpServer @Inject constructor(
    private val messageDao: MessageDao,
    private val kpiTracker: KpiTracker
) {
    private val tag = "EmbeddedHttpServer"
    private var server: ApplicationEngine? = null

    companion object {
        const val DEFAULT_PORT = 8080
    }

    fun start(scope: CoroutineScope) {
        if (server != null) { Log.w(tag, "Already running"); return }

        server = embeddedServer(CIO, port = DEFAULT_PORT, host = "0.0.0.0") {

            install(ContentNegotiation) { gson() }

            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    Log.e(tag, "Unhandled: ${cause.message}", cause)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "internal_error", "message" to (cause.message ?: "unknown"))
                    )
                }
            }

            routing {

                // GET /api/v1/health
                get("/api/v1/health") {
                    call.respond(HttpStatusCode.OK, mapOf(
                        "status"         to "ok",
                        "uptime_seconds" to kpiTracker.uptimeSeconds(),
                        "timestamp"      to System.currentTimeMillis()
                    ))
                }

                // GET /api/v1/kpi
                get("/api/v1/kpi") {
                    call.respondText(
                        kpiTracker.getSnapshot(),
                        KtorContentType.Application.Json,
                        HttpStatusCode.OK
                    )
                }

                // POST /api/v1/messages/send
                post("/api/v1/messages/send") {
                    val body = try {
                        call.receive<SendMessageRequest>()
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "invalid_body", "message" to (e.message ?: "bad json"))
                        )
                        return@post
                    }

                    val messageId = body.clientMessageId ?: UUID.randomUUID().toString()
                    val nowMs     = System.currentTimeMillis()

                    messageDao.insert(MessageEntity(
                        messageId   = messageId,
                        userId      = body.userId,
                        chatId      = body.chatId,
                        senderId    = body.userId,
                        recipientId = body.recipientId,
                        content     = body.content,
                        contentType = body.contentType.name.lowercase(),
                        timestamp   = nowMs,
                        delivered   = false,
                        source      = "online"
                    ))

                    kpiTracker.increment(KpiTracker.Key.ONLINE_MESSAGES_SENT)
                    Log.d(tag, "Stored: $messageId for user=${body.userId}")

                    call.respond(HttpStatusCode.OK, SendMessageResponse(
                        messageId = messageId,
                        chatId    = body.chatId,
                        status    = DeliveryStatus.QUEUED,
                        timestamp = nowMs
                    ))
                }

                // GET /api/v1/chats
                // Called by: UI App to build the dynamic conversation list.
                // Returns one entry per chat the user has participated in,
                // with the last message content, timestamp, and the other
                // participant's ID — everything the list screen needs.
                get("/api/v1/chats") {
                    val userId = call.request.queryParameters["user_id"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing_user_id"))
                        return@get
                    }

                    val latest = messageDao.getLatestMessagePerChat(userId)

                    // Derive the other participant's ID from the message fields
                    val summaries = latest.map { msg ->
                        val otherId = if (msg.senderId == userId) msg.recipientId else msg.senderId
                        mapOf(
                            "chat_id"        to msg.chatId,
                            "other_user_id"  to otherId,
                            "last_message"   to msg.content,
                            "timestamp"      to msg.timestamp,
                            "unread"         to false         // unread tracking is future work
                        )
                    }

                    call.respond(HttpStatusCode.OK, mapOf(
                        "user_id" to userId,
                        "chats"   to summaries,
                        "total"   to summaries.size
                    ))
                }

                // GET /api/v1/messages/{chat_id}
                get("/api/v1/messages/{chat_id}") {
                    val chatId = call.parameters["chat_id"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing_chat_id"))
                        return@get
                    }
                    val limit  = call.request.queryParameters["limit"]?.toIntOrNull()  ?: 50
                    val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                    val messages = messageDao.getByChatId(chatId, limit, offset)
                        .map { it.toOnlineMessage() }

                    call.respond(HttpStatusCode.OK, ChatHistoryResponse(
                        chatId   = chatId,
                        messages = messages,
                        total    = messages.size,
                        hasMore  = messages.size == limit
                    ))
                }
            }
        }

        scope.launch {
            try {
                server?.start(wait = false)
                Log.i(tag, "HTTP server started — http://127.0.0.1:$DEFAULT_PORT/api/v1/")
            } catch (e: Exception) {
                Log.e(tag, "Failed to start: ${e.message}", e)
            }
        }
    }

    fun stop() {
        server?.stop(1_000, 3_000)
        server = null
        Log.i(tag, "HTTP server stopped")
    }

    fun isRunning() = server != null
}