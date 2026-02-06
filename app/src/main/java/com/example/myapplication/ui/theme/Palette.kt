package com.example.myapplication.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Calm neutrals + single accent.
 * These are only used when dynamic color is unavailable/disabled.
 */
private val Accent = Color(0xFF2AAFA4) // teal

val LightAppColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7EFE8),
    onPrimaryContainer = Color(0xFF00201D),

    secondary = Color(0xFF4B635F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE8E3),
    onSecondaryContainer = Color(0xFF06201D),

    background = Color(0xFFFBFCFC),
    onBackground = Color(0xFF0F1414),

    surface = Color(0xFFFBFCFC),
    onSurface = Color(0xFF0F1414),
    surfaceVariant = Color(0xFFE7ECEB),
    onSurfaceVariant = Color(0xFF3F4947),

    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFC3C8C6),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

val DarkAppColorScheme = darkColorScheme(
    primary = Color(0xFF7BD8CD),
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF005049),
    onPrimaryContainer = Color(0xFF9BF4E8),

    secondary = Color(0xFFB2CCC7),
    onSecondary = Color(0xFF1D3531),
    secondaryContainer = Color(0xFF334B47),
    onSecondaryContainer = Color(0xFFCDE8E3),

    background = Color(0xFF0B1111),
    onBackground = Color(0xFFEAF1EF),

    surface = Color(0xFF0B1111),
    onSurface = Color(0xFFEAF1EF),
    surfaceVariant = Color(0xFF3F4947),
    onSurfaceVariant = Color(0xFFC3C8C6),

    outline = Color(0xFF8D9593),
    outlineVariant = Color(0xFF3F4947),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)
