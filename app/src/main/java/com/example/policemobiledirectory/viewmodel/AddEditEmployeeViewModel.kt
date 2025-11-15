package com.example.policemobiledirectory.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.repository.EmployeeRepository
import com.example.policemobiledirectory.repository.ImageRepository
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.repository.RepoResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditEmployeeViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    val imageRepository: ImageRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _employee = MutableStateFlow<Employee?>(null)
    val employee = _employee.asStateFlow()

    private val _saveStatus = MutableStateFlow<RepoResult<Boolean>?>(null)
    val saveStatus = _saveStatus.asStateFlow()

    private val _photoUploadStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val photoUploadStatus = _photoUploadStatus.asStateFlow()

    init {
        savedStateHandle.get<String>("employeeId")?.let { employeeId ->
            if (employeeId.isNotEmpty()) {
                viewModelScope.launch {
                    employeeRepository.getEmployees().collect { repoResult ->
                        if (repoResult is RepoResult.Success) {
                            _employee.value = repoResult.data?.find { it.kgid == employeeId }
                        }
                    }
                }
            }
        }
    }

    /**
     * Used by:
     *  - Admin Add Employee
     *  - Admin Edit Employee
     *  - MyProfileEdit (self-edit)  <-- new requirement
     */
    fun saveEmployee(employee: Employee, newPhotoUri: Uri?) {
        android.util.Log.e("AddEditViewModel", "═══════════════════════════════════════")
        android.util.Log.e("AddEditViewModel", "🎯🎯🎯 saveEmployee() FUNCTION CALLED 🎯🎯🎯")
        android.util.Log.e("AddEditViewModel", "Employee KGID: ${employee.kgid}")
        android.util.Log.e("AddEditViewModel", "Has new photo: ${newPhotoUri != null}")
        android.util.Log.e("AddEditViewModel", "Photo URI: $newPhotoUri")
        android.util.Log.e("AddEditViewModel", "About to launch viewModelScope...")
        
        viewModelScope.launch {
            android.util.Log.e("AddEditViewModel", "✅ Inside viewModelScope.launch block")
            try {
                var updatedEmployee = employee

                // Upload new photo if provided
                if (newPhotoUri != null) {
                    android.util.Log.d("AddEditViewModel", "📸 Photo provided, starting upload...")
                    android.util.Log.d("AddEditViewModel", "📤 Starting photo upload for: ${employee.kgid}")
                    var uploadFailed = false

                    try {
                        imageRepository.uploadOfficerImage(newPhotoUri, employee.kgid).collect { status ->
                            android.util.Log.d("AddEditViewModel", "📥 Photo upload status: $status")
                            _photoUploadStatus.value = status

                            when (status) {
                                is OperationStatus.Success -> {
                                    status.data?.let { url ->
                                        android.util.Log.d("AddEditViewModel", "✅ Upload successful, URL: $url")
                                        updatedEmployee = updatedEmployee.copy(photoUrl = url)
                                    }
                                }
                                is OperationStatus.Error -> {
                                    android.util.Log.e("AddEditViewModel", "❌ Upload failed: ${status.message}")
                                    _saveStatus.value = RepoResult.Error(message = status.message)
                                    uploadFailed = true
                                }
                                is OperationStatus.Loading -> {
                                    android.util.Log.d("AddEditViewModel", "⏳ Upload in progress...")
                                }
                                else -> {
                                    android.util.Log.d("AddEditViewModel", "📊 Status: $status")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AddEditViewModel", "❌ Exception in upload flow: ${e.message}", e)
                        _saveStatus.value = RepoResult.Error(message = "Upload failed: ${e.message}")
                        uploadFailed = true
                    }

                    _photoUploadStatus.value = OperationStatus.Idle
                    if (uploadFailed) {
                        android.util.Log.e("AddEditViewModel", "❌ Upload failed, returning early")
                        return@launch
                    }
                    android.util.Log.d("AddEditViewModel", "✅ Photo upload completed successfully")
                } else {
                    android.util.Log.d("AddEditViewModel", "ℹ️ No photo to upload, proceeding to save employee")
                }

                // Save to DB + Google Sheet
                android.util.Log.d("AddEditViewModel", "💾 Starting to save employee to repository...")
                _saveStatus.value = RepoResult.Loading

                employeeRepository.addOrUpdateEmployee(updatedEmployee).collect { result ->
                    android.util.Log.d("AddEditViewModel", "📥 Repository result: $result")
                    _saveStatus.value = result
                }
                android.util.Log.d("AddEditViewModel", "✅ saveEmployee() completed")
            } catch (e: Exception) {
                android.util.Log.e("AddEditViewModel", "❌❌❌ EXCEPTION in saveEmployee: ${e.message}", e)
                e.printStackTrace()
                _saveStatus.value = RepoResult.Error(message = "Unexpected error: ${e.message}")
            }
        }
    }

    // 👇 New helper for MyProfileEditScreen — calls same function
    fun updateMyProfile(employee: Employee, newPhotoUri: Uri?) {
        saveEmployee(employee, newPhotoUri)
    }

    fun resetSaveStatus() {
        _saveStatus.value = null
    }

    fun resetPhotoStatus() {
        _photoUploadStatus.value = OperationStatus.Idle
    }

    /**
     * Upload photo and return the URL via callback
     */
    suspend fun uploadPhotoAndGetUrl(photoUri: Uri, kgid: String, onComplete: (String?) -> Unit) {
        imageRepository.uploadOfficerImage(photoUri, kgid).collect { status ->
            _photoUploadStatus.value = status
            when (status) {
                is OperationStatus.Success -> {
                    onComplete(status.data)
                    _photoUploadStatus.value = OperationStatus.Idle
                }
                is OperationStatus.Error -> {
                    onComplete(null)
                    _photoUploadStatus.value = OperationStatus.Idle
                }
                else -> Unit
            }
        }
    }
}
