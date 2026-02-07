package com.example.myapplication.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
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

    data object Tokens : AppRoute("settings/tokens", "API Tokens")
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
    val navController = rememberNavController()
    val tabs = listOf(AppRoute.Inbox, AppRoute.Numbers, AppRoute.Automations, AppRoute.Settings)
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination
    val showTabs = tabs.any { tab -> currentDestination.isTabRoute(tab.route) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!isWide && showTabs) {
                BottomNavBar(nav = navController, tabs = tabs)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (isWide && showTabs) {
                AppRail(nav = navController, tabs = tabs)
            }
            Box(modifier = Modifier.fillMaxSize()) {
                AppNavHost(nav = navController, onLogout = onLogout)
            }
        }
    }
}

@Composable
private fun BottomNavBar(nav: NavHostController, tabs: List<AppRoute>) {
    val currentBackStack by nav.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination

    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentDestination.isTabRoute(tab.route),
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = tab.label,
                    )
                },
                label = { androidx.compose.material3.Text(text = tab.label) },
            )
        }
    }
}

@Composable
private fun AppRail(nav: NavHostController, tabs: List<AppRoute>) {
    val currentBackStack by nav.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination

    NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        tabs.forEach { tab ->
            NavigationRailItem(
                selected = currentDestination.isTabRoute(tab.route),
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = tab.label,
                    )
                },
                label = { androidx.compose.material3.Text(text = tab.label) },
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
        composable(AppRoute.Tokens.route) { TokensScreen(onBack = { nav.popBackStack() }) }
        composable(AppRoute.Thread.route) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id").orEmpty()
            ThreadScreen(conversationId = id, onBack = { nav.popBackStack() })
        }
    }
}

private fun AppRoute.icon() = when (this) {
    AppRoute.Inbox -> Icons.AutoMirrored.Filled.Chat
    AppRoute.Numbers -> Icons.Default.SimCard
    AppRoute.Automations -> Icons.Default.Tune
    AppRoute.Settings -> Icons.Default.Settings
    else -> Icons.Default.Settings
}

private fun NavDestination?.isTabRoute(route: String): Boolean {
    if (this == null) return false
    return hierarchy.any { destination -> destination.route == route }
}
