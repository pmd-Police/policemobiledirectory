package com.example.policemobiledirectory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.data.local.SessionManager
import com.example.policemobiledirectory.data.mapper.toEmployee
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.repository.EmployeeRepository
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.ui.screens.GoogleSignInUiEvent
import com.example.policemobiledirectory.utils.OperationStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel responsible for authentication operations:
 * - Login/Logout
 * - Google Sign-In
 * - OTP/PIN management
 * - Session management
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val employeeRepo: EmployeeRepository,
    private val sessionManager: SessionManager,
    private val auth: FirebaseAuth
) : ViewModel() {

    // Authentication State
    private val _currentUser = MutableStateFlow<Employee?>(null)
    val currentUser: StateFlow<Employee?> = _currentUser.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _authStatus = MutableStateFlow<OperationStatus<Employee>>(OperationStatus.Idle)
    val authStatus: StateFlow<OperationStatus<Employee>> = _authStatus.asStateFlow()

    private val _googleSignInUiEvent = MutableStateFlow<GoogleSignInUiEvent>(GoogleSignInUiEvent.Idle)
    val googleSignInUiEvent: StateFlow<GoogleSignInUiEvent> = _googleSignInUiEvent.asStateFlow()

    // OTP/PIN State
    private val _otpUiState = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val otpUiState: StateFlow<OperationStatus<String>> = _otpUiState

    private val _verifyOtpUiState = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val verifyOtpUiState: StateFlow<OperationStatus<String>> = _verifyOtpUiState

    private val _pinResetUiState = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val pinResetUiState: StateFlow<OperationStatus<String>> = _pinResetUiState

    private val _pinChangeState = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val pinChangeState: StateFlow<OperationStatus<String>> = _pinChangeState.asStateFlow()

    private var otpSentTime: Long? = null
    private val otpValidityDuration = 5 * 60 * 1000L
    private val _remainingTime = MutableStateFlow(0L)
    val remainingTime: StateFlow<Long> = _remainingTime

    init {
        Log.d("AuthViewModel", "🟢 AuthViewModel initialized")
        loadSession()

        // Observe login state from DataStore
        viewModelScope.launch {
            sessionManager.isLoggedIn.collect { loggedIn ->
                _isLoggedIn.value = loggedIn
                Log.d("Session", "🔄 isLoggedIn = $loggedIn")
            }
        }

        // Observe admin flag from DataStore
        viewModelScope.launch {
            sessionManager.isAdmin.collect { isAdmin ->
                _isAdmin.value = isAdmin
                Log.d("Session", "🔄 isAdmin = $isAdmin")
            }
        }

        // Restore current user session from Room or Firestore
        viewModelScope.launch {
            sessionManager.userEmail.collect { email ->
                if (_isLoggedIn.value == false && email.isBlank()) {
                    Log.d("Session", "🔒 Logout in progress, skipping session restore")
                    return@collect
                }

                if (email.isNotBlank()) {
                    Log.d("Session", "🔁 Restoring session for $email")

                    // Try Room first
                    val localUser = employeeRepo.getEmployeeByEmail(email)
                    if (localUser != null) {
                        _currentUser.value = localUser.toEmployee()
                        _isAdmin.value = localUser.isAdmin
                        _isLoggedIn.value = true
                        Log.d("Session", "✅ Loaded user ${localUser.name} (Admin=${localUser.isAdmin})")
                    } else {
                        // Fallback to Firestore if Room is empty
                        when (val remoteResult = employeeRepo.getUserByEmail(email)) {
                            is RepoResult.Success -> {
                                remoteResult.data?.let { user ->
                                    _currentUser.value = user
                                    _isAdmin.value = user.isAdmin
                                    _isLoggedIn.value = true
                                    Log.d("Session", "✅ Loaded remote user ${user.name}")
                                } ?: run {
                                    Log.w("Session", "⚠️ No matching user found for $email — resetting session")
                                    sessionManager.clearSession()
                                    _isLoggedIn.value = false
                                }
                            }
                            is RepoResult.Error -> {
                                Log.e("Session", "❌ Error loading user: ${remoteResult.message}")
                                sessionManager.clearSession()
                                _isLoggedIn.value = false
                            }
                            else -> Unit
                        }
                    }
                } else {
                    Log.d("Session", "🔒 No stored email — user not logged in")
                    if (_currentUser.value != null || _isLoggedIn.value == true) {
                        _currentUser.value = null
                        _isAdmin.value = false
                        _isLoggedIn.value = false
                    }
                }
            }
        }

        // Ensure signed in if needed
        viewModelScope.launch {
            try {
                ensureSignedInIfNeeded()
            } catch (e: Exception) {
                Log.e("Startup", "Startup failed: ${e.message}", e)
            }
        }
    }

    // =========================================================
    // AUTHENTICATION METHODS
    // =========================================================

    fun loginWithPin(email: String, pin: String) {
        viewModelScope.launch {
            employeeRepo.loginUser(email, pin).collect { result ->
                when (result) {
                    is RepoResult.Success -> {
                        val user = result.data
                        if (user != null) {
                            // Instantly update UI before waiting for DataStore
                            _currentUser.value = user
                            _isAdmin.value = user.isAdmin
                            _isLoggedIn.value = true

                            // Save to DataStore for persistence
                            sessionManager.saveLogin(email, user.isAdmin)

                            // Fetch a fresh version from local DB
                            val refreshed = employeeRepo.getEmployeeDirect(email)
                            if (refreshed != null) {
                                _currentUser.value = refreshed
                                _isAdmin.value = refreshed.isAdmin
                            }

                            _authStatus.value = OperationStatus.Success(user)
                            Log.d("Login", "✅ Logged in as ${user.name}, Admin=${user.isAdmin}")
                        } else {
                            _authStatus.value = OperationStatus.Error("User not found")
                        }
                    }
                    is RepoResult.Error -> {
                        _authStatus.value = OperationStatus.Error(result.message ?: "Login failed")
                    }
                    is RepoResult.Loading -> {
                        _authStatus.value = OperationStatus.Loading
                    }
                }
            }
        }
    }

    fun handleGoogleSignIn(email: String, googleIdToken: String) {
        viewModelScope.launch {
            _googleSignInUiEvent.value = GoogleSignInUiEvent.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                if (authResult.user != null) {
                    val existingUser = employeeRepo.getEmployeeByEmail(email)
                    if (existingUser != null) {
                        val user = existingUser.toEmployee()
                        sessionManager.saveLogin(user.email, user.isAdmin)
                        _currentUser.value = user
                        _isLoggedIn.value = true
                        _googleSignInUiEvent.value = GoogleSignInUiEvent.SignInSuccess(user)
                    } else {
                        _googleSignInUiEvent.value = GoogleSignInUiEvent.RegistrationRequired(email)
                    }
                } else {
                    _googleSignInUiEvent.value = GoogleSignInUiEvent.Error("Sign-in failed: Firebase user is null.")
                }
            } catch (e: Exception) {
                Log.e("GoogleSignIn", "❌ Failed", e)
                _googleSignInUiEvent.value = GoogleSignInUiEvent.Error(e.localizedMessage ?: "Unknown error")
                logout()
            }
        }
    }

    fun logout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                Log.d("Logout", "🚪 Starting logout...")

                // Clear local session FIRST
                sessionManager.clearSession()

                // Reset in-memory session IMMEDIATELY
                _isLoggedIn.value = false
                _isAdmin.value = false
                _currentUser.value = null

                // Reset auth/UI state
                _authStatus.value = OperationStatus.Idle
                _googleSignInUiEvent.value = GoogleSignInUiEvent.Idle

                // Sign out of Firebase
                FirebaseAuth.getInstance().signOut()
                auth.signOut()

                // Clear repository data
                employeeRepo.logout()

                Log.d("Logout", "✅ Logout complete")

                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                Log.e("Logout", "❌ Logout failed: ${e.message}")
                // Even if there's an error, ensure state is cleared
                _isLoggedIn.value = false
                _isAdmin.value = false
                _currentUser.value = null
                _authStatus.value = OperationStatus.Idle
                _googleSignInUiEvent.value = GoogleSignInUiEvent.Idle
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            }
        }
    }

    // =========================================================
    // OTP / PIN FLOW
    // =========================================================

    fun sendOtp(email: String) {
        viewModelScope.launch {
            Log.d("ForgotPinFlow", "🟢 sendOtp() for $email")
            _otpUiState.value = OperationStatus.Loading

            try {
                when (val result = employeeRepo.sendOtp(email)) {
                    is RepoResult.Success -> {
                        _otpUiState.value = OperationStatus.Success(result.data ?: "OTP sent to $email")
                        startOtpCountdown()
                    }
                    is RepoResult.Error -> {
                        _otpUiState.value = OperationStatus.Error(result.message ?: "Failed to send OTP")
                    }
                    else -> Unit
                }
            } catch (e: Exception) {
                _otpUiState.value = OperationStatus.Error("Unexpected error: ${e.localizedMessage}")
            }
        }
    }

    fun verifyOtp(email: String, code: String) {
        viewModelScope.launch {
            _verifyOtpUiState.value = OperationStatus.Loading
            try {
                when (val result = employeeRepo.verifyLoginCode(email, code)) {
                    is RepoResult.Success -> _verifyOtpUiState.value = OperationStatus.Success("OTP verified successfully")
                    is RepoResult.Error -> _verifyOtpUiState.value = OperationStatus.Error(result.message ?: "Invalid OTP")
                    else -> Unit
                }
            } catch (e: Exception) {
                _verifyOtpUiState.value = OperationStatus.Error(e.message ?: "Error verifying OTP")
            }
        }
    }

    fun updatePinAfterOtp(email: String, newPin: String) {
        viewModelScope.launch {
            _pinResetUiState.value = OperationStatus.Loading
            try {
                val result = employeeRepo.updateUserPin(email, null, newPin, true)
                when (result) {
                    is RepoResult.Success -> _pinResetUiState.value = OperationStatus.Success("PIN reset successful")
                    is RepoResult.Error -> _pinResetUiState.value = OperationStatus.Error(result.message ?: "Failed to reset PIN")
                    else -> Unit
                }
            } catch (e: Exception) {
                _pinResetUiState.value = OperationStatus.Error(e.message ?: "Error updating PIN")
            }
        }
    }

    fun changePin(email: String, oldPin: String, newPin: String) {
        viewModelScope.launch {
            _pinChangeState.value = OperationStatus.Loading
            when (val result = employeeRepo.updateUserPin(email, oldPin, newPin, false)) {
                is RepoResult.Success -> _pinChangeState.value = OperationStatus.Success("PIN changed successfully")
                is RepoResult.Error -> _pinChangeState.value = OperationStatus.Error(result.message ?: "Failed to change PIN")
                else -> Unit
            }
        }
    }

    private fun startOtpCountdown() {
        viewModelScope.launch {
            val start = System.currentTimeMillis()
            otpSentTime = start
            while (System.currentTimeMillis() - start < otpValidityDuration) {
                _remainingTime.value = otpValidityDuration - (System.currentTimeMillis() - start)
                delay(1000)
            }
            _remainingTime.value = 0L
            resetForgotPinFlow()
        }
    }

    fun resetForgotPinFlow() {
        _otpUiState.value = OperationStatus.Idle
        _verifyOtpUiState.value = OperationStatus.Idle
        _pinResetUiState.value = OperationStatus.Idle
    }

    fun resetPinChangeState() {
        _pinChangeState.value = OperationStatus.Idle
    }

    fun setPinResetError(message: String) {
        _pinResetUiState.value = OperationStatus.Error(message)
    }

    // =========================================================
    // SESSION MANAGEMENT
    // =========================================================

    fun loadSession() {
        viewModelScope.launch {
            val isLoggedIn = sessionManager.isLoggedIn.first()
            _isLoggedIn.value = isLoggedIn

            if (isLoggedIn) {
                val email = sessionManager.userEmail.first()
                val isAdmin = sessionManager.isAdmin.first()
                _isAdmin.value = isAdmin

                if (email.isNotBlank()) {
                    try {
                        val userEntity = employeeRepo.getEmployeeByEmail(email)
                        val user = userEntity?.toEmployee()

                        if (user != null) {
                            _currentUser.value = user
                            Log.d("Session", "✅ Session restored for user: ${user.name}, admin=$isAdmin")
                        } else {
                            Log.e("Session", "❌ Session exists for $email but user not found in DB. Forcing logout.")
                            logout()
                        }
                    } catch (e: Exception) {
                        Log.e("Session", "❌ DB error during session restore: ${e.message}. Forcing logout.")
                        logout()
                    }
                } else {
                    Log.e("Session", "❌ Invalid session state. Forcing logout.")
                    logout()
                }
            } else {
                _isAdmin.value = false
                _currentUser.value = null
                Log.d("Session", "ℹ️ No active session. App is in Guest mode.")
            }
        }
    }

    /**
     * Prevents unwanted Firebase guest auto-login
     */
    private suspend fun ensureSignedInIfNeeded() {
        val hasValidSession = sessionManager.isLoggedIn.first()
        if (!hasValidSession) {
            val user = auth.currentUser
            if (user != null) {
                Log.w("AuthCheck", "⚠️ Firebase user exists but no valid session — signing out.")
                try {
                    auth.signOut()
                    FirebaseAuth.getInstance().signOut()
                } catch (e: Exception) {
                    Log.e("AuthCheck", "❌ Failed to sign out: ${e.message}")
                }
            }
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Log.d("AuthCheck", "🔒 No Firebase user — not signing in automatically.")
            return
        }

        if (user.isAnonymous || user.email.isNullOrBlank()) {
            Log.w("AuthCheck", "⚠️ Anonymous Firebase session detected — signing out.")
            try {
                auth.signOut()
                FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                Log.e("AuthCheck", "❌ Failed to sign out anonymous user: ${e.message}")
            }
        } else {
            Log.d("AuthCheck", "✅ Valid Firebase user: ${user.email}")
        }
    }

    fun checkIfAdmin() {
        viewModelScope.launch {
            try {
                val email = _currentUser.value?.email
                if (email.isNullOrBlank()) {
                    val sessionEmail = sessionManager.userEmail.first()
                    if (sessionEmail.isNotBlank()) {
                        val user = employeeRepo.getEmployeeByEmail(sessionEmail)
                        _isAdmin.value = user?.isAdmin ?: false
                        Log.d("AdminCheck", "✅ Admin status from session: ${user?.isAdmin}")
                    } else {
                        _isAdmin.value = false
                    }
                    return@launch
                }

                val currentUser = _currentUser.value
                if (currentUser != null) {
                    _isAdmin.value = currentUser.isAdmin
                    Log.d("AdminCheck", "✅ Admin status from currentUser: ${currentUser.isAdmin}")
                } else {
                    val user = employeeRepo.getEmployeeByEmail(email)
                    _isAdmin.value = user?.isAdmin ?: false
                    Log.d("AdminCheck", "✅ Admin status from repository: ${user?.isAdmin}")
                }
            } catch (e: Exception) {
                Log.e("AdminCheck", "❌ Error checking admin status: ${e.message}")
            }
        }
    }
}


