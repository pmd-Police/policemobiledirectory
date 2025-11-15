package com.example.policemobiledirectory.viewmodel

// Sealed interface for photo upload state
sealed interface UploadPhotoUiState {
    object Idle : UploadPhotoUiState
    object Loading : UploadPhotoUiState
    data class Success(val url: String) : UploadPhotoUiState
    data class Error(val message: String) : UploadPhotoUiState
}
