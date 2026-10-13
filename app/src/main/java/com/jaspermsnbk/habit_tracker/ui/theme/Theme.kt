package com.jaspermsnbk.habit_tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/*
 * The app's own colours come from its pets. Primary is the periwinkle of Mochi, the first pet,
 * deepened so white text on it is readable. Secondary is the sunny yellow of Pip and tertiary the
 * mint of Shell, so a screen with a pet on it already matches the interface around it. Neutrals
 * lean slightly blue-violet rather than grey, so cards and backgrounds sit with the pets instead
 * of fighting them.
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFF4757C8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE3FF),
    onPrimaryContainer = Color(0xFF0A1A66),
    secondary = Color(0xFF735C00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE9A3),
    onSecondaryContainer = Color(0xFF241A00),
    tertiary = Color(0xFF1F7A5C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBDEFD3),
    onTertiaryContainer = Color(0xFF00210F),
    background = Color(0xFFFAF9FF),
    onBackground = Color(0xFF1B1B27),
    surface = Color(0xFFFAF9FF),
    onSurface = Color(0xFF1B1B27),
    surfaceVariant = Color(0xFFE4E3F2),
    onSurfaceVariant = Color(0xFF46465A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F3FC),
    surfaceContainer = Color(0xFFEFEEF8),
    surfaceContainerHigh = Color(0xFFE9E8F3),
    surfaceContainerHighest = Color(0xFFE3E2ED),
    outline = Color(0xFF77768C),
    outlineVariant = Color(0xFFC7C6DC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7C3FF),
    onPrimary = Color(0xFF14226E),
    primaryContainer = Color(0xFF2F3FA6),
    onPrimaryContainer = Color(0xFFDDE3FF),
    secondary = Color(0xFFF2C94C),
    onSecondary = Color(0xFF3A2C00),
    secondaryContainer = Color(0xFF564400),
    onSecondaryContainer = Color(0xFFFFE9A3),
    tertiary = Color(0xFF8ED6A8),
    onTertiary = Color(0xFF00391F),
    tertiaryContainer = Color(0xFF00522F),
    onTertiaryContainer = Color(0xFFBDEFD3),
    background = Color(0xFF121420),
    onBackground = Color(0xFFE4E1F0),
    surface = Color(0xFF121420),
    onSurface = Color(0xFFE4E1F0),
    surfaceVariant = Color(0xFF2E3042),
    onSurfaceVariant = Color(0xFFC7C6DC),
    surfaceContainerLowest = Color(0xFF0D0F1A),
    surfaceContainerLow = Color(0xFF1A1C28),
    surfaceContainer = Color(0xFF1E2030),
    surfaceContainerHigh = Color(0xFF292B3B),
    surfaceContainerHighest = Color(0xFF343646),
    outline = Color(0xFF918FA6),
    outlineVariant = Color(0xFF46465A),
)

@Composable
fun HabitTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the app looks like itself. Turning it on derives a palette from the wallpaper
    // (always available at minSdk 33).
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
