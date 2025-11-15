package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    navController: NavController,
    viewModel: EmployeeViewModel = hiltViewModel()
) {
    val isAdmin by viewModel.isAdmin.collectAsState()
    val employeesList by viewModel.employees.collectAsState()
    val pendingRegistrationsList by viewModel.pendingRegistrations.collectAsState()
    val adminNotifications by viewModel.adminNotifications.collectAsState()

    // 🔹 Fetch notifications in real-time
    LaunchedEffect(Unit) {
        viewModel.fetchAdminNotifications()
    }

    val employeesCount = employeesList.size
    val pendingRegistrationsCount = pendingRegistrationsList.size

    // 🔹 Load admin data
    LaunchedEffect(isAdmin) {
        if (isAdmin) {
            viewModel.refreshEmployees()
            viewModel.refreshPendingRegistrations()
        }
    }

    Scaffold(
        topBar = {
            CommonTopAppBar(title = "Admin Panel", navController = navController)
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                if (isAdmin) {
                // notification for new user registerd
                    if (adminNotifications.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("🔔 Admin Alerts", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))

                                adminNotifications.take(3).forEach { notif ->
                                    val title = notif["title"] as? String ?: "New Notification"
                                    val body = notif["body"] as? String ?: "No message available"
                                    val timestamp = notif["timestamp"] as? Long

                                    Text("• $title", style = MaterialTheme.typography.bodyMedium)
                                    Text(body, style = MaterialTheme.typography.bodySmall)
                                    timestamp?.let {
                                        Text(
                                            "⏱ ${java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a").format(java.util.Date(it))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                }

                                if (adminNotifications.size > 3) {
                                    TextButton(onClick = { navController.navigate(Routes.SEND_NOTIFICATION) }) {
                                        Text("View All Notifications")
                                    }
                                }
                            }
                        }
                    }


                    // 🔹 Admin Actions
                    ButtonRow(
                        icon = Icons.Filled.People,
                        text = "Total Employees ($employeesCount)",
                        onClick = { navController.navigate(Routes.EMPLOYEE_STATS) }
                    )

                    ButtonRow(
                        icon = Icons.Filled.HourglassTop,
                        text = "Pending Approvals ($pendingRegistrationsCount)",
                        onClick = { navController.navigate(Routes.PENDING_APPROVALS) }
                    )

                    ButtonRow(
                        icon = Icons.Filled.Notifications,
                        text = "Send Notification",
                        onClick = { navController.navigate(Routes.SEND_NOTIFICATION) }
                    )

                    ButtonRow(
                        icon = Icons.Filled.UploadFile,
                        text = "Upload to Database",
                        onClick = { navController.navigate(Routes.UPLOAD_CSV) }
                    )

                    ButtonRow(
                        icon = Icons.Filled.Link,
                        text = "Add Useful Link",
                        onClick = { navController.navigate(Routes.ADD_USEFUL_LINK) }
                    )

                    ButtonRow(
                        icon = Icons.Filled.Description,
                        text = "Upload Document",
                        onClick = { navController.navigate("upload_document") }
                    )

                } else {
                    // 🔸 Non-admin users — fallback message + redirect
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "You do not have access to this page.\nRedirecting...",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 🔹 Redirect after showing message
                    LaunchedEffect(Unit) {
                        navController.navigate(Routes.EMPLOYEE_LIST) {
                            popUpTo(Routes.ADMIN_PANEL) { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ButtonRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }
}
