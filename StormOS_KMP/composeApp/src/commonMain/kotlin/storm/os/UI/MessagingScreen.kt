package storm.os.UI

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import storm.os.ChatMessage
import storm.os.IngestMessageRequest
import storm.os.MessagingApi
import storm.os.StormApi
import storm.os.getUserId

@Composable
fun MessagingScreen(userName: String, displayName: String = userName) {
    val myId    = remember { getUserId() }
    val chatId  = remember(userName) {
        "chat_" + listOf(myId, userName).sorted().joinToString("_")
    }

    var messageText by remember { mutableStateOf("") }
    var messages    by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(true) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }

    val scope     = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // ── Initial load ──────────────────────────────────────────────────────────
    LaunchedEffect(chatId) {
        isLoading = true
        errorMsg  = null
        messages  = MessagingApi.getHistory(chatId)
        isLoading = false
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }

    // ── Auto-scroll when new messages arrive ──────────────────────────────────
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    // ── Local DB poll — 1 second interval ─────────────────────────────────────
    // Picks up BLE messages that arrive directly in Room DB.
    // Lightweight: only a local SQL query, no network.
    LaunchedEffect(chatId) {
        while (true) {
            kotlinx.coroutines.delay(1_000L)
            val updated = MessagingApi.getHistory(chatId)
            if (updated.size != messages.size ||
                updated.lastOrNull()?.messageId != messages.lastOrNull()?.messageId) {
                messages = updated
            }
        }
    }

    // ── Cloud sync poll — 10 second interval ──────────────────────────────────
    // Pulls online messages from PostGIS for this specific chat and ingests
    // them into the local DB. The 1s local poll above picks them up immediately
    // after ingest, so the user sees new messages within ~1 second of sync.
    LaunchedEffect(chatId) {
        while (true) {
            kotlinx.coroutines.delay(5_000L)
            try {
                val cloudMessages = StormApi.syncMessages(userId = myId)
                val forThisChat = cloudMessages.filter { it.chat_id == chatId }
                if (forThisChat.isNotEmpty()) {
                    MessagingApi.ingestMessages(forThisChat.map { msg ->
                        IngestMessageRequest(
                            messageId   = msg.message_id,
                            senderId    = msg.sender_id,
                            recipientId = msg.recipient_id,
                            chatId      = msg.chat_id,
                            content     = msg.content,
                            contentType = msg.content_type ?: "text",
                            timestamp   = msg.timestamp,
                            delivered   = msg.delivered ?: true,
                            source      = "online"
                        )
                    })
                    // Local DB poll will pick up the new messages within 1s
                }
            } catch (e: Exception) {
                // Non-fatal — local BLE messages still show via the 1s poll
                println("MessagingScreen cloud sync error: ${e.message}")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Text(
            text     = "Chat: $displayName",
            modifier = Modifier.padding(16.dp),
            style    = MaterialTheme.typography.titleLarge
        )
        HorizontalDivider()

        // ── Message list ──────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                messages.isEmpty() -> {
                    Text(
                        text     = "Ainda não há mensagens.\nEnvia a primeira!",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        state               = listState,
                        modifier            = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding      = PaddingValues(vertical = 16.dp)
                    ) {
                        items(items = messages, key = { it.messageId }) { msg ->
                            val isMe      = msg.senderId == myId
                            val alignment = if (isMe) Alignment.End else Alignment.Start
                            val color     = if (isMe)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.secondaryContainer

                            Column(
                                modifier            = Modifier.fillMaxWidth(),
                                horizontalAlignment = alignment
                            ) {
                                Card(
                                    colors   = CardDefaults.cardColors(containerColor = color),
                                    modifier = Modifier
                                        .widthIn(max = 280.dp)
                                        .padding(vertical = 4.dp),
                                    shape    = MaterialTheme.shapes.medium
                                ) {
                                    Text(
                                        text     = msg.content,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Error banner ──────────────────────────────────────────────────────
        errorMsg?.let {
            Text(
                text     = it,
                color    = MaterialTheme.colorScheme.error,
                style    = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // ── Input bar ─────────────────────────────────────────────────────────
        Surface(tonalElevation = 3.dp) {
            Row(
                modifier          = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = messageText,
                    onValueChange = { messageText = it },
                    placeholder   = { Text("Mensagem...") },
                    modifier      = Modifier.weight(1f),
                    shape         = CircleShape,
                    singleLine    = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val text = messageText.trim()
                        if (text.isBlank()) return@IconButton
                        messageText = ""
                        scope.launch {
                            val ok = MessagingApi.sendMessage(
                                userId      = myId,
                                chatId      = chatId,
                                recipientId = userName,
                                content     = text
                            )
                            if (ok) {
                                messages = MessagingApi.getHistory(chatId)
                                errorMsg = null
                            } else {
                                errorMsg = "Serviço de mensagens indisponível. Tenta novamente."
                            }
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}