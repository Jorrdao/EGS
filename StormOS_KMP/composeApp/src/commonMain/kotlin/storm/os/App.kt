package storm.os

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import storm.os.UI.* // Garante que a pasta 'ui' está em minúsculas conforme a convenção

@Composable
fun App() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false, // Sugestão: implementar lógica de seleção real
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
                    icon = { Icon(Icons.Default.Add, "Vender") },
                    label = { Text("Criar") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("messageList") },
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
            composable("map") { MapScreen(onAdClick = { adId ->
                navController.navigate("adDetail/$adId")
            }) }

            composable("create") { CreateItemScreen() }

            // Fluxo de Mensagens: Lista -> Chat Individual
            composable("messageList") {
                MessageListScreen(onUserClick = { name ->
                    navController.navigate("chat/$name")
                })
            }
            composable("chat/{userName}") { backStackEntry ->
                val userName = backStackEntry.arguments?.getString("userName") ?: "Utilizador"
                MessagingScreen(userName)
            }
            composable("list") {
                ListScreen(onAdClick = { id ->
                    navController.navigate("adDetail/$id")
                })
            }
            composable("adDetail/{adId}") { backStackEntry ->
                val adId = backStackEntry.arguments?.getString("adId") ?: ""
                AdDetailScreen(adId = adId) { vendor ->
                    navController.navigate("chat/$vendor")
                }
            }
        }
    }
}