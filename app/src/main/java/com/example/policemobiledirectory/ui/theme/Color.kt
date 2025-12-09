package com.example.policemobiledirectory.ui.theme

import androidx.compose.ui.graphics.Color

// New Palette from Image
val PurplePrimary = Color(0xFF6200EE)
val PurplePrimaryDark = Color(0xFF3700B3) // Was PrimaryVariant in M2
val TealSecondary = Color(0xFF03DAC6)
val TealSecondaryDark = Color(0xFF018786) // Was SecondaryVariant in M2

val ImageBackground = Color.White // #FFFFFF (Original from image)
val BrightCyanBackground = Color(0xFF18FFFF) // Previous requested background
val VividCyanBackground = Color(0xFF00E5FF)   // Previous requested background
val SoftBlueBackground = Color(0xFF81D4FA)    // Previous requested background
val PaleBlueBackground = Color(0xFFBBDEFB)    // Previous requested background
val BlueGreyBackground = Color(0xFFCFD8DC)    // New requested background #CFD8DC

val ImageSurface = Color.White    // #FFFFFF
val ImageError = Color(0xFFB00020)

val OnImagePrimary = Color.White
val OnImageSecondary = Color.Black // #000000
val OnImageBackground = Color.Black // Original for white background, good for BlueGreyBackground too
val OnImageSurface = Color.Black    // Original for white surface
val OnImageError = Color.White

// Previous Palette (keeping for reference or if needed for dark theme variations)
val AppPrimary = Color(0xFF0D47A1)
val AppPrimaryVariant = Color(0xFF1565C0)
val AppSecondary = Color(0xFF00ACC1)
val AppSecondaryVariant = Color(0xFF00838F)
val AppAccent = Color(0xFFFF7043)
val AppLightBackground = Color(0xFFF9FAFB) // Previous light background
val AppDarkBackground = Color(0xFF121212)
val OnAppPrimary = Color.White
val OnAppAccent = Color.White
val TextOnLight = Color(0xFF1F1F1F) // General dark text for light backgrounds
val TextOnDark = Color(0xFFE6E6E6)  // General light text for dark backgrounds

// Colorful Employee Card Colors
val VibrantBlue = Color(0xFF2196F3)      // Bright blue
val VibrantGreen = Color(0xFF4CAF50)     // Bright green
val VibrantOrange = Color(0xFFFF9800)    // Bright orange
val VibrantPurple = Color(0xFF9C27B0)    // Bright purple
val VibrantRed = Color(0xFFF44336)       // Bright red
val VibrantTeal = Color(0xFF00BCD4)      // Bright teal
val VibrantPink = Color(0xFFE91E63)      // Bright pink
val VibrantIndigo = Color(0xFF3F51B5)    // Bright indigo
val VibrantLime = Color(0xFFCDDC39)      // Bright lime
val VibrantCyan = Color(0xFF00BCD4)      // Bright cyan

// Calm gradient colors for employee cards (from color palette)
// PRIMARY GRADIENT colors
val CalmSkyBlue = Color(0xFF8EC5FC)
val CalmSoftBlue = Color(0xFFA9C9FF)
val CalmPeriwinkle = Color(0xFFDAD4EC)

// ACCENT COLORS
val CalmRoyalBlue = Color(0xFF4A6CF7)
val CalmIndigo = Color(0xFF5B4EFA)
val CalmAquaTeal = Color(0xFF4BC0C8)
val CalmMint = Color(0xFFC7F5FF)

// POLICE THEME colors
val CalmNavyBlue = Color(0xFF1F2A6B)
val CalmRoyalBlueDark = Color(0xFF244B9E)
val CalmGold = Color(0xFFFFD700)

// ============================================
// SOCIAL NETWORK THEME COLOR PALETTE
// Based on provided design reference
// ============================================

// Primary Color - Teal/Turquoise (Dominant)
val PrimaryTeal = Color(0xFF00BCD4)        // Vibrant teal for top bars, buttons
val PrimaryTealDark = Color(0xFF0097A7)     // Darker teal for gradients
val PrimaryTealLight = Color(0xFF4DD0E1)    // Lighter teal for highlights

