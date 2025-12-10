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
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    // Profile Image with Blood Group in top right corner
                    Box(
                        modifier = Modifier.size(90.dp),
                        contentAlignment = Alignment.TopEnd
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
                        
                        // Blood Group badge in top right corner
                        currentUser?.bloodGroup?.takeIf { it.isNotBlank() }?.let { bg ->
                            // Format blood group: show "??" as-is, otherwise format normally
                            val formattedBg = if (bg.trim() == "??") {
                                "??"
                            } else {
                                bg.uppercase()
                                    .replace("POSITIVE", "+")
                                    .replace("NEGATIVE", "–")
                                    .replace("VE", "")
                                    .replace("(", "")
                                    .replace(")", "")
                                    .trim()
                                    .let { clean ->
                                        when (clean) {
                                            "A" -> "A+"
                                            "B" -> "B+"
                                            "O" -> "O+"
                                            "AB" -> "AB+"
                                            "A-" -> "A–"
                                            "B-" -> "B–"
                                            "O-" -> "O–"
                                            "AB-" -> "AB–"
                                            else -> clean
                                        }
                                    }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(28.dp)
                                    .offset(x = 4.dp, y = (-4).dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = formattedBg,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Name (bold, prominent) + rank (smaller, no brackets)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentUser?.name ?: "",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        currentUser?.displayRank?.takeIf { it.isNotBlank() }?.let { rank ->
                            Text(
                                text = rank,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                    
                    // KGID (larger)
                    Text(
                        text = currentUser?.kgid?.let { "KGID: $it" } ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 16.sp
                        )
                    )
                    
                    // Station (larger)
                    currentUser?.station?.takeIf { it.isNotBlank() }?.let { station ->
                        Text(
                            text = station,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 16.sp
                            )
                        )
                    }
                    
                    // Email (larger)
                    Text(
                        text = currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp
                        )
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
                            navController.navigate(Routes.MY_PROFILE) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(Routes.EMPLOYEE_LIST) { inclusive = false }
                            }
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
                    icon = Icons.Default.Translate,
                    text = "Nudi Converter",
                    selected = currentRoute == Routes.NUDI_CONVERTER,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.NUDI_CONVERTER) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(Routes.EMPLOYEE_LIST) { inclusive = false }
                            }
                        }
                    }
                )

                DrawerItem(
                    icon = Icons.Default.Info,
                    text = "About App",
                    selected = currentRoute == Routes.ABOUT,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.ABOUT) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(Routes.EMPLOYEE_LIST) { inclusive = false }
                            }
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
                var showSupportDialog by remember { mutableStateOf(false) }
                val clipboardManager = LocalClipboardManager.current

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
                                    Text("Logout", color = Color.Red)
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

                if (showSupportDialog) {
                    AlertDialog(
                        onDismissRequest = { showSupportDialog = false },
                        icon = { Icon(Icons.Default.Email, contentDescription = null) },
                        title = { Text("Contact Support") },
                        text = { Text("Email: noreply.policemobiledirectory@gmail.com\nWe usually respond quickly.") },
                        confirmButton = {
                            TextButton(onClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:noreply.policemobiledirectory@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "App Support Request")
                                }
                                try {
                                    context.startActivity(emailIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                                }
                                showSupportDialog = false
                            }) {
                                Text("Email")
                            }
                        },
                        dismissButton = {
                            Row {
                                TextButton(onClick = {
                                    clipboardManager.setText(AnnotatedString("noreply.policemobiledirectory@gmail.com"))
                                    Toast.makeText(context, "Email copied", Toast.LENGTH_SHORT).show()
                                }) { Text("Copy") }
                                Spacer(modifier = Modifier.width(4.dp))
                                TextButton(onClick = { showSupportDialog = false }) { Text("Close") }
                            }
                        }
                    )
                }

                DrawerItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    text = "Logout",
                    textColor = Color.Red,
                    onClick = { showLogoutDialog = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                DrawerItem(
                    icon = Icons.Default.Email,
                    text = "Contact Support",
                    onClick = { showSupportDialog = true }
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
    textColor: Color = MaterialTheme.colorScheme.onSurface,
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
                color = textColor
            )
        )
    }
}
