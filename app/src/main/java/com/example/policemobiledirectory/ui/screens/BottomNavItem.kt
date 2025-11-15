package com.example.policemobiledirectory.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.policemobiledirectory.navigation.Routes

/**
 * Data class for bottom navigation items.
 *
 * @param route Used for navigation (matches Routes constants or special actions).
 * @param icon  The icon displayed in the bottom bar.
 * @param label The text label under the icon.
 */
data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

/**
 * Global bottom navigation items.
 *
 * Order:
 * - Menu (opens Navigation Drawer)
 * - Home
 * - Documents
 * - Useful Links
 */
val bottomNavItems = listOf(
    BottomNavItem(
        route = "drawer_menu", // special action (opens Navigation Drawer)
        icon = Icons.Default.Menu,
        label = "Menu"
    ),
    BottomNavItem(
        route = Routes.GALLERY_SCREEN, // 🖼️ New Gallery Screen
        icon = Icons.Default.PhotoLibrary, // You can replace with your custom drawable later
        label = "Gallery"
    ),
    BottomNavItem(
        route = Routes.EMPLOYEE_LIST,
        icon = Icons.Default.Home,
        label = "Home"
    ),
    BottomNavItem(
        route = Routes.DOCUMENTS,
        icon = Icons.Default.Description,
        label = "Docs"
    ),
    BottomNavItem(
        route = Routes.USEFUL_LINKS,
        icon = Icons.Default.Link,
        label = "Links"
    )
)