// Secondary Color - Soft Yellow/Gold
val SecondaryYellow = Color(0xFFFFC107)     // Soft yellow for secondary buttons
val SecondaryYellowDark = Color(0xFFFFA000) // Darker yellow
val SecondaryYellowLight = Color(0xFFFFEB3B) // Lighter yellow

// Background Colors
val BackgroundLight = Color(0xFFF5F5F5)     // Light off-white / pale grey-green background
val BackgroundWhite = Color(0xFFFFFFFF)   // Pure white for cards
val BackgroundCard = Color(0xFFFFFFFF)     // White card background

// Text Colors
val TextPrimary = Color(0xFF212121)        // Dark grey for primary text
val TextSecondary = Color(0xFF757575)       // Light grey for secondary/placeholder text
val TextOnTeal = Color(0xFFFFFFFF)          // White text on teal backgrounds
val TextOnYellow = Color(0xFFFFFFFF)        // White text on yellow backgrounds

// Border & Divider Colors
val BorderLight = Color(0xFFE0E0E0)         // Light grey borders
val BorderTeal = Color(0xFF00BCD4)         // Teal border for active/focused
val DividerColor = Color(0xFFBDBDBD)        // Divider color

// Status Colors
val SuccessGreen = Color(0xFF4CAF50)
val WarningYellow = Color(0xFFFFC107)
val ErrorRed = Color(0xFFE53935)

// Shadow Colors
val CardShadow = Color(0x22000000)          // #00000022 - soft shadow
val CardShadowLight = Color(0x11000000)     // Lighter shadow

// Glassmorphism (keeping for compatibility)
val GlassOpacity = 0.92f

// Border Colors (for compatibility)
val BorderLightPurple = BorderTeal          // Use teal instead of purple
val BorderChipUnselected = BorderLight

// Chip Colors
val ChipSelectedStart = PrimaryTeal
val ChipSelectedEnd = PrimaryTealLight
val ChipUnselected = BackgroundWhite

// Employee Card Background Color (Light Cyan/Teal)
// Single solid color for all employee cards (matching KGID 2810847)
val EmployeeCardBackground = Color(0xFF9DDCE7)  // Light cyan/teal #9DDCE7

// Employee Card Gradients (Teal theme) - Legacy, kept for compatibility
// Upper cards: Light teal gradient
val CardGradientUpperStart = Color(0xFFB2EBF2)  // Very light teal
val CardGradientUpperEnd = Color(0xFF80DEEA)    // Light teal

// Lower cards: Teal to cyan gradient
val CardGradientLowerStart = Color(0xFF4DD0E1)  // Light teal
val CardGradientLowerEnd = Color(0xFF26C6DA)    // Medium teal

// Gradient pairs for employee cards
val GradientStartBlue = CardGradientUpperStart
val GradientEndBlue = CardGradientUpperEnd
val GradientStartGreen = CardGradientLowerStart
val GradientEndGreen = CardGradientLowerEnd
val GradientStartPurple = CardGradientUpperStart
val GradientEndPurple = CardGradientUpperEnd
val GradientStartOrange = CardGradientLowerStart
val GradientEndOrange = CardGradientLowerEnd
val GradientStartTeal = CardGradientUpperStart
val GradientEndTeal = CardGradientUpperEnd

// Bottom Navigation
val BottomNavStart = PrimaryTeal
val BottomNavEnd = PrimaryTealDark

// FAB Color
val FABColor = PrimaryTeal

// Legacy compatibility (mapping to new colors)
val DeepRoyalPurple = PrimaryTeal
val BrightViolet = PrimaryTealLight
val AquaBlue = PrimaryTeal
val SoftPink = SecondaryYellowLight
// ErrorRed is already defined above in Status Colors section

// Card accent colors
val CardAccentGold = Color(0xFFFFD700)
val CardAccentSilver = Color(0xFFC0C0C0)
val CardAccentBronze = Color(0xFFCD7F32)