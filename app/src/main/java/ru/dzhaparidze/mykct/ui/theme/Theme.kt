package ru.dzhaparidze.mykct.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Роли Material 3 используются по назначению, чтобы редизайн не разъезжался:
// primary — карточки пар, primaryContainer — шапка, tertiary — выбранный день,
// secondary — кольцо прогресса, surfaceVariant — невыбранные круги и разделители.
private val LightColors = lightColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = Lavender,
    onPrimaryContainer = Ink,
    secondary = Green,
    onSecondary = Color.White,
    tertiary = Orange,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = GreyFill,
    onSurfaceVariant = GreyText,
    outlineVariant = GreyFill,
)

private val DarkColors = darkColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = DarkLavender,
    onPrimaryContainer = Color.White,
    secondary = Green,
    onSecondary = Color.White,
    tertiary = Orange,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkGreyFill,
    onSurfaceVariant = DarkGreyText,
    outlineVariant = DarkGreyFill,
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
