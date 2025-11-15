package com.example.policemobiledirectory.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.data.local.PendingRegistrationEntity
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.ui.components.CommonEmployeeForm
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.AddEditEmployeeViewModel
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import kotlinx.coroutines.launch

@Composable
fun AddEditEmployeeScreen(
    employeeId: String?,
    navController: NavController,
    addEditViewModel: AddEditEmployeeViewModel = hiltViewModel(),
    employeeViewModel: EmployeeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val employee by addEditViewModel.employee.collectAsState()
    val saveStatus by addEditViewModel.saveStatus.collectAsState()
    val isNewEmployee = employeeId.isNullOrBlank()

    // Show feedback messages
    LaunchedEffect(saveStatus) {
        when (val status = saveStatus) {
            is RepoResult.Success -> {
                Toast.makeText(
                    context,
                    if (isNewEmployee) "Employee saved successfully" else "Employee updated successfully",
                    Toast.LENGTH_SHORT
                ).show()
                addEditViewModel.resetSaveStatus()
            }
            is RepoResult.Error -> {
                Toast.makeText(
                    context,
                    status.message ?: "Failed to save employee",
                    Toast.LENGTH_LONG
                ).show()
                addEditViewModel.resetSaveStatus()
            }
            else -> Unit
        }
    }

    val scope = rememberCoroutineScope()
    
    // Track if we've submitted (to distinguish submission errors from load errors)
    val hasSubmittedState = remember { mutableStateOf(false) }
    
    // Show pending approval status (only for submission, not for load errors)
    val pendingStatus by employeeViewModel.pendingStatus.collectAsState()

    CommonEmployeeForm(
        isAdmin = true,
        isSelfEdit = false,
        isRegistration = false,
        initialEmployee = employee,
        initialKgid = employeeId,
        onSubmit = { emp: Employee, photo: Uri? ->
            scope.launch {
                var finalPhotoUrl = emp.photoUrl
                
                // Upload photo if provided
                if (photo != null) {
                    var uploadSuccess = false
                    addEditViewModel.imageRepository.uploadOfficerImage(photo, emp.kgid).collect { status ->
                        when (status) {
                            is OperationStatus.Success -> {
                                status.data?.let { url ->
                                    finalPhotoUrl = url
                                    uploadSuccess = true
                                }
                            }
                            is OperationStatus.Error -> {
                                Toast.makeText(
                                    context,
                                    "Photo upload failed: ${status.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            else -> Unit
                        }
                    }
                    if (!uploadSuccess) return@launch
                }

                // Submit for approval
                hasSubmittedState.value = true
                val pending = PendingRegistrationEntity(
                    name = emp.name,
                    kgid = emp.kgid,
                    email = emp.email,
                    mobile1 = emp.mobile1 ?: "",
                    mobile2 = emp.mobile2,
                    pin = "", // No PIN for updates
                    rank = emp.rank ?: "",
                    metalNumber = emp.metalNumber,
                    district = emp.district.orEmpty(),
                    station = emp.station.orEmpty(),
                    bloodGroup = emp.bloodGroup.orEmpty(),
                    firebaseUid = "",
                    photoUrl = finalPhotoUrl
                )
                employeeViewModel.registerNewUser(pending)
            }
        },
        onRegisterSubmit = { pending: PendingRegistrationEntity, photo: Uri? ->
            scope.launch {
                var finalPhotoUrl = pending.photoUrl
                
                // Upload photo if provided
                if (photo != null) {
                    var uploadSuccess = false
                    addEditViewModel.imageRepository.uploadOfficerImage(photo, pending.kgid).collect { status ->
                        when (status) {
                            is OperationStatus.Success -> {
                                status.data?.let { url ->
                                    finalPhotoUrl = url
                                    uploadSuccess = true
                                }
                            }
                            is OperationStatus.Error -> {
                                Toast.makeText(
                                    context,
                                    "Photo upload failed: ${status.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            else -> Unit
                        }
                    }
                    if (!uploadSuccess) return@launch
                }

                hasSubmittedState.value = true
                val updatedPending = pending.copy(photoUrl = finalPhotoUrl)
                employeeViewModel.registerNewUser(updatedPending)
            }
        }
    )
    
    LaunchedEffect(pendingStatus) {
        when (val status = pendingStatus) {
            is OperationStatus.Success -> {
                if (hasSubmittedState.value || status.data?.contains("submitted") == true || status.data?.contains("approved") == true) {
                    Toast.makeText(
                        context,
                        status.data ?: "Employee details submitted for approval",
                        Toast.LENGTH_SHORT
                    ).show()
                    hasSubmittedState.value = false
                    employeeViewModel.resetPendingStatus()
                }
                // Silently handle load success - we don't need to show it
                if (status.data == "Loaded") {
                    employeeViewModel.resetPendingStatus()
                }
            }
            is OperationStatus.Error -> {
                // Only show error if it's related to submission, not loading
                val errorMessage = status.message ?: ""
                if (hasSubmittedState.value || errorMessage.contains("submitted") || errorMessage.contains("approval")) {
                    Toast.makeText(
                        context,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                    hasSubmittedState.value = false
                }
                // Silently reset load errors (permission denied, etc.)
                employeeViewModel.resetPendingStatus()
            }
            else -> Unit
        }
    }
}
