package com.example.policemobiledirectory.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.data.local.PendingRegistrationEntity
import com.example.policemobiledirectory.model.NotificationTarget
import com.example.policemobiledirectory.repository.PendingRegistrationRepository
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.utils.OperationStatus
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * ViewModel responsible for pending registration operations:
 * - Fetching pending registrations
 * - Approving/rejecting registrations
 * - Registering new users
 * - Updating pending registrations
 */
@HiltViewModel
class PendingRegistrationViewModel @Inject constructor(
    private val pendingRepo: PendingRegistrationRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _pendingRegistrations = MutableStateFlow<List<PendingRegistrationEntity>>(emptyList())
    val pendingRegistrations: StateFlow<List<PendingRegistrationEntity>> = _pendingRegistrations.asStateFlow()

    private val _pendingStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val pendingStatus: StateFlow<OperationStatus<String>> = _pendingStatus.asStateFlow()

    fun refreshPendingRegistrations() = viewModelScope.launch {
        try {
            _pendingStatus.value = OperationStatus.Loading

            when (val result = pendingRepo.fetchPendingFromFirestore()) {
                is RepoResult.Success -> {
                    val list = result.data ?: emptyList()
                    _pendingRegistrations.value = list
                    pendingRepo.saveAllToLocal(list)   // sync to Room
                    _pendingStatus.value = OperationStatus.Success("Loaded")
                }

                is RepoResult.Error -> {
                    _pendingRegistrations.value = emptyList()
                    val errorMsg = result.message ?: "Load failed"
                    // Only set error status if it's not a permission issue
                    if (errorMsg.contains("Permission", ignoreCase = true) ||
                        errorMsg.contains("permission denied", ignoreCase = true)) {
                        Log.d("PendingReg", "Permission denied loading pending registrations (expected for non-admins)")
                        _pendingStatus.value = OperationStatus.Idle
                    } else {
                        _pendingStatus.value = OperationStatus.Error(errorMsg)
                    }
                }

                else -> {
                    _pendingStatus.value = OperationStatus.Idle
                }
            }
        } catch (e: Exception) {
            _pendingRegistrations.value = emptyList()
            val errorMsg = e.message ?: "Load failed"
            if (errorMsg.contains("Permission", ignoreCase = true)) {
                Log.d("PendingReg", "Permission denied loading pending registrations (expected for non-admins)")
                _pendingStatus.value = OperationStatus.Idle
            } else {
                _pendingStatus.value = OperationStatus.Error(errorMsg)
            }
        }
    }

    fun approveRegistration(entity: PendingRegistrationEntity) {
        viewModelScope.launch {
            _pendingStatus.value = OperationStatus.Loading

            when (val result = pendingRepo.approve(entity)) {
                is RepoResult.Success -> {
                    _pendingStatus.value = OperationStatus.Success("Approved successfully")
                    refreshPendingRegistrations()
                }
                is RepoResult.Error -> {
                    _pendingStatus.value = OperationStatus.Error(result.message ?: "Approval failed")
                }
                else -> Unit
            }
        }
    }

    fun rejectRegistration(entity: PendingRegistrationEntity, reason: String) {
        viewModelScope.launch {
            _pendingStatus.value = OperationStatus.Loading

            when (val result = pendingRepo.reject(entity, reason)) {
                is RepoResult.Success -> {
                    _pendingStatus.value = OperationStatus.Success("Rejected")
                    refreshPendingRegistrations()
                }
                is RepoResult.Error -> {
                    _pendingStatus.value = OperationStatus.Error(result.message ?: "Rejection failed")
                }
                else -> Unit
            }
        }
    }

    fun registerNewUser(
        entity: PendingRegistrationEntity,
        onNotificationSent: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _pendingStatus.value = OperationStatus.Loading

            try {
                // Check for duplicate registration directly in Firestore
                val hasDuplicate = try {
                    val kgidSnapshot = firestore.collection("pending_registrations")
                        .whereEqualTo("status", "pending")
                        .whereEqualTo("kgid", entity.kgid)
                        .limit(1)
                        .get()
                        .await()

                    if (!kgidSnapshot.isEmpty) {
                        true
                    } else {
                        val emailSnapshot = firestore.collection("pending_registrations")
                            .whereEqualTo("status", "pending")
                            .whereEqualTo("email", entity.email)
                            .limit(1)
                            .get()
                            .await()
                        !emailSnapshot.isEmpty
                    }
                } catch (e: Exception) {
                    Log.w("RegisterUser", "Duplicate check failed, proceeding anyway: ${e.message}")
                    false
                }

                if (hasDuplicate) {
                    _pendingStatus.value = OperationStatus.Error(
                        "A registration for this KGID/Email already exists and is pending approval."
                    )
                    return@launch
                }

                // Prepare safe PendingRegistration object
                val pending = entity.copy(
                    isApproved = false,
                    firebaseUid = entity.firebaseUid.takeIf { it.isNotBlank() } ?: "",
                    status = "pending",
                    rejectionReason = null,
                    photoUrlFromGoogle = null
                )

                // Submit to Firestore + Room
                pendingRepo.addPendingRegistration(pending).collect { result ->
                    when (result) {
                        is RepoResult.Loading ->
                            _pendingStatus.value = OperationStatus.Loading

                        is RepoResult.Success -> {
                            _pendingStatus.value =
                                OperationStatus.Success("Registration submitted for admin approval.")

                            // Notify admin (don't wait for completion)
                            onNotificationSent?.invoke()
                        }

                        is RepoResult.Error -> {
                            _pendingStatus.value =
                                OperationStatus.Error(result.message ?: "Registration failed.")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("RegisterUser", "❌ Registration failed", e)
                _pendingStatus.value =
                    OperationStatus.Error(e.localizedMessage ?: "Unexpected error")
            }
        }
    }

    fun updatePendingRegistration(entity: PendingRegistrationEntity, newPhotoUri: Uri?) {
        viewModelScope.launch {
            _pendingStatus.value = OperationStatus.Loading
            try {
                var updatedEntity = entity
                if (newPhotoUri != null) {
                    val photoUrl = pendingRepo.uploadPhoto(entity, newPhotoUri)
                    updatedEntity = updatedEntity.copy(photoUrl = photoUrl)
                }

                when (val result = pendingRepo.updatePendingRegistration(updatedEntity)) {
                    is RepoResult.Success -> {
                        _pendingStatus.value = OperationStatus.Success("Pending registration updated.")
                        refreshPendingRegistrations()
                    }
                    is RepoResult.Error -> {
                        _pendingStatus.value = OperationStatus.Error(result.message ?: "Update failed.")
                    }
                    else -> Unit
                }
            } catch (e: Exception) {
                Log.e("PendingUpdate", "❌ Update failed", e)
                _pendingStatus.value = OperationStatus.Error(e.localizedMessage ?: "Update failed.")
            }
        }
    }

    fun resetPendingStatus() {
        _pendingStatus.value = OperationStatus.Idle
    }
}



