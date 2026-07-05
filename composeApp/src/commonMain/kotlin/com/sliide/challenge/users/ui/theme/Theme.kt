package com.sliide.challenge.users.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Material 3 theme, dark-mode aware.
 *
 * Android 12+ returns a wallpaper-derived dynamic scheme (Material You);
 * everywhere else (older Android, iOS) falls back to the brand palette so
 * both platforms always look deliberate. expect/actual keeps this decision
 * in platform code without leaking platform APIs into shared UI.
 */
@Composable
internal expect fun platformColorScheme(dark: Boolean): ColorScheme?

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = platformColorScheme(dark) ?: if (dark) BrandDark else BrandLight
    MaterialTheme(
        colorScheme = scheme,
        content = content,
    )
}

// -- Brand fallback palette (indigo/violet) ------------------------------

private val BrandLight = lightColorScheme(
    primary = Color(0xFF4B5CC4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDEE0FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF5B5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E1F9),
    onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Color(0xFF77536D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD7F1),
    onTertiaryContainer = Color(0xFF2D1228),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF767680),
    surfaceContainer = Color(0xFFEFEDF7),
    surfaceContainerHigh = Color(0xFFE9E7F1),
    surfaceContainerHighest = Color(0xFFE3E1EC),
)

private val BrandDark = darkColorScheme(
    primary = Color(0xFFBBC3FF),
    onPrimary = Color(0xFF1A2793),
    primaryContainer = Color(0xFF3343AB),
    onPrimaryContainer = Color(0xFFDEE0FF),
    secondary = Color(0xFFC4C5DD),
    onSecondary = Color(0xFF2D2F42),
    secondaryContainer = Color(0xFF434559),
    onSecondaryContainer = Color(0xFFE0E1F9),
    tertiary = Color(0xFFE6BAD7),
    onTertiary = Color(0xFF44263D),
    tertiaryContainer = Color(0xFF5D3C55),
    onTertiaryContainer = Color(0xFFFFD7F1),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    outline = Color(0xFF90909A),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
)
