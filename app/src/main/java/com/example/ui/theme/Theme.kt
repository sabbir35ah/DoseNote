package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Color(0xFF003833),
    primaryContainer = Color(0xFF005049),
    onPrimaryContainer = Color(0xFF8CF8EC),
    secondary = TealGrey80,
    onSecondary = Color(0xFF223533),
    secondaryContainer = Color(0xFF384C49),
    onSecondaryContainer = Color(0xFFD4E8E5),
    tertiary = Amber80,
    onTertiary = Color(0xFF452B00),
    tertiaryContainer = Color(0xFF633F00),
    onTertiaryContainer = Color(0xFFFFDDB3),
    background = Color(0xFF101514),
    surface = Color(0xFF101514),
    onBackground = Color(0xFFE0E3E2),
    onSurface = Color(0xFFE0E3E2)
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    secondary = SlateSecondary,
    onSecondary = SlateOnSecondary,
    secondaryContainer = SlateSecondaryContainer,
    onSecondaryContainer = SlateOnSecondaryContainer,
    tertiary = WarmAmberTertiary,
    onTertiary = WarmAmberOnTertiary,
    tertiaryContainer = WarmAmberContainer,
    onTertiaryContainer = WarmAmberOnContainer,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = Color(0xFF161E1D),
    onSurface = Color(0xFF161E1D)
)

@Composable
fun DoseNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted calm teal palette for brand identity
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
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    DoseNoteTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
