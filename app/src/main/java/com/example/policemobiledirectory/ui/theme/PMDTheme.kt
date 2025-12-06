package com.example.policemobiledirectory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

// Light color scheme - Social Network Theme (Teal & Yellow)
private val LightColors = lightColorScheme(
    primary = PrimaryTeal, // Teal for primary actions
    onPrimary = TextOnTeal,
    primaryContainer = PrimaryTealLight,
    onPrimaryContainer = TextPrimary,

    secondary = SecondaryYellow, // Yellow for secondary actions
    onSecondary = TextOnYellow,
    secondaryContainer = SecondaryYellowLight,
    onSecondaryContainer = TextPrimary,

    tertiary = PrimaryTealDark,
    onTertiary = TextOnTeal,
    tertiaryContainer = PrimaryTealLight,
    onTertiaryContainer = TextPrimary,

    error = ErrorRed,
    onError = TextOnTeal,
    errorContainer = Color(0xFFFCD8DF),
    onErrorContainer = TextPrimary,

    background = BackgroundLight, // Light off-white background
    onBackground = TextPrimary,

    surface = BackgroundWhite, // White for cards
    onSurface = TextPrimary,

    surfaceVariant = BackgroundCard, // White card background
    onSurfaceVariant = TextSecondary,

    outline = BorderLight
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
