package com.taio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.taio.app.data.AppRepository
import com.taio.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TaioApp()
            }
        }
    }
}

private data class TabItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    TabItem("dashboard", "الرئيسية", Icons.Default.Home),
    TabItem("products", "المنتجات", Icons.Default.Inventory2),
    TabItem("orders", "الأوردرات", Icons.Default.ShoppingCart),
    TabItem("logostamper", "الختم", Icons.Default.Image),
    TabItem("settings", "الحاسبة", Icons.Default.Settings)
)

@Composable
fun TaioApp() {
    var loggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            loggedIn = auth.currentUser != null
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
    }

    if (!loggedIn) {
        AuthScreen(onLoggedIn = { loggedIn = true })
    } else {
        MainScaffold()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val repo = remember { AppRepository() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "dashboard"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TAIO") },
                actions = {
                    IconButton(onClick = { FirebaseAuth.getInstance().signOut() }) {
                        Icon(Icons.Default.Logout, contentDescription = "خروج")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = "dashboard", modifier = androidx.compose.ui.Modifier.padding(padding)) {
            composable("dashboard") { DashboardScreen(repo) }
            composable("products") { ProductsScreen(repo) }
            composable("orders") { OrdersScreen(repo) }
            composable("logostamper") { LogoStamperScreen() }
            composable("settings") { SettingsScreen(repo) }
        }
    }
}
