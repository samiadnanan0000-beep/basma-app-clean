package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = OneUiDarkPrimary,
    onPrimary = OneUiDarkOnPrimary,
    primaryContainer = OneUiDarkSurfaceVariant,
    onPrimaryContainer = OneUiDarkPrimary,
    secondary = OneUiDarkSecondary,
    tertiary = OneUiDarkTertiary,
    background = OneUiDarkBackground,
    surface = OneUiDarkSurface,
    surfaceVariant = OneUiDarkSurfaceVariant,
    onBackground = OneUiDarkOnSurface,
    onSurface = OneUiDarkOnSurface,
    outline = OneUiDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = OneUiLightPrimary,
    onPrimary = OneUiLightOnPrimary,
    primaryContainer = OneUiLightSurfaceVariant,
    onPrimaryContainer = OneUiLightPrimary,
    secondary = OneUiLightSecondary,
    tertiary = OneUiLightTertiary,
    background = OneUiLightBackground,
    surface = OneUiLightSurface,
    surfaceVariant = OneUiLightSurfaceVariant,
    onBackground = OneUiLightOnSurface,
    onSurface = OneUiLightOnSurface,
    outline = OneUiLightOutline
)

// Samsung One UI Squircle Shapes
val OneUiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp)
)

@Composable
fun BasmaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve bespoke One UI look
    content: @Composable () -> Unit
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
        shapes = OneUiShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = BasmaTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
