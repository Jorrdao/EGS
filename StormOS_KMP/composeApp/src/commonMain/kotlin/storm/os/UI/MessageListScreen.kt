package storm.os.UI

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MessageListScreen(onUserClick: (String) -> Unit) {
    val chatList = remember { listOf("João Silva", "Maria Santos", "Tech Store", "Suporte StormOS") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "As tuas Conversas",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn {
            items(chatList) { contact ->
                ListItem(
                    headlineContent = { Text(contact, style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text("Clica para abrir o chat...") },
                    leadingContent = {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            // Placeholder para avatar
                        }
                    },
                    modifier = Modifier.clickable { onUserClick(contact) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}