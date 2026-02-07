package com.example.myapplication.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFF2B6EF2)
private val LightBackground = Color(0xFFF8FAFC)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF111827)
private val LightOnSurfaceVariant = Color(0xFF4B5563)
private val DarkBackground = Color(0xFF0A0F1A)
private val DarkSurface = Color(0xFF111827)
private val DarkOnSurface = Color(0xFFE5E7EB)
private val DarkOnSurfaceVariant = Color(0xFF9CA3AF)

val LightAppColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF001848),

    secondary = Color(0xFF3F4C67),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE2F2),
    onSecondaryContainer = Color(0xFF0F1B32),

    tertiary = Accent,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCE7FF),
    onTertiaryContainer = Color(0xFF001848),

    background = LightBackground,
    onBackground = LightOnSurface,

    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFE5E7EB),
    onSurfaceVariant = LightOnSurfaceVariant,

    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

val DarkAppColorScheme = darkColorScheme(
    primary = Color(0xFFB3CAFF),
    onPrimary = Color(0xFF022A78),
    primaryContainer = Color(0xFF1F4FB7),
    onPrimaryContainer = Color(0xFFDCE7FF),

    secondary = Color(0xFFBEC7DC),
    onSecondary = Color(0xFF283247),
    secondaryContainer = Color(0xFF3A455A),
    onSecondaryContainer = Color(0xFFDAE2F6),

    tertiary = Color(0xFFB3CAFF),
    onTertiary = Color(0xFF022A78),
    tertiaryContainer = Color(0xFF1F4FB7),
    onTertiaryContainer = Color(0xFFDCE7FF),

    background = DarkBackground,
    onBackground = DarkOnSurface,

    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF283146),
    onSurfaceVariant = DarkOnSurfaceVariant,

    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)
