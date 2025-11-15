package com.example.policemobiledirectory.ui.screens

sealed class PinChangeUiState {
    object Idle : PinChangeUiState()
    object Loading : PinChangeUiState()
    data class Success(val message: String) : PinChangeUiState()
    data class Error(val message: String) : PinChangeUiState()
}
