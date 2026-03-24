package storm.os.UI

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Message(val text: String, val isMe: Boolean)

@Composable
fun MessagingScreen(userName: String) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            Message("Olá! O item ainda está disponível?", false),
            Message("Sim, está em perfeitas condições.", true)
        )
    }
    // ... restante estrutura da Column ...
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Chat: $userName",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleLarge
        )

        HorizontalDivider()

        // Área de mensagens com scroll automático
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { msg ->
                val alignment = if (msg.isMe) Alignment.End else Alignment.Start
                val color = if (msg.isMe) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.secondaryContainer

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = color),
                        modifier = Modifier.widthIn(max = 280.dp).padding(vertical = 4.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(msg.text, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }

        // Barra de input fixa no fundo
        Surface(tonalElevation = 3.dp) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Mensagem...") },
                    modifier = Modifier.weight(1f),
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            messages.add(Message(messageText, true))
                            messageText = ""
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