package com.example.policemobiledirectory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

// Light color scheme with BlueGreyBackground
private val LightColors = lightColorScheme(
    primary = PurplePrimary, // Purple for actions
    onPrimary = OnImagePrimary,
    primaryContainer = PurplePrimaryDark,
    onPrimaryContainer = Color(0xFF1B1B1B), // ✅ readable dark text

    secondary = TealSecondary,
    onSecondary = OnImageSecondary,
    secondaryContainer = TealSecondaryDark,
    onSecondaryContainer = OnImagePrimary,

    tertiary = TealSecondaryDark,
    onTertiary = OnImagePrimary,
    tertiaryContainer = Color(0xFFACEAE5),
    onTertiaryContainer = OnImageBackground,

    error = ImageError,
    onError = OnImageError,
    errorContainer = Color(0xFFFCD8DF),
    onErrorContainer = OnImageBackground,

    background = BlueGreyBackground, // Blue Grey Background #CFD8DC
    onBackground = OnImageBackground,

    surface = Color.White,
    onSurface = OnImageBackground,

    surfaceVariant = Color(0xFFF8F8F8), // ✅ new softer background for drawers
    onSurfaceVariant = OnImageBackground,

    outline = Color(0xFFB0BEC5)
)

// Dark color scheme
private val DarkColors = darkColorScheme(
    primary = AppPrimaryVariant,
    onPrimary = OnAppPrimary,
    primaryContainer = AppPrimary,
    onPrimaryContainer = Color(0xFFE0E0E0), // ✅ light text for dark backgrounds

    secondary = AppSecondary,
    onSecondary = OnImageSecondary,
    secondaryContainer = AppSecondaryVariant,
    onSecondaryContainer = OnAppPrimary,

    tertiary = AppAccent,
    onTertiary = OnAppAccent,
    tertiaryContainer = Color(0xFF7A2F17),
    onTertiaryContainer = OnAppAccent,

    error = Color(0xFFCF6679),
    onError = Color.Black,
    errorContainer = Color(0xFFB00020),
    onErrorContainer = Color.White,

    background = AppDarkBackground,
    onBackground = TextOnDark,
    surface = Color(0xFF1E1E1E),
    onSurface = TextOnDark,

    surfaceVariant = Color(0xFF2C2C2E),  // ✅ soft dark tone for drawer
    onSurfaceVariant = TextOnDark,

    outline = Color(0xFF8E8E93)
)


@Composable
fun PMDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = !darkTheme

    // ✅ Apply matching system bar colors for all screens
    SideEffect {
        // Match status bar to primary color
        systemUiController.setStatusBarColor(
            color = colors.primary,
            darkIcons = false // Keep icons white for purple top bar
        )
        // Optionally, set navigation bar to background or primary
        systemUiController.setNavigationBarColor(
            color = colors.background,
            darkIcons = useDarkIcons
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
