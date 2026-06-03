package com.messaging.service.online.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.Gson
import com.messaging.service.db.MessageDao
import com.messaging.service.db.MessageEntity
import com.messaging.service.kpi.KpiTracker
import com.messaging.service.offline.repository.OfflineMessagingRepository
import com.messaging.service.online.models.ChatHistoryResponse
import com.messaging.service.online.models.CloudSyncPayload
import com.messaging.service.online.models.DeliveryStatus
import com.messaging.service.online.models.IngestMessageRequest
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

class EmbeddedHttpServer(
    private val context:     Context,
    private val messageDao:  MessageDao,
    private val offlineRepo: OfflineMessagingRepository,
    private val kpiTracker:  KpiTracker,
    private val myUserId:    String        // needed for batch ingest
) {
    private val tag = "EmbeddedHttpServer"
    private var server: ApplicationEngine? = null
    private lateinit var serverScope: CoroutineScope

    private val gson = Gson()

    // OkHttp client for the cloud push — separate from the Ktor server
    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        const val DEFAULT_PORT = 8080

        // GeoLocation service — same host the UI uses
        const val GEOLOCATION_URL = "http://stormos-103075.duckdns.org"
    }

    // ── Connectivity check ────────────────────────────────────────────────────

    private fun isInternetAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps    = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // ── Cloud push helper ─────────────────────────────────────────────────────

    /**
     * Pushes a sent message to the cloud PostGIS DB via the GeoLocation service.
     * Called in a background coroutine — never blocks the UI response.
     */
    private suspend fun pushToCloud(payload: CloudSyncPayload) = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(listOf(payload))   // API expects a list
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$GEOLOCATION_URL/api/v1/sync/messages")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            okHttp.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.i(tag, "Cloud sync OK for message ${payload.messageId}")
                } else {
                    Log.w(tag, "Cloud sync failed: ${response.code} ${response.message}")
                }
            }
        } catch (e: Exception) {
            // Non-fatal — message is already stored locally
            Log.w(tag, "Cloud sync error (message kept locally): ${e.message}")
        }
    }

    // ── Server lifecycle ──────────────────────────────────────────────────────

    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun start(scope: CoroutineScope) {
        if (server != null) { Log.w(tag, "Already running"); return }
        serverScope = scope

        server = embeddedServer(CIO, port = DEFAULT_PORT, host = "0.0.0.0") {

            install(ContentNegotiation) {
                gson {
                    serializeNulls()
                    setLenient()
                }
            }

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

                // ── GET /api/v1/health ────────────────────────────────────────
                get("/api/v1/health") {
                    call.respond(HttpStatusCode.OK, mapOf(
                        "status"         to "ok",
                        "uptime_seconds" to kpiTracker.uptimeSeconds(),
                        "internet"       to isInternetAvailable(),
                        "timestamp"      to System.currentTimeMillis()
                    ))
                }

                // ── GET /api/v1/kpi ───────────────────────────────────────────
                get("/api/v1/kpi") {
                    call.respondText(
                        kpiTracker.getSnapshot(),
                        KtorContentType.Application.Json,
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
                            mapOf("error" to "invalid_body", "message" to (e.message ?: "bad json"))
                        )
                        return@post
                    }

                    val messageId = body.clientMessageId ?: UUID.randomUUID().toString()
                    val nowMs     = System.currentTimeMillis()
                    val online    = isInternetAvailable()

                    // ── Step 1: always persist locally first ──────────────────
                    messageDao.insert(MessageEntity(
                        messageId   = messageId,
                        userId      = body.userId,
                        chatId      = body.chatId,
                        senderId    = body.userId,
                        recipientId = body.recipientId,
                        content     = body.content,
                        contentType = body.contentType?.name?.lowercase() ?: "text",
                        timestamp   = nowMs,
                        delivered   = false,
                        source      = if (online) "online" else "ble"
                    ))

                    // ── Step 2: respond immediately to the UI ─────────────────
                    call.respond(HttpStatusCode.OK, SendMessageResponse(
                        messageId = messageId,
                        chatId    = body.chatId,
                        status    = DeliveryStatus.QUEUED,
                        timestamp = nowMs
                    ))

                    // ── Step 3: route in background ───────────────────────────
                    serverScope.launch {
                        if (online) {
                            kpiTracker.recordMessage("online", "sent")
                            Log.i(tag, "[$messageId] Online — syncing to cloud")

                            // Push to cloud PostGIS so the recipient can pull it
                            pushToCloud(CloudSyncPayload(
                                messageId   = messageId,
                                userId      = body.userId,
                                chatId      = body.chatId,
                                recipientId = body.recipientId,
                                content     = body.content,
                                contentType = body.contentType?.name?.lowercase() ?: "text"
                                // latitude/longitude default to 0.0 — location not required
                            ))
                        } else {
                            kpiTracker.recordMessage("ble", "sent")
                            Log.i(tag, "[$messageId] Offline — routing via BLE mesh")
                            offlineRepo.sendMeshMessage(
                                destinationId = body.recipientId,
                                plaintext     = body.content,
                                senderId      = body.userId,
                                scope         = serverScope
                            )
                        }
                    }
                }

                // ── POST /api/v1/messages/batch ───────────────────────────────
                // Receives messages pulled from the cloud sync and stores them
                // in the local Room DB. insertOrIgnore prevents duplicates.
                post("/api/v1/messages/batch") {
                    val messages = try {
                        call.receive<List<IngestMessageRequest>>()
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "invalid_body", "message" to (e.message ?: "bad json"))
                        )
                        return@post
                    }

                    var stored = 0
                    messages.forEach { msg ->
                        val localUserId = when {
                            msg.recipientId == myUserId -> myUserId  // we are the recipient
                            msg.senderId    == myUserId -> myUserId  // we are the sender
                            else                        -> myUserId  // store under our id regardless
                        }

                        messageDao.insertOrIgnore(MessageEntity(
                            messageId   = msg.messageId,
                            userId      = localUserId,    // ← always Phone B's UUID
                            chatId      = msg.chatId,
                            senderId    = msg.senderId,
                            recipientId = msg.recipientId,
                            content     = msg.content,
                            contentType = msg.contentType ?: "text",
                            timestamp   = msg.timestamp,
                            delivered   = msg.delivered ?: true,
                            source      = msg.source ?: "online"
                        ))
                        stored++
                    }

                    Log.d(tag, "Batch ingested $stored messages")
                    call.respond(HttpStatusCode.OK, mapOf(
                        "stored" to stored,
                        "total"  to messages.size
                    ))
                }

                // ── GET /api/v1/messages/{chat_id} ────────────────────────────
                get("/api/v1/messages/{chat_id}") {
                    val chatId = call.parameters["chat_id"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing_chat_id"))
                        return@get
                    }
                    val limit  = call.request.queryParameters["limit"]?.toIntOrNull()  ?: 50
                    val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                    val messages = messageDao.getByChatId(chatId, limit, offset).map { it.toOnlineMessage() }
                    val total    = messageDao.countByChatId(chatId)

                    call.respond(HttpStatusCode.OK, ChatHistoryResponse(
                        chatId   = chatId,
                        messages = messages,
                        total    = total,
                        hasMore  = (offset + messages.size) < total
                    ))
                }

                // ── GET /api/v1/chats ─────────────────────────────────────────
                get("/api/v1/chats") {
                    val userId = call.request.queryParameters["user_id"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing_user_id"))
                        return@get
                    }

                    val latest = messageDao.getLatestMessagePerChat(userId)
                    val summaries = latest.map { msg ->
                        val otherId = if (msg.senderId == userId) msg.recipientId else msg.senderId
                        mapOf(
                            "chat_id"       to msg.chatId,
                            "other_user_id" to otherId,
                            "last_message"  to msg.content,
                            "timestamp"     to msg.timestamp,
                            "unread"        to false
                        )
                    }

                    call.respond(HttpStatusCode.OK, mapOf(
                        "user_id" to userId,
                        "chats"   to summaries,
                        "total"   to summaries.size
                    ))
                }
            }
        }

        scope.launch {
            try {
                server?.start(wait = false)
                Log.i(tag, "HTTP server started on :$DEFAULT_PORT")
            } catch (e: Exception) {
                Log.e(tag, "Failed to start: ${e.message}", e)
            }
        }
    }

    fun stop() {
        server?.stop(1_000, 3_000)
        server = null
    }

    fun isRunning() = server != null
}