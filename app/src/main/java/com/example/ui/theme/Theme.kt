package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DeepSlatePrimary,
    secondary = DeepSlateSecondary,
    tertiary = DeepSlateTertiary,
    background = DeepSlateBackground,
    surface = DeepSlateSurface,
    onPrimary = DeepSlateOnPrimary,
    onBackground = DeepSlateOnBackground,
    onSurface = DeepSlateOnSurface,
    surfaceVariant = DeepSlateSurface
)

private val LightColorScheme = lightColorScheme(
    primary = SlatePrimary,
    secondary = SlateSecondary,
    tertiary = SlateTertiary,
    background = SlateBackground,
    surface = SlateSurface,
    onPrimary = SlateOnPrimary,
    onBackground = SlateOnBackground,
    onSurface = SlateOnSurface,
    surfaceVariant = SlateSurface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors by default to enforce our premium slate aesthetic
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
