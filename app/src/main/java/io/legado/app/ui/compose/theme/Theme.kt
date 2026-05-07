package io.legado.app.ui.compose.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.legado.app.constant.Theme
import io.legado.app.help.config.AppConfig

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    primaryContainer = LightPrimaryDark,
    secondary = LightAccent,
    secondaryContainer = LightAccent.copy(alpha = 0.12f),
    background = LightBackground,
    surface = LightSurface,
    error = Error,
    onPrimary = LightOnPrimary,
    onSecondary = LightOnSecondary,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    primaryContainer = DarkPrimaryDark,
    secondary = DarkAccent,
    secondaryContainer = DarkAccent.copy(alpha = 0.12f),
    background = DarkBackground,
    surface = DarkSurface,
    error = Error,
    onPrimary = DarkOnPrimary,
    onSecondary = DarkOnSecondary,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onError = Color.Black
)

@Composable
fun LegadoTheme(
    theme: Theme = Theme.Auto,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val actualTheme = when (theme) {
        Theme.Dark -> Theme.Dark
        Theme.Light -> Theme.Light
        else -> if (isSystemInDarkTheme()) Theme.Dark else Theme.Light
    }

    val colorScheme = when (actualTheme) {
        Theme.Dark -> DarkColorScheme
        Theme.Light -> LightColorScheme
        else -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LegadoTypography,
        content = content
    )
}

@Composable
fun getThemeColorScheme(): androidx.compose.material3.ColorScheme {
    return MaterialTheme.colorScheme
}
