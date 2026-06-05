package storm.os.UI

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import storm.os.ChatSummary
import storm.os.IngestMessageRequest
import storm.os.MessagingApi
import storm.os.StormApi
import storm.os.getUserId

@Composable
fun MessageListScreen(onUserClick: (String) -> Unit) {

    val myId  = remember { getUserId() }
    val scope = rememberCoroutineScope()

    var chats      by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    var isLoading  by remember { mutableStateOf(true) }
    var syncStatus by remember { mutableStateOf("") }

    /**
     * Full sync + reload.
     * [showLoading] = true for initial load and manual refresh (shows spinner).
     * [showLoading] = false for background auto-refresh (silent, no spinner).
     */
    fun load(showLoading: Boolean = true) {
        scope.launch {
            if (showLoading) {
                isLoading  = true
                syncStatus = ""
            }

            // ── Pull from cloud ───────────────────────────────────────────────
            val cloudMessages = StormApi.syncMessages(userId = myId)

            if (cloudMessages.isNotEmpty()) {
                val requests = cloudMessages.map { msg ->
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
                }
                MessagingApi.ingestMessages(requests)
                if (showLoading) {
                    syncStatus = "${cloudMessages.size} mensagem(ns) sincronizada(s)"
                }
            }

            // ── Load from local DB ────────────────────────────────────────────
            chats = MessagingApi.getChats(myId)

            if (showLoading) isLoading = false
        }
    }

    // ── Initial load (with spinner) ───────────────────────────────────────────
    LaunchedEffect(Unit) { load(showLoading = true) }

    // ── Background auto-refresh — 15 second interval ──────────────────────────
    // Runs silently in the background so the user sees new conversations
    // appear automatically without pressing the reload button.
    // Does NOT show the loading spinner or sync status banner.
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(15_000L)
            load(showLoading = false)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "As tuas Conversas",
                style    = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            // Manual refresh button — always visible, shows spinner when tapped
            IconButton(onClick = { load(showLoading = true) }) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier  = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                }
            }
        }

        // ── Sync status banner (only shown after manual refresh) ──────────────
        if (syncStatus.isNotEmpty()) {
            Text(
                text     = syncStatus,
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        HorizontalDivider()

        // ── Body ──────────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {

            when {
                // Initial loading spinner
                isLoading && chats.isEmpty() -> {
                    Column(
                        modifier            = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "A sincronizar mensagens...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                chats.isEmpty() -> {
                    Column(
                        modifier            = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector        = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier           = Modifier.size(64.dp),
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Ainda não tens conversas.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Abre um anúncio e envia uma mensagem ao vendedor para começar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { load(showLoading = true) }) {
                            Text("Atualizar")
                        }
                    }
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(items = chats, key = { it.chatId }) { chat ->
                            ChatListItem(
                                chat    = chat,
                                onClick = { onUserClick(chat.otherUserId) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatListItem(chat: ChatSummary, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(text = chat.otherUserId, style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = {
            Text(
                text     = chat.lastMessage,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Surface(
                shape    = androidx.compose.foundation.shape.CircleShape,
                color    = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector        = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        modifier           = Modifier.size(22.dp),
                        tint               = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}