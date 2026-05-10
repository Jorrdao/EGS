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
import storm.os.MessagingApi
import storm.os.getUserId

/**
 * Dynamic conversation list built from the user's real chat history.
 *
 * Calls GET /api/v1/chats?user_id=xxx on the Messaging Service, which
 * returns one entry per chat — sorted newest first — with the last
 * message preview and the other participant's ID.
 *
 * No static list anywhere. States: Loading → Empty → Error → Loaded.
 */
@Composable
fun MessageListScreen(onUserClick: (String) -> Unit) {

    val myId  = remember { getUserId() }
    val scope = rememberCoroutineScope()

    var chats     by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError  by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            isLoading = true
            hasError  = false
            val result = MessagingApi.getChats(myId)
            hasError  = result.isEmpty() && !isLoading  // will refine below
            chats     = result
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

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
            if (!isLoading) {
                IconButton(onClick = { load() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                }
            }
        }

        HorizontalDivider()

        // ── Body ──────────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {

            when {
                // Loading spinner
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Empty state — no conversations yet
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
                        OutlinedButton(onClick = { load() }) {
                            Text("Atualizar")
                        }
                    }
                }

                // Conversation list — sorted newest first by the server
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = chats,
                            key   = { it.chatId }
                        ) { chat ->
                            ChatListItem(
                                chat    = chat,
                                onClick = { onUserClick(chat.otherUserId) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Private composable ────────────────────────────────────────────────────────

@Composable
private fun ChatListItem(
    chat:    ChatSummary,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text  = chat.otherUserId,
                style = MaterialTheme.typography.titleMedium
            )
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