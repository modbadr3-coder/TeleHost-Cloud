package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TeleHostDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBackground,
    secondary = EmeraldLive,
    onSecondary = ObsidianBackground,
    tertiary = AmberBuild,
    background = ObsidianBackground,
    onBackground = CrispWhite,
    surface = ElevatedCarbonSurface,
    onSurface = CrispWhite,
    surfaceVariant = SlateBorder,
    onSurfaceVariant = MutedSteel,
    error = CrimsonError,
    onError = CrispWhite
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TeleHostDarkColorScheme,
        typography = Typography,
        content = content
    )
}
