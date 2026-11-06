package com.example.blooddonorfinder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BloodRed,
    onPrimary = PureWhite,
    primaryContainer = BloodRedContainer,
    onPrimaryContainer = BloodRedOnContainer,
    secondary = BloodRedLight,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFFFCDD2),
    onSecondaryContainer = Color(0xFF5C0000),
    tertiary = BloodRedDark,
    onTertiary = PureWhite,
    background = OffWhite,
    onBackground = TextPrimary,
    surface = PureWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceTint,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    outlineVariant = DividerColor
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFEF5350),
    onPrimary = Color(0xFF2B0000),
    primaryContainer = Color(0xFF5F0909),
    onPrimaryContainer = Color(0xFFFFEBEE),
    secondary = Color(0xFFE57373),
    onSecondary = Color(0xFF2B0000),
    secondaryContainer = Color(0xFF491010),
    onSecondaryContainer = Color(0xFFFFCDD2),
    tertiary = Color(0xFFFF8A80),
    onTertiary = Color(0xFF2B0000),
    background = Color(0xFF141212),
    onBackground = Color(0xFFF0EAE9),
    surface = Color(0xFF1C1919),
    onSurface = Color(0xFFF0EAE9),
    surfaceVariant = Color(0xFF2B2525),
    onSurfaceVariant = Color(0xFFD6C2C1),
    outline = Color(0xFF6B5858),
    outlineVariant = Color(0xFF382F2F)
)

@Composable
fun BloodDonorFinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamicColor to false to maintain the red-and-white blood donation branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}