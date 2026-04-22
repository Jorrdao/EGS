package com.messaging.service.online.api

import android.util.Log
import com.google.gson.Gson
import com.messaging.service.db.MessageDao
import com.messaging.service.db.MessageEntity
import com.messaging.service.kpi.KpiTracker
import com.messaging.service.online.models.ChatHistoryResponse
import com.messaging.service.online.models.DeliveryStatus
import com.messaging.service.online.models.SendMessageRequest
import com.messaging.service.online.models.SendMessageResponse
import com.messaging.service.online.repository.ApiResult
import com.messaging.service.online.repository.OnlineMessagingRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.serialization.gson.gson
import io.ktor.server.engine.ApplicationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Embedded Ktor/CIO HTTP server that exposes the Messaging Service REST API
 * to other applications running on the same device (or same network).
 *
 * Endpoints (mirror the OpenAPI spec in res/raw/openapi.yaml):
 *
 *   POST /api/v1/messages/send          → send a message via cloud
 *   GET  /api/v1/messages/{chat_id}     → get message history
 *   GET  /api/v1/health                 → liveness probe
 *   GET  /api/v1/kpi                    → KPI snapshot (JSON)
 *
 * Other apps call this via:
 *   http://127.0.0.1:PORT/api/v1/...    (same device)
 *
 * Default port: 8080 — configurable via constructor.
 */
@Singleton
class EmbeddedHttpServer @Inject constructor(
    private val messageDao: MessageDao,
    private val onlineRepo: OnlineMessagingRepository,
    private val kpiTracker: KpiTracker
) {
    private val tag = "EmbeddedHttpServer"
    private val gson = Gson()
    private var server: ApplicationEngine? = null

    val port: Int get() = DEFAULT_PORT

    companion object {
        const val DEFAULT_PORT = 8080
    }

    private lateinit var backgroundScope: CoroutineScope

    fun start(scope: CoroutineScope) {
        backgroundScope = scope
        if (server != null) {
            Log.w(tag, "Server already running on port $DEFAULT_PORT")
            return
        }

        server = embeddedServer(CIO, port = DEFAULT_PORT, host = "0.0.0.0") {

            // ── Plugins ───────────────────────────────────────────────────────
            install(ContentNegotiation) { gson() }

            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    Log.e(tag, "Unhandled error: ${cause.message}", cause)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "internal_error", "message" to (cause.message ?: "Unknown"))
                    )
                }
            }

            // ── Routes ────────────────────────────────────────────────────────
            routing {

                // ── Health probe ──────────────────────────────────────────────
                get("/api/v1/health") {
                    call.respond(HttpStatusCode.OK, mapOf(
                        "status"          to "ok",
                        "uptime_seconds"  to kpiTracker.uptimeSeconds(),
                        "ble_running"     to true,
                        "timestamp"       to System.currentTimeMillis()
                    ))
                }

                // ── KPI snapshot ──────────────────────────────────────────────
                get("/api/v1/kpi") {
                    call.respondText(
                        kpiTracker.getSnapshot(),
                        ContentType.Application.Json,
                        HttpStatusCode.OK
                    )
                }

                // ── POST /api/v1/messages/send ────────────────────────────────
                post("/api/v1/messages/send") {
                    val body = try {
                        call.receive<SendMessageRequest>()
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "invalid_body", "message" to e.message)
                        )
                        return@post
                    }

                    val messageId = body.clientMessageId ?: UUID.randomUUID().toString()
                    val nowMs = System.currentTimeMillis()
                    messageDao.insert(
                        MessageEntity(
                            messageId = messageId,
                            chatId = body.chatId,
                            senderId = "local",
                            recipientId = body.recipientId,
                            content = body.content,
                            contentType = body.contentType.name.lowercase(),
                            timestamp = nowMs,
                            delivered = false,
                            source = "online"
                        )
                    )
                    kpiTracker.increment(KpiTracker.Key.ONLINE_MESSAGES_SENT)

                    // 2. Respond immediately — caller never waits for cloud
                    call.respond(HttpStatusCode.OK, SendMessageResponse(
                        messageId = messageId,
                        chatId = body.chatId,
                        status = DeliveryStatus.QUEUED,   // ← was DELIVERED, now QUEUED
                        timestamp = nowMs
                    )
                    )

                    // 3. Try cloud silently in background
                    backgroundScope.launch {
                        when (val r = onlineRepo.sendMessage(body.chatId, body.recipientId, body.content)) {
                            is ApiResult.Success     -> Log.i(tag, "Cloud forward OK: $messageId")
                            is ApiResult.Error       -> Log.w(tag, "Cloud forward failed: ${r.code} ${r.message}")
                            is ApiResult.NetworkError -> Log.w(tag, "No cloud — $messageId queued locally")
                        }
                    }
                }

                // ── GET /api/v1/messages/{chat_id} ────────────────────────────
                get("/api/v1/messages/{chat_id}") {
                    val chatId  = call.parameters["chat_id"]
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "missing_chat_id")
                        )
                    val limit   = call.request.queryParameters["limit"]?.toIntOrNull()  ?: 50
                    val offset  = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                    val since   = call.request.queryParameters["since"]?.toLongOrNull()

                    val messages = messageDao.getByChatId(chatId, limit, offset).map { it.toOnlineMessage() }
                    call.respond(HttpStatusCode.OK, ChatHistoryResponse(
                        chatId = chatId,
                        messages = messages,
                        total = messages.size,
                        hasMore = messages.size == limit
                    )
                    )
                }
            }

        }

        scope.launch {
            try {
                server?.start(wait = false)
                Log.i(tag, "Embedded HTTP server started on port $DEFAULT_PORT")
                Log.i(tag, "Other apps can reach it at http://127.0.0.1:$DEFAULT_PORT/api/v1/")
                kpiTracker.increment(KpiTracker.Key.SERVICE_RESTARTS)
            } catch (e: Exception) {
                Log.e(tag, "Failed to start HTTP server: ${e.message}", e)
            }
        }
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 1_000, timeoutMillis = 3_000)
        server = null
        Log.i(tag, "Embedded HTTP server stopped")
    }

    fun isRunning() = server != null
}
