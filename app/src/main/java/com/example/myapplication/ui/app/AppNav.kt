package com.example.myapplication.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.screens.AutomationsScreen
import com.example.myapplication.ui.screens.InboxScreen
import com.example.myapplication.ui.screens.NumbersScreen
import com.example.myapplication.ui.screens.SettingsScreen
import com.example.myapplication.ui.screens.ThreadScreen
import com.example.myapplication.ui.screens.TokensScreen

sealed class AppRoute(val route: String, val label: String) {
    data object Inbox : AppRoute("inbox", "Inbox")
    data object Numbers : AppRoute("numbers", "Numbers")
    data object Automations : AppRoute("automations", "Automations")
    data object Settings : AppRoute("settings", "Settings")

    data object Tokens : AppRoute("settings/tokens", "Tokens")
    data object Thread : AppRoute("thread/{id}", "Thread") {
        fun create(id: String) = "thread/$id"
    }
}

@Composable
fun AppScaffold(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    isWide: Boolean,
) {
    val nav = rememberNavController()
    val tabs = listOf(AppRoute.Inbox, AppRoute.Numbers, AppRoute.Automations, AppRoute.Settings)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!isWide) BottomNavBar(nav, tabs)
        },
    ) { inner ->
        Row(Modifier.padding(inner).fillMaxSize()) {
            if (isWide) {
                AppRail(nav, tabs)
            }
            Box(Modifier.fillMaxSize()) {
                AppNavHost(nav = nav, onLogout = onLogout)
            }
        }
    }
}

@Composable
private fun BottomNavBar(nav: NavHostController, tabs: List<AppRoute>) {
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route

    NavigationBar {
        tabs.forEach { t ->
            NavigationBarItem(
                selected = route == t.route,
                onClick = {
                    nav.navigate(t.route) {
                        launchSingleTop = true
                        popUpTo(AppRoute.Inbox.route) { saveState = true }
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = when (t) {
                            AppRoute.Inbox -> Icons.Default.Chat
                            AppRoute.Numbers -> Icons.Default.SimCard
                            AppRoute.Automations -> Icons.Default.Tune
                            else -> Icons.Default.Settings
                        },
                        contentDescription = t.label
                    )
                },
                label = { Text(t.label) }
            )
        }
    }
}

@Composable
private fun AppRail(nav: NavHostController, tabs: List<AppRoute>) {
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route

    NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        tabs.forEach { t ->
            NavigationRailItem(
                selected = route == t.route,
                onClick = {
                    nav.navigate(t.route) {
                        launchSingleTop = true
                        popUpTo(AppRoute.Inbox.route) { saveState = true }
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = when (t) {
                            AppRoute.Inbox -> Icons.Default.Chat
                            AppRoute.Numbers -> Icons.Default.SimCard
                            AppRoute.Automations -> Icons.Default.Tune
                            else -> Icons.Default.Settings
                        },
                        contentDescription = t.label
                    )
                },
                label = { Text(t.label) }
            )
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, onLogout: () -> Unit) {
    NavHost(navController = nav, startDestination = AppRoute.Inbox.route) {
        composable(AppRoute.Inbox.route) {
            InboxScreen(onOpenThread = { nav.navigate(AppRoute.Thread.create(it)) })
        }
        composable(AppRoute.Numbers.route) { NumbersScreen() }
        composable(AppRoute.Automations.route) { AutomationsScreen() }
        composable(AppRoute.Settings.route) {
            SettingsScreen(
                onOpenTokens = { nav.navigate(AppRoute.Tokens.route) },
                onLogout = onLogout,
            )
        }
        composable(AppRoute.Tokens.route) { TokensScreen() }
        composable(AppRoute.Thread.route) { backStack ->
            val id = backStack.arguments?.getString("id") ?: ""
            ThreadScreen(conversationId = id, onBack = { nav.popBackStack() })
        }
    }
}
