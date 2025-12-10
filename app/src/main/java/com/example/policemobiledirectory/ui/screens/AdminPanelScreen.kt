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
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import com.example.policemobiledirectory.utils.OperationStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    navController: NavController,
    viewModel: EmployeeViewModel = hiltViewModel(),
    constantsViewModel: ConstantsViewModel = hiltViewModel()
) {
    val isAdmin by viewModel.isAdmin.collectAsState()
    val employeesList by viewModel.employees.collectAsState()
    val pendingRegistrationsList by viewModel.pendingRegistrations.collectAsState()
    val firestoreToSheetStatus by viewModel.firestoreToSheetStatus.collectAsState()
    val sheetToFirestoreStatus by viewModel.sheetToFirestoreStatus.collectAsState()
    val officersSyncStatus by viewModel.officersSyncStatus.collectAsState()
    val constantsRefreshStatus by constantsViewModel.refreshStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Handle back button to navigate to home screen
    BackHandler {
        // Navigate to home screen, clearing back stack up to (but not including) EMPLOYEE_LIST
        navController.navigate(Routes.EMPLOYEE_LIST) {
            popUpTo(Routes.EMPLOYEE_LIST) { inclusive = false }
            launchSingleTop = true
        }
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

    LaunchedEffect(firestoreToSheetStatus) {
        when (val status = firestoreToSheetStatus) {
            is OperationStatus.Success -> {
                coroutineScope.launch { snackbarHostState.showSnackbar(status.data) }
                viewModel.resetFirestoreToSheetStatus()
            }
            is OperationStatus.Error -> {
                coroutineScope.launch { snackbarHostState.showSnackbar(status.message) }
                viewModel.resetFirestoreToSheetStatus()
            }
            else -> Unit
        }
    }

    LaunchedEffect(sheetToFirestoreStatus) {
        when (val status = sheetToFirestoreStatus) {
            is OperationStatus.Success -> {
                coroutineScope.launch { snackbarHostState.showSnackbar(status.data) }
                viewModel.resetSheetToFirestoreStatus()
            }
            is OperationStatus.Error -> {
                coroutineScope.launch { snackbarHostState.showSnackbar(status.message) }
                viewModel.resetSheetToFirestoreStatus()
            }
            else -> Unit
        }
    }
    
    LaunchedEffect(officersSyncStatus) {
        when (val status = officersSyncStatus) {
            is OperationStatus.Success -> {
                coroutineScope.launch { snackbarHostState.showSnackbar(status.data) }
                viewModel.resetOfficersSyncStatus()
            }
            is OperationStatus.Error -> {
                coroutineScope.launch { snackbarHostState.showSnackbar(status.message) }
                viewModel.resetOfficersSyncStatus()
            }
            else -> Unit
        }
    }

    // 🔹 Handle constants refresh status
    LaunchedEffect(constantsRefreshStatus) {
        when (val status = constantsRefreshStatus) {
            is OperationStatus.Success -> {
                coroutineScope.launch { snackbarHostState.showSnackbar(status.data) }
                constantsViewModel.resetRefreshStatus()
            }
            is OperationStatus.Error -> {
                coroutineScope.launch { snackbarHostState.showSnackbar(status.message) }
                constantsViewModel.resetRefreshStatus()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            CommonTopAppBar(title = "Admin Panel", navController = navController)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                        onClick = { navController.navigate(Routes.UPLOAD_DOCUMENT) }
                    )

                    ButtonRow(
                        icon = Icons.Filled.CloudDownload,
                        text = "Sync Employees Firestore → Sheet",
                        enabled = firestoreToSheetStatus !is OperationStatus.Loading,
                        isLoading = firestoreToSheetStatus is OperationStatus.Loading,
                        onClick = { viewModel.syncFirebaseToSheet() }
                    )

                    ButtonRow(
                        icon = Icons.Filled.CloudUpload,
                        text = "Sync Employees Sheet → Firestore",
                        enabled = sheetToFirestoreStatus !is OperationStatus.Loading,
                        isLoading = sheetToFirestoreStatus is OperationStatus.Loading,
                        onClick = { viewModel.syncSheetToFirebase() }
                    )
                    
                    ButtonRow(
                        icon = Icons.Filled.Person,
                        text = "Sync Officers Sheet → Firestore",
                        enabled = officersSyncStatus !is OperationStatus.Loading,
                        isLoading = officersSyncStatus is OperationStatus.Loading,
                        onClick = { viewModel.syncOfficersSheetToFirebase() }
                    )

                    ButtonRow(
                        icon = Icons.Filled.Refresh,
                        text = "Refresh Constants (Clear Cache)",
                        enabled = constantsRefreshStatus !is OperationStatus.Loading,
                        isLoading = constantsRefreshStatus is OperationStatus.Loading,
                        onClick = { constantsViewModel.clearCacheAndRefresh() }
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
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = enabled
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(12.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }
}
