package com.example.policemobiledirectory.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),   // Chips, textfields
    medium = RoundedCornerShape(16.dp), // Cards
    large = RoundedCornerShape(50)      // Buttons (pill shape)
)
