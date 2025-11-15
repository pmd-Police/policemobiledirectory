package com.example.policemobiledirectory.ui.screens

import com.example.policemobiledirectory.model.Employee

sealed class GoogleSignInUiEvent {
    data object Idle : GoogleSignInUiEvent()
    data object Loading : GoogleSignInUiEvent()
    data class SignInSuccess(val user: Employee) : GoogleSignInUiEvent()
    data class RegistrationRequired(val email: String) : GoogleSignInUiEvent()
    data class UserNotFoundInFirebase(val email: String) : GoogleSignInUiEvent()
    data class LoginSuccess(val isAdmin: Boolean) : GoogleSignInUiEvent()
    data class Error(val message: String) : GoogleSignInUiEvent()
}
