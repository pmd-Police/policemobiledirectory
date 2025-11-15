package com.example.policemobiledirectory.viewmodel

sealed interface EmployeeCrudUiState {
    object Idle : EmployeeCrudUiState
    object Loading : EmployeeCrudUiState
    data class Success(val message: String) : EmployeeCrudUiState
    data class Error(val message: String) : EmployeeCrudUiState
}
