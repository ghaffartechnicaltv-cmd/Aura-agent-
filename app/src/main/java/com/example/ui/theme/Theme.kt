package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AuraDarkColorScheme = darkColorScheme(
    primary = AuraCyanPrimary,
    onPrimary = Color(0xFF001F28),
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = AuraVioletSecondary,
    onSecondary = Color(0xFF28004F),
    secondaryContainer = Color(0xFF4A127A),
    onSecondaryContainer = Color(0xFFEEDBFF),
    tertiary = AuraSuccessEmerald,
    onTertiary = Color(0xFF003822),
    background = AuraDarkBackground,
    onBackground = AuraTextPrimary,
    surface = AuraSurfaceCard,
    onSurface = AuraTextPrimary,
    surfaceVariant = AuraSurfaceCardElevated,
    onSurfaceVariant = AuraTextSecondary,
    outline = AuraSurfaceBorder,
    error = AuraDangerCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // AURA is sleek futuristic dark theme by default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
        }
        else -> AuraDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
