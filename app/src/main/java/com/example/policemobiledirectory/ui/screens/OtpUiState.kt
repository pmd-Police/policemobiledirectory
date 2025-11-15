package com.example.policemobiledirectory.ui.screens

sealed class OtpUiState {
    object Idle : OtpUiState()
    object Loading : OtpUiState()
    data class OtpSent(val message: String) : OtpUiState()
    data class OtpVerified(val message: String) : OtpUiState()
    data class Error(val message: String) : OtpUiState()
}
