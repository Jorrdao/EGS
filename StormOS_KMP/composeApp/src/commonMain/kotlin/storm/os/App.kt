package storm.os

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import storm.os.UI.*


@Composable
fun App() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("map") },
                    icon = { Icon(Icons.Default.Map, "Mapa") },
                    label = { Text("Mapa") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("list") },
                    icon = { Icon(Icons.Default.List, "Lista") },
                    label = { Text("Anúncios") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("create") },
                    icon = { Icon(Icons.Default.Add, "Criar") },
                    label = { Text("Vender") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("messages") },
                    icon = { Icon(Icons.Default.Email, "Mensagens") },
                    label = { Text("Chat") }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "map",
            modifier = Modifier.padding(padding)
        ) {
            composable("map") { MapScreen() }
            composable("list") { ListScreen() }
            composable("create") { CreateItemScreen() } // Move o teu código antigo para aqui
            composable("messages") { MessagingScreen() }
        }
    }
}