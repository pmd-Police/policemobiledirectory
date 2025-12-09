package com.example.policemobiledirectory.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.policemobiledirectory.R
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.policemobiledirectory.data.local.PendingRegistrationEntity
import com.example.policemobiledirectory.data.local.toEmployee
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import com.example.policemobiledirectory.ui.components.CommonEmployeeForm
import java.util.Date

// ✅ Helper function for clean date formatting
private fun formatDate(date: Date?, context: Context): String {
    return date?.let {
        android.text.format.DateFormat.getDateFormat(context).format(it)
    } ?: "N/A"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingApprovalsScreen(
    navController: NavController,
    viewModel: EmployeeViewModel = hiltViewModel()
) {
    val pendingRegistrations by viewModel.pendingRegistrations.collectAsState()
    val operationStatus by viewModel.pendingStatus.collectAsState()
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf<PendingRegistrationEntity?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    var dialogActionIsApprove by remember { mutableStateOf(true) }
    var pendingToEdit by remember { mutableStateOf<PendingRegistrationEntity?>(null) }

    // Get the current back stack entry to detect when screen comes back into focus
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // ✅ Fetch pending registrations on screen load and when coming back
    LaunchedEffect(currentRoute) {
        // Only refresh if we're on the pending approvals screen
        if (currentRoute == com.example.policemobiledirectory.navigation.Routes.PENDING_APPROVALS) {
            viewModel.refreshPendingRegistrations()
        }
    }

    // ✅ Listen for operation results (approve/reject)
    LaunchedEffect(operationStatus) {
        when (val status = operationStatus) {
            is OperationStatus.Success<*> -> {
                Toast.makeText(context, status.data.toString(), Toast.LENGTH_SHORT).show()
                viewModel.refreshPendingRegistrations()
                viewModel.resetPendingStatus()
            }

            is OperationStatus.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                viewModel.resetPendingStatus()
            }

            else -> Unit
        }
    }

    val isProcessing = operationStatus is OperationStatus.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Approvals") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
        ) {
            when {
                isProcessing && pendingRegistrations.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                pendingRegistrations.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No pending approvals at the moment.")
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(pendingRegistrations, key = { it.firestoreId ?: it.kgid }) { entity ->
                            PendingApprovalCard(
                                entity = entity,
                                isProcessing = isProcessing,
                                onApproveClick = {
                                    dialogActionIsApprove = true
                                    showDialog = entity
                                },
                                onRejectClick = {
                                    dialogActionIsApprove = false
                                    rejectionReason = ""
                                    showDialog = entity
                                },
                                onEditClick = { pendingToEdit = entity }
                            )
                        }
                    }
                }
            }
        }
    }

    // ✅ Confirm / Reject dialog
    showDialog?.let { entityToProcess ->
        AlertDialog(
            onDismissRequest = { showDialog = null },
            title = {
                Text(
                    if (dialogActionIsApprove) "Approve Registration"
                    else "Reject Registration"
                )
            },
            text = {
                Column {
                    val currentName = pendingRegistrations.find {
                        it.firestoreId == entityToProcess.firestoreId
                    }?.name ?: entityToProcess.name

                    Text(
                        "Are you sure you want to ${if (dialogActionIsApprove) "approve" else "reject"} " +
                                "KGID: ${entityToProcess.kgid} ($currentName)?"
                    )

                    if (!dialogActionIsApprove) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = rejectionReason,
                            onValueChange = { rejectionReason = it },
                            label = { Text("Reason for rejection (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            maxLines = 3
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dialogActionIsApprove)
                            viewModel.approveRegistration(entityToProcess)
                        else
                            viewModel.rejectRegistration(entityToProcess, rejectionReason.ifBlank { "No reason provided" })

                        showDialog = null
                    }
                ) {
                    Text(if (dialogActionIsApprove) "APPROVE" else "REJECT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = null }) { Text("CANCEL") }
            }
        )
    }

    pendingToEdit?.let { entityToEdit ->
        PendingRegistrationEditDialog(
            entity = entityToEdit,
            onDismiss = { pendingToEdit = null },
            onSubmit = { updatedEntity, photoUri ->
                viewModel.updatePendingRegistration(updatedEntity, photoUri)
                pendingToEdit = null
            }
        )
    }
}

@Composable
fun PendingApprovalCard(
    entity: PendingRegistrationEntity,
    isProcessing: Boolean,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AsyncImage(
                model = entity.photoUrl ?: entity.photoUrlFromGoogle,
                contentDescription = "User Photo",
                placeholder = painterResource(R.drawable.officer),
                error = painterResource(R.drawable.officer),
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(entity.name, style = MaterialTheme.typography.titleMedium)
            Text("KGID: ${entity.kgid}")
            Text("Email: ${entity.email}")
            Text("Mobile 1: ${entity.mobile1}")
            entity.mobile2?.takeIf { it.isNotBlank() }?.let { Text("Mobile 2: $it") }
            Text("District: ${entity.district}")
            Text("Station: ${entity.station}")
            Text("Rank: ${entity.rank}")
            entity.metalNumber?.takeIf { it.isNotBlank() }?.let { Text("Metal No: $it") }
            Text("Blood Group: ${entity.bloodGroup ?: "??"}")
            Text("Registered On: ${formatDate(entity.createdAt, context)}")

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEditClick,
                    enabled = !isProcessing
                ) {
                    Text("EDIT")
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onRejectClick,
                    enabled = !isProcessing
                ) { Text("REJECT") }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onApproveClick,
                    enabled = !isProcessing
                ) { Text("APPROVE") }
            }
        }
    }
}

@Composable
private fun PendingRegistrationEditDialog(
    entity: PendingRegistrationEntity,
    onDismiss: () -> Unit,
    onSubmit: (PendingRegistrationEntity, android.net.Uri?) -> Unit
) {
    val editableEmployee = remember(entity) {
        entity.toEmployee(entity.photoUrl ?: entity.photoUrlFromGoogle)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Pending Registration",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = onDismiss) {
                        Text("CLOSE")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                CommonEmployeeForm(
                    isAdmin = true,
                    isSelfEdit = false,
                    isRegistration = false,
                    initialEmployee = editableEmployee,
                    initialKgid = entity.kgid,
                    onNavigateToTerms = null,
                    onSubmit = { employee, photoUri ->
                        val updatedPending = entity.copy(
                            kgid = employee.kgid,
                            name = employee.name,
                            email = employee.email,
                            mobile1 = employee.mobile1 ?: "",
                            mobile2 = employee.mobile2,
                            rank = employee.rank ?: "",
                            metalNumber = employee.metalNumber,
                            district = employee.district ?: "",
                            station = employee.station ?: "",
                            bloodGroup = employee.bloodGroup ?: "",
                            photoUrl = employee.photoUrl
                        )
                        onSubmit(updatedPending, photoUri)
                    },
                    onRegisterSubmit = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
