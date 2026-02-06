package com.example.myapplication.ui

sealed class AppScreen(val route: String, val label: String) {
    data object Status : AppScreen("status", "Status")
    data object Settings : AppScreen("settings", "Settings")
    data object Debug : AppScreen("debug", "Debug")
}
