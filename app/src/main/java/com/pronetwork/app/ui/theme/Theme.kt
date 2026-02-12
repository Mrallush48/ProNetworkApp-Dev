package com.pronetwork.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SpotPrimary,
    onPrimary = SpotOnPrimary,
    primaryContainer = SpotPrimaryContainer,
    onPrimaryContainer = SpotOnPrimaryContainer,

    secondary = SpotSecondary,
    onSecondary = SpotOnSecondary,
    secondaryContainer = SpotSecondaryContainer,
    onSecondaryContainer = SpotOnSecondaryContainer,

    tertiary = SpotTertiary,
    onTertiary = SpotOnTertiary,
    tertiaryContainer = SpotTertiaryContainer,
    onTertiaryContainer = SpotOnTertiaryContainer,

    error = SpotError,
    onError = SpotOnError,
    errorContainer = SpotErrorContainer,
    onErrorContainer = SpotOnErrorContainer,

    background = SpotBackground,
    onBackground = SpotOnBackground,
    surface = SpotSurface,
    onSurface = SpotOnSurface,
    surfaceVariant = SpotSurfaceVariant,
    onSurfaceVariant = SpotOnSurfaceVariant,

    outline = SpotOutline,
    outlineVariant = SpotOutlineVariant,
    scrim = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    primary = SpotPrimaryDark,
    onPrimary = SpotOnPrimaryDark,
    primaryContainer = SpotPrimaryContainerDark,
    onPrimaryContainer = SpotOnPrimaryContainerDark,

    secondary = SpotSecondaryDark,
    onSecondary = SpotOnSecondaryDark,
    secondaryContainer = SpotSecondaryContainerDark,
    onSecondaryContainer = SpotOnSecondaryContainerDark,

    tertiary = SpotTertiaryDark,
    onTertiary = SpotOnTertiaryDark,
    tertiaryContainer = SpotTertiaryContainerDark,
    onTertiaryContainer = SpotOnTertiaryContainerDark,

    error = SpotErrorDark,
    onError = SpotOnErrorDark,
    errorContainer = SpotErrorContainerDark,
    onErrorContainer = SpotOnErrorContainerDark,

    background = SpotBackgroundDark,
    onBackground = SpotOnBackgroundDark,
    surface = SpotSurfaceDark,
    onSurface = SpotOnSurfaceDark,
    surfaceVariant = SpotSurfaceVariantDark,
    onSurfaceVariant = SpotOnSurfaceVariantDark,

    outline = SpotOutlineDark,
    outlineVariant = SpotOutlineVariantDark,
    scrim = Color.Black
)

@Composable
fun ProNetworkSpotTheme(
    darkTheme: Boolean = false,
    colorSchemeOverride: ColorScheme? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = colorSchemeOverride ?: if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpotTypography, // أو AppTypography لو غيّرت الاسم
        content = content
    )
}
