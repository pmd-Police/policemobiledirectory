package com.example.policemobiledirectory.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.data.local.PendingRegistrationEntity
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.ui.components.CommonEmployeeForm
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.AddEditEmployeeViewModel
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import kotlinx.coroutines.launch

@Composable
fun UserRegistrationScreen(
    navController: NavController,
    viewModel: EmployeeViewModel = hiltViewModel(),
    addEditViewModel: AddEditEmployeeViewModel = hiltViewModel(),
    initialEmail: String = ""
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val pendingStatus by viewModel.pendingStatus.collectAsState()
    val scope = rememberCoroutineScope()

    // Track if we've submitted (to show proper feedback)
    val hasSubmittedState = androidx.compose.runtime.remember { 
        androidx.compose.runtime.mutableStateOf(false) 
    }

    // Handle registration status feedback
    LaunchedEffect(pendingStatus) {
        when (val status = pendingStatus) {
            is OperationStatus.Success -> {
                if (hasSubmittedState.value || status.data?.contains("submitted") == true || 
                    status.data?.contains("approved") == true) {
                    Toast.makeText(
                        context,
                        status.data ?: "Registration submitted for admin approval",
                        Toast.LENGTH_SHORT
                    ).show()
                    hasSubmittedState.value = false
                    viewModel.resetPendingStatus()
                    // Optionally navigate back after successful submission
                    // navController.popBackStack()
                }
                // Silently handle load success
                if (status.data == "Loaded") {
                    viewModel.resetPendingStatus()
                }
            }
            is OperationStatus.Error -> {
                // Only show error if it's related to submission
                val errorMessage = status.message ?: ""
                if (hasSubmittedState.value || errorMessage.contains("submitted") || 
                    errorMessage.contains("approval") || errorMessage.contains("Registration")) {
                    Toast.makeText(
                        context,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                    hasSubmittedState.value = false
                }
                // Silently reset load errors
                viewModel.resetPendingStatus()
            }
            else -> Unit
        }
    }

    Scaffold { _ ->
        CommonEmployeeForm(
            isAdmin = false,
            isSelfEdit = false,
            isRegistration = true,
            initialEmployee = null,
            initialKgid = null,
            initialEmail = initialEmail, // ✅ Prefill email from Google Sign-In
            onSubmit = { _, _ -> /* not used */ },
            onNavigateToTerms = {
                navController.navigate(Routes.TERMS_AND_CONDITIONS)
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

                    // Mark that we're submitting
                    hasSubmittedState.value = true
                    
                    // Prepare pending registration with uploaded photo URL
                    val withUid = pending.copy(
                        firebaseUid = currentUser?.firebaseUid ?: "",
                        photoUrl = finalPhotoUrl
                    )
                    
                    // Submit for approval
                    viewModel.registerNewUser(withUid)
                }
            }
        )
    }
}
