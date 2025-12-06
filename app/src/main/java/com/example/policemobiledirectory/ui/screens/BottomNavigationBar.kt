package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.policemobiledirectory.ui.theme.BottomNavStart
import com.example.policemobiledirectory.ui.theme.BottomNavEnd
import com.example.policemobiledirectory.ui.theme.CardShadow
import com.example.policemobiledirectory.ui.theme.BackgroundLight
import com.example.policemobiledirectory.ui.theme.TextPrimary
import com.example.policemobiledirectory.ui.theme.TextSecondary
import com.example.policemobiledirectory.ui.theme.PrimaryTeal
import com.example.policemobiledirectory.ui.theme.SecondaryYellow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Modern Material3 Bottom Navigation Bar with gradient and glowing active icon.
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    drawerState: DrawerState? = null,
    scope: CoroutineScope? = null
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                spotColor = CardShadow,
                ambientColor = CardShadow.copy(alpha = 0.5f)
            )
    ) {
        NavigationBar(
            containerColor = BackgroundLight, // Light off-white background
            contentColor = TextPrimary,
            modifier = Modifier
        ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                icon = { 
                    Box {
                        Icon(
                            item.icon, 
                            contentDescription = item.label,
                            modifier = Modifier.shadow(
                                elevation = if (isSelected) 8.dp else 0.dp,
                                shape = androidx.compose.foundation.shape.CircleShape,
                                spotColor = if (isSelected) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                                ambientColor = if (isSelected) Color.White.copy(alpha = 0.3f) else Color.Transparent
                            )
                        )
                    }
                },
                label = { Text(item.label) },
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryTeal, // Teal for active icon
                    unselectedIconColor = TextSecondary, // Light grey for inactive
                    selectedTextColor = PrimaryTeal,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = SecondaryYellow.copy(alpha = 0.3f) // Yellow highlight for active
                ),
                onClick = {
                    when (item.route) {
                        // ☰ Drawer Menu special action
                        "drawer_menu" -> {
                            if (drawerState != null && scope != null) {
                                scope.launch { drawerState.open() }
                            }
                        }

                        // Normal navigation
                        else -> {
                            if (!isSelected) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                }
            )
        }
        }
    }
}
