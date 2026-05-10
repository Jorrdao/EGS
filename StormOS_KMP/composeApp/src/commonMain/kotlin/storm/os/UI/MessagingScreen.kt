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
import storm.os.MessagingApi
import storm.os.getUserId

/**
 * Chat screen for a single conversation.
 *
 * [userName] is used both as the display name and the recipientId.
 * The chatId is derived deterministically from both user IDs so both
 * sides of the conversation always resolve to the same chat.
 *
 * Messages are loaded from the Messaging Service local DB on open and
 * after every send, so the list stays in sync across app restarts.
 */
@Composable
fun MessagingScreen(userName: String) {
    val myId       = remember { getUserId() }
    // Sort IDs so the chat_id is the same regardless of who initiates
    val chatId     = remember(userName) {
        "chat_" + listOf(myId, userName).sorted().joinToString("_")
    }

    var messageText by remember { mutableStateOf("") }
    var messages    by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(true) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }

    val scope       = rememberCoroutineScope()
    val listState   = rememberLazyListState()

    // Load history when the screen opens or chatId changes
    LaunchedEffect(chatId) {
        isLoading = true
        errorMsg  = null
        messages  = MessagingApi.getHistory(chatId)
        isLoading = false
        // Scroll to the most recent message
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    // Auto-scroll to bottom whenever new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    // Poll for new incoming messages (BLE messages arrive in the background).
    // The interval is short enough to feel responsive but not hammering the DB.
    LaunchedEffect(chatId) {
        while (true) {
            kotlinx.coroutines.delay(3_000L)
            val updated = MessagingApi.getHistory(chatId)
            // Only update state if something actually changed to avoid recomposition
            if (updated.size != messages.size ||
                updated.lastOrNull()?.messageId != messages.lastOrNull()?.messageId) {
                messages = updated
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Text(
            text  = "Chat: $userName",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleLarge
        )
        HorizontalDivider()

        // ── Message list ──────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
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
                        state            = listState,
                        modifier         = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement  = Arrangement.spacedBy(8.dp),
                        contentPadding   = PaddingValues(vertical = 16.dp)
                    ) {
                        items(
                            items = messages,
                            key   = { it.messageId }
                        ) { msg ->
                            val isMe      = msg.senderId == myId
                            val alignment = if (isMe) Alignment.End else Alignment.Start
                            val color     = if (isMe)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.secondaryContainer

                            Column(
                                modifier           = Modifier.fillMaxWidth(),
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

        // ── Error banner (shown if service unreachable) ───────────────────────
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
                    onClick  = {
                        val text = messageText.trim()
                        if (text.isBlank()) return@IconButton
                        messageText = ""    // clear immediately for snappy UX
                        scope.launch {
                            val ok = MessagingApi.sendMessage(
                                userId      = myId,
                                chatId      = chatId,
                                recipientId = userName,
                                content     = text
                            )
                            if (ok) {
                                // Reload history so Room DB is the single source of truth
                                messages  = MessagingApi.getHistory(chatId)
                                errorMsg  = null
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