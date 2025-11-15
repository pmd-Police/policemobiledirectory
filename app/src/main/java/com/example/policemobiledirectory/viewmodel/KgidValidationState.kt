package com.example.policemobiledirectory.viewmodel
sealed interface KgidValidationState {
    object Idle : KgidValidationState
    object Loading : KgidValidationState
    object Valid : KgidValidationState
    data class Invalid(val message: String) : KgidValidationState
}