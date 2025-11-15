package com.example.policemobiledirectory.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NavigationDrawer(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    viewModel: EmployeeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val currentRoute = navController.currentDestination?.route

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
        ) {

            // ============================================================
            // 🔹 TOP SECTION: PROFILE CARD
            // ============================================================
            Surface(
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 26.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(currentUser?.photoUrl)
                            .placeholder(R.drawable.officer)
                            .error(R.drawable.officer)
                            .crossfade(true)
                            .scale(Scale.FILL)
                            .build()
                    )

                    Image(
                        painter = painter,
                        contentDescription = "Profile photo",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = currentUser?.name ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = currentUser?.kgid?.let { "KGID: $it" } ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f))
                    )
                    Text(
                        text = currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                    )
                }
            }

            Divider(thickness = 1.dp)

            // ============================================================
            // 🔹 MIDDLE SECTION: MENU ITEMS
            // ============================================================
            Column(
                Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                DrawerItem(
                    icon = Icons.Default.Person,
                    text = "My Profile",
                    selected = currentRoute == Routes.MY_PROFILE,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.MY_PROFILE)
                        }
                    }
                )

                if (isAdmin) {
                    DrawerItem(
                        icon = Icons.Default.AdminPanelSettings,
                        text = "Admin Panel",
                        selected = currentRoute == Routes.ADMIN_PANEL,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                delay(250)
                                val current = navController.currentDestination?.route
                                if (current != Routes.ADMIN_PANEL) {
                                    navController.navigate(Routes.ADMIN_PANEL) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(Routes.EMPLOYEE_LIST) { inclusive = false }
                                    }
                                }
                            }
                        }
                    )
                }

                DrawerItem(
                    icon = Icons.Default.Info,
                    text = "About App",
                    selected = currentRoute == Routes.ABOUT,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.ABOUT)
                        }
                    }
                )
            }

            Divider(thickness = 1.dp)

            // ============================================================
            // 🔹 BOTTOM SECTION: LOGOUT + CONTACT
            // ============================================================
            Column(modifier = Modifier.padding(16.dp)) {
                var showLogoutDialog by remember { mutableStateOf(false) }
                var isLoggingOut by remember { mutableStateOf(false) }

                if (showLogoutDialog) {
                    AlertDialog(
                        onDismissRequest = { if (!isLoggingOut) showLogoutDialog = false },
                        confirmButton = {
                            TextButton(
                                enabled = !isLoggingOut,
                                onClick = {
                                    isLoggingOut = true
                                    scope.launch {
                                        drawerState.close()
                                        viewModel.logout {
                                            isLoggingOut = false
                                            showLogoutDialog = false
                                            navController.navigate(Routes.LOGIN) {
                                                popUpTo(0) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                }
                            ) {
                                if (isLoggingOut) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Logout")
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                enabled = !isLoggingOut,
                                onClick = { showLogoutDialog = false }
                            ) { Text("Cancel") }
                        },
                        icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                        title = { Text("Confirm Logout") },
                        text = { Text("Are you sure you want to log out?") }
                    )
                }

                DrawerItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    text = "Logout",
                    onClick = { showLogoutDialog = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                DrawerItem(
                    icon = Icons.Default.Email,
                    text = "Contact Support",
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:noreply.policemobiledirectory@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "App Support Request")
                        }
                        try {
                            context.startActivity(emailIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

// ============================================================
// 🔹 Reusable Drawer Item Composable
// ============================================================
@Composable
fun DrawerItem(
    icon: ImageVector,
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = if (selected)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else
        Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
