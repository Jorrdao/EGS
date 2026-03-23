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

@Composable
fun MessagingScreen() {
    var messageText by remember { mutableStateOf("") }
    // Lista mutável para simular mensagens em memória
    val messages = remember { mutableStateListOf("Olá! O item ainda está disponível?", "Sim, está em perfeitas condições.") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Chat: Vendedor Alpha",
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
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.align(Alignment.End).widthIn(max = 280.dp)
                ) {
                    Text(msg, modifier = Modifier.padding(12.dp))
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
                            messages.add(messageText)
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