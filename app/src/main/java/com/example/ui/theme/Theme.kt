package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkTealPrimary,
    onPrimary = OnDarkTealPrimary,
    primaryContainer = DarkNavyContainer,
    onPrimaryContainer = OnDarkNavyContainer,
    secondary = DarkSecondary,
    onSecondary = OnDarkSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = OnDarkSecondaryContainer,
    tertiary = DarkCoralTertiary,
    onTertiary = OnDarkCoral,
    tertiaryContainer = DarkCoralContainer,
    onTertiaryContainer = OnDarkCoralContainer,
    background = DarkBackground,
    onBackground = OnDarkSurface,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = DeepNavyPrimary,
    onPrimary = OnDeepNavy,
    primaryContainer = NavyPrimaryContainer,
    onPrimaryContainer = OnNavyPrimaryContainer,
    secondary = ClinicalTealSecondary,
    onSecondary = OnClinicalTeal,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = OnTealSecondaryContainer,
    tertiary = WarmCoralTertiary,
    onTertiary = OnWarmCoral,
    tertiaryContainer = CoralTertiaryContainer,
    onTertiaryContainer = OnCoralTertiaryContainer,
    background = LightBackground,
    onBackground = OnLightSurface,
    surface = LightSurface,
    onSurface = OnLightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnLightSurfaceVariant,
    outline = LightOutline
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
