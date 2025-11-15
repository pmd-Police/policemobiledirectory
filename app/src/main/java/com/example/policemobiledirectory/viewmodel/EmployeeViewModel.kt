package com.example.policemobiledirectory.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.data.local.PendingRegistrationEntity
import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.data.local.SessionManager
import com.example.policemobiledirectory.data.mapper.toEmployee
import com.example.policemobiledirectory.repository.*
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.ExternalLinkInfo
import com.example.policemobiledirectory.ui.screens.GoogleSignInUiEvent
import com.example.policemobiledirectory.model.NotificationTarget
import com.example.policemobiledirectory.model.AppNotification
import com.example.policemobiledirectory.utils.OperationStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.text.contains
import com.example.policemobiledirectory.repository.EmployeeRepository
import com.example.policemobiledirectory.repository.PendingRegistrationRepository
import com.example.policemobiledirectory.repository.ConstantsRepository
import com.example.policemobiledirectory.repository.ImageRepository
import com.example.policemobiledirectory.repository.ImageUploadRepository
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.repository.AppIconRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@HiltViewModel
open class EmployeeViewModel @Inject constructor(
    private val employeeRepo: EmployeeRepository,
    private val pendingRepo: PendingRegistrationRepository,
    private val sessionManager: SessionManager,
    private val constantsRepository: ConstantsRepository,
    private val imageRepo: ImageRepository,
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val appIconRepository by lazy { AppIconRepository.create(context) }

    // (All your StateFlows are correctly defined here)
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
    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()
    private val _employeeStatus = MutableStateFlow<OperationStatus<List<Employee>>>(OperationStatus.Loading)
    val employeeStatus: StateFlow<OperationStatus<List<Employee>>> = _employeeStatus.asStateFlow()
    private val _searchQuery = MutableStateFlow("")
    private val _searchFilter = MutableStateFlow(SearchFilter.NAME)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()
    private val _selectedDistrict = MutableStateFlow("All")
    private val _selectedStation = MutableStateFlow("All")
    private val _adminNotifications = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val adminNotifications = _adminNotifications.asStateFlow()
    private val _userNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val userNotifications: StateFlow<List<AppNotification>> = _userNotifications.asStateFlow()

    val filteredEmployees: StateFlow<List<Employee>> = combine(
        _employees, _searchQuery, _searchFilter, _selectedDistrict, _selectedStation
    ) { list, query, filter, district, station ->
        list.filter { emp ->
            (district == "All" || emp.district == district) &&
                    (station == "All" || emp.station == station) &&
                    (query.isBlank() || emp.matches(query, filter)) // ✅ fixed here
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _uploadStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val uploadStatus: StateFlow<OperationStatus<String>> = _uploadStatus.asStateFlow()
    private val _pendingRegistrations = MutableStateFlow<List<PendingRegistrationEntity>>(emptyList())
    val pendingRegistrations: StateFlow<List<PendingRegistrationEntity>> = _pendingRegistrations.asStateFlow()
    private val _pendingStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val pendingStatus: StateFlow<OperationStatus<String>> = _pendingStatus.asStateFlow()
    private val _usefulLinks = MutableStateFlow<List<ExternalLinkInfo>>(emptyList())
    val usefulLinks: StateFlow<List<ExternalLinkInfo>> = _usefulLinks.asStateFlow()
    private val _operationResult = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val operationResult: StateFlow<OperationStatus<String>> = _operationResult.asStateFlow()
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme
    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private var userNotificationsListener: ListenerRegistration? = null
    private var userNotificationsListenerKgid: String? = null


    init {
        Log.d("EmployeeVM", "🟢 ViewModel initialized")

        loadSession()
        // 1️⃣ Sync constants once
        viewModelScope.launch {
            val success = constantsRepository.refreshConstants()
            if (success) Log.d("EmployeeVM", "✅ Constants synced")
            else Log.e("EmployeeVM", "⚠️ Constants sync failed")
        }

        // 2️⃣ Observe login state from DataStore
        viewModelScope.launch {
            sessionManager.isLoggedIn.collect { loggedIn ->
                _isLoggedIn.value = loggedIn
                Log.d("Session", "🔄 isLoggedIn = $loggedIn")
            }
        }

        // 3️⃣ Observe admin flag from DataStore
        viewModelScope.launch {
            sessionManager.isAdmin.collect { isAdmin ->
                _isAdmin.value = isAdmin
                Log.d("Session", "🔄 isAdmin = $isAdmin")
            }
        }

        // 4️⃣ Restore current user session from Room or Firestore
        viewModelScope.launch {
            sessionManager.userEmail.collect { email ->
                // ✅ Only restore if we're not in the middle of a logout
                // Check if isLoggedIn is already false (indicating logout in progress)
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

                    // Refresh employees after user restore
                    refreshEmployees()
                } else {
                    Log.d("Session", "🔒 No stored email — user not logged in")
                    // Only clear if not already cleared (avoid unnecessary updates)
                    if (_currentUser.value != null || _isLoggedIn.value == true) {
                        _currentUser.value = null
                        _isAdmin.value = false
                        _isLoggedIn.value = false
                    }
                }
            }
        }

        // 5️⃣ Startup data prefetch
        viewModelScope.launch {
            try {
                ensureSignedInIfNeeded()
                // Only fetch pending registrations if user is admin and logged in
                // Wait for admin status to be determined (check once)
                val isAdmin = _isAdmin.first()
                if (isAdmin && _isLoggedIn.first()) {
                    try {
                        refreshPendingRegistrations()
                    } catch (e: Exception) {
                        // Silently handle permission errors - non-admins don't need pending registrations
                        Log.d("PendingReg", "Could not load pending registrations: ${e.message}")
                        // Reset status to Idle so errors don't persist
                        _pendingStatus.value = OperationStatus.Idle
                    }
                }
            } catch (e: Exception) {
                Log.e("Startup", "Startup failed: ${e.message}", e)
            }
        }

        viewModelScope.launch {
            currentUser.collectLatest { user ->
                updateUserNotificationListener(user)
            }
        }
    }


    // =========================================================
    // AUTHENTICATION (LOGIN, GOOGLE SIGN-IN, LOGOUT)
    // =========================================================
    fun loginWithPin(email: String, pin: String) {
        viewModelScope.launch {
            employeeRepo.loginUser(email, pin).collect { result ->
                when (result) {
                    is RepoResult.Success -> {
                        val user = result.data
                        if (user != null) {
                            // ✅ Instantly update UI before waiting for DataStore
                            _currentUser.value = user
                            _isAdmin.value = user.isAdmin
                            _isLoggedIn.value = true

                            // ✅ Save to DataStore for persistence
                            sessionManager.saveLogin(email, user.isAdmin)

                            // ✅ Fetch a fresh version from local DB (ensures latest info)
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

    //show real-time admin alert

    fun fetchAdminNotifications() {
        firestore.collection("admin_notifications")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("AdminNotifications", "❌ Failed to fetch: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _adminNotifications.value = snapshot.documents.mapNotNull { it.data }
                }
            }
    }

    private fun updateUserNotificationListener(user: Employee?) {
        val kgid = user?.kgid

        if (user?.isAdmin == true) {
            userNotificationsListener?.remove()
            userNotificationsListener = null
            userNotificationsListenerKgid = null
            _userNotifications.value = emptyList()
            return
        }

        if (userNotificationsListenerKgid == kgid) return

        userNotificationsListener?.remove()
        userNotificationsListener = null
        userNotificationsListenerKgid = kgid

        if (user == null || kgid.isNullOrBlank()) {
            _userNotifications.value = emptyList()
            return
        }

        userNotificationsListener = firestore.collection("notifications_queue")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("UserNotifications", "❌ Failed to fetch: ${e.message}")
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: return@addSnapshotListener
                val notifications = docs.mapNotNull { doc ->
                    val notification = doc.data?.toAppNotification(doc.id) ?: return@mapNotNull null
                    if (shouldDeliverNotification(notification, user)) notification else null
                }
                _userNotifications.value = notifications
            }
    }

    private fun shouldDeliverNotification(notification: AppNotification, user: Employee): Boolean {
        fun matches(lhs: String?, rhs: String?): Boolean =
            lhs != null && rhs != null && lhs.equals(rhs, ignoreCase = true)

        return when (notification.targetType) {
            NotificationTarget.ALL -> true
            NotificationTarget.SINGLE -> matches(notification.targetKgid, user.kgid)
            NotificationTarget.DISTRICT -> matches(notification.targetDistrict, user.district)
            NotificationTarget.STATION -> matches(notification.targetDistrict, user.district) &&
                    matches(notification.targetStation, user.station)
            NotificationTarget.ADMIN -> user.isAdmin
        }
    }

    private fun Map<String, Any>.toAppNotification(id: String): AppNotification? {
        val title = this["title"] as? String ?: "Notification"
        val body = this["body"] as? String ?: "You have a new message."
        val timestamp = (this["timestamp"] as? Number)?.toLong()
        val targetType = (this["targetType"] as? String)?.runCatching {
            NotificationTarget.valueOf(this.uppercase())
        }?.getOrNull() ?: NotificationTarget.ALL
        val targetKgid = this["targetKgid"] as? String
        val targetDistrict = this["targetDistrict"] as? String
        val targetStation = this["targetStation"] as? String

        return AppNotification(
            id = id,
            title = title,
            body = body,
            timestamp = timestamp,
            targetType = targetType,
            targetKgid = targetKgid,
            targetDistrict = targetDistrict,
            targetStation = targetStation
        )
    }

    fun logout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                Log.d("Logout", "🚪 Starting logout...")

                // 1️⃣ Clear local session FIRST to prevent observers from triggering
                sessionManager.clearSession()
                
                // 2️⃣ Reset in-memory session IMMEDIATELY
                _isLoggedIn.value = false
                _isAdmin.value = false
                _currentUser.value = null

                // 5️⃣ Reset auth/UI state so login screen doesn't re-trigger stale events
                _authStatus.value = OperationStatus.Idle
                _googleSignInUiEvent.value = GoogleSignInUiEvent.Idle

                // 3️⃣ Sign out of Firebase (including any anonymous sessions)
                FirebaseAuth.getInstance().signOut()
                auth.signOut()
                
                // 4️⃣ Clear repository data
                employeeRepo.logout()

                Log.d("Logout", "✅ Logout complete, no anonymous re-login")

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

    fun uploadGalleryImage(uri: Uri, context: Context, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val downloadUrl = com.example.policemobiledirectory.helper.FirebaseStorageHelper.uploadPhoto(uri)

                // Optionally save to Firestore
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val data = hashMapOf(
                    "imageUrl" to downloadUrl,
                    "uploadedAt" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("gallery").add(data)

                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    // =========================================================
// OTP / PIN FLOW  (Secure Version)
// =========================================================
    fun sendOtp(email: String) {
        viewModelScope.launch {
            Log.d("ForgotPinFlow", "🟢 sendOtp() for $email")
            _otpUiState.value = OperationStatus.Loading

            try {
                // ✅ 1. Check if email exists and is approved before sending OTP
                val employee = employeeRepo.getEmployeeByEmail(email)

                if (employee == null) {
                    _otpUiState.value = OperationStatus.Error("❌ No account found with this email.")
                    return@launch
                }

                if (!employee.isApproved) {
                    _otpUiState.value = OperationStatus.Error("⚠️ Account not approved yet. Please wait for admin approval.")
                    return@launch
                }

                // ✅ 2. Proceed with OTP if approved
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

    /**
     * Loads the user session from SessionManager.
     * If a valid session exists, it fetches user details and refreshes data.
     * If not, it ensures the app is in a clean, logged-out state.
     */
    fun loadSession() {
        viewModelScope.launch {
            // First, get the logged-in status.
            val isLoggedIn = sessionManager.isLoggedIn.first()
            _isLoggedIn.value = isLoggedIn

            if (isLoggedIn) {
                // If logged in, get the email and admin status.
                val email = sessionManager.userEmail.first()
                val isAdmin = sessionManager.isAdmin.first()
                _isAdmin.value = isAdmin

                if (email.isNotBlank()) {
                    try {
                        // Fetch the full user object from the repository.
                        val userEntity = employeeRepo.getEmployeeByEmail(email)
                        val user = userEntity?.toEmployee()

                        if (user != null) {
                            _currentUser.value = user
                            Log.d("Session", "✅ Session restored for user: ${user.name}, admin=$isAdmin")
                            // Refresh data now that we have a valid user.
                            refreshEmployees()
                            // Only fetch pending registrations if user is admin
                            if (isAdmin) {
                                refreshPendingRegistrations()
                            }
                        } else {
                            // Data is inconsistent (session exists but user not in DB).
                            // This is a failure case, so log out.
                            Log.e("Session", "❌ Session exists for $email but user not found in DB. Forcing logout.")
                            logout()
                        }
                    } catch (e: Exception) {
                        Log.e("Session", "❌ DB error during session restore: ${e.message}. Forcing logout.")
                        logout()
                    }
                } else {
                    // Session is invalid (isLoggedIn=true but no email). Force logout.
                    Log.e("Session", "❌ Invalid session state. Forcing logout.")
                    logout()
                }
            } else {
                // Not logged in. Ensure all states are clean.
                _isAdmin.value = false
                _currentUser.value = null
                Log.d("Session", "ℹ️ No active session. App is in Guest mode.")
            }
        }
    }

    private fun Employee.matches(query: String, filter: SearchFilter): Boolean {
        // ✅ FIX: Use lowercase() for case-insensitive comparison
        val q = query.lowercase()
        return when (filter) {
            SearchFilter.NAME -> name.lowercase().contains(q)
            SearchFilter.KGID -> kgid.lowercase().contains(q)
            SearchFilter.MOBILE -> mobile1?.contains(q) == true || mobile2?.contains(q) == true
            SearchFilter.STATION -> station?.lowercase()?.contains(q) == true
            SearchFilter.RANK -> rank?.lowercase()?.contains(q) == true
            SearchFilter.METAL_NUMBER -> metalNumber?.lowercase()?.contains(q) == true
        }
    }


    // =========================================================
    // EMPLOYEE CRUD + HELPERS
    // =========================================================
    fun refreshEmployees() = viewModelScope.launch {
        try {
            employeeRepo.refreshEmployees()
            employeeRepo.getEmployees().collectLatest { result ->
                when (result) {
                    is RepoResult.Loading -> _employeeStatus.value = OperationStatus.Loading
                    is RepoResult.Success -> {
                        val list = result.data ?: emptyList()
                        _employees.value = list
                        _employeeStatus.value = OperationStatus.Success(list)
                    }
                    is RepoResult.Error -> _employeeStatus.value = OperationStatus.Error(result.message ?: "Failed to load employees")
                }
            }
        } catch (e: Exception) {
            _employeeStatus.value = OperationStatus.Error("Refresh failed: ${e.message}")
        }
    }

    fun addOrUpdateEmployee(emp: Employee) = viewModelScope.launch {
        employeeRepo.addOrUpdateEmployee(emp).collect { refreshEmployees() }
    }

    fun deleteEmployee(kgid: String, photoUrl: String?) = viewModelScope.launch {
        Log.d("DeleteEmployee", "Deleting employee $kgid...")

        // 1️⃣ Delete from Google Sheet + Room
        employeeRepo.deleteEmployee(kgid).collect {
            refreshEmployees()
        }

        // 2️⃣ Delete Drive photo (if available)
        photoUrl?.let { url ->
            val fileId = url.substringAfter("id=").substringBefore("&")
            Log.d("DeleteEmployee", "Attempting to delete image ID: $fileId")

            imageRepo.deleteOfficerImage(fileId, kgid).collect { status ->
                when (status) {
                    is OperationStatus.Idle -> {
                        Log.d("DriveDelete", "Idle — no operation started yet.")
                    }

                    is OperationStatus.Loading -> {
                        Log.d("DriveDelete", "Deleting image from Google Drive...")
                    }

                    is OperationStatus.Success -> {
                        Log.d("DriveDelete", status.data ?: "✅ Image deleted from Drive successfully.")
                    }

                    is OperationStatus.Error -> {
                        Log.e("DriveDelete", "❌ Drive deletion failed: ${status.message}")
                    }
                }
            }
        }
    }


    // ✅ FIX: ALL FUNCTIONS ARE NOW CORRECTLY PLACED AT THE TOP LEVEL OF THE CLASS
    // =========================================================
    // UI + MISC
    // =========================================================
    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    /**
     * ✅ Prevents unwanted Firebase guest auto-login
     */
    private suspend fun ensureSignedInIfNeeded() {
        // ✅ Only check if we have a valid session in DataStore
        val hasValidSession = sessionManager.isLoggedIn.first()
        if (!hasValidSession) {
            // No valid session, ensure Firebase is signed out
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




    private suspend fun isEmailApproved(email: String): Boolean {
        // Prefer repository-level check (fast), but fallback to Firestore if needed.
        val emp = employeeRepo.getEmployeeByEmail(email) // returns entity or null
        return emp?.isApproved == true || emp?.toEmployee()?.isAdmin == true // adjust fields as per your model
    }



    // =========================================================
//  PENDING REGISTRATIONS (Final + Corrected)
// =========================================================

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
                    // Only set error status if it's not a permission issue (permission errors are handled silently)
                    if (errorMsg.contains("Permission", ignoreCase = true) || 
                        errorMsg.contains("permission denied", ignoreCase = true)) {
                        // Silently handle permission errors - reset to Idle
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

    // =========================================================
//  NEW USER REGISTRATION (Pending Approval + Admin Notification)
// =========================================================
    fun registerNewUser(entity: PendingRegistrationEntity) {
        viewModelScope.launch {
            _pendingStatus.value = OperationStatus.Loading

            try {
                // 1️⃣ Check for duplicate registration directly in Firestore (more reliable than cached list)
                val hasDuplicate = try {
                    // Check by KGID
                    val kgidSnapshot = firestore.collection("pending_registrations")
                        .whereEqualTo("status", "pending")
                        .whereEqualTo("kgid", entity.kgid)
                        .limit(1)
                        .get()
                        .await()
                    
                    if (!kgidSnapshot.isEmpty) {
                        true // Duplicate found by KGID
                    } else {
                        // Also check by email
                        val emailSnapshot = firestore.collection("pending_registrations")
                            .whereEqualTo("status", "pending")
                            .whereEqualTo("email", entity.email)
                            .limit(1)
                            .get()
                            .await()
                        !emailSnapshot.isEmpty // true if duplicate found
                    }
                } catch (e: Exception) {
                    Log.w("RegisterUser", "Duplicate check failed, proceeding anyway: ${e.message}")
                    false // Allow registration if check fails
                }

                if (hasDuplicate) {
                    _pendingStatus.value = OperationStatus.Error(
                        "A registration for this KGID/Email already exists and is pending approval."
                    )
                    return@launch
                }

                // 2️⃣ Prepare safe PendingRegistration object
                val pending = entity.copy(
                    isApproved = false,
                    firebaseUid = entity.firebaseUid.takeIf { it.isNotBlank() } ?: "",
                    status = "pending",
                    rejectionReason = null,
                    photoUrlFromGoogle = null
                )

                // 3️⃣ Submit to Firestore + Room
                pendingRepo.addPendingRegistration(pending).collect { result ->
                    when (result) {
                        is RepoResult.Loading ->
                            _pendingStatus.value = OperationStatus.Loading

                        is RepoResult.Success -> {
                            _pendingStatus.value =
                                OperationStatus.Success("Registration submitted for admin approval.")

                            // Refresh UI (only if admin)
                            if (_isAdmin.value) {
                                refreshPendingRegistrations()
                            }

                            // 4️⃣ Notify admin (don't wait for completion, send in background)
                            viewModelScope.launch {
                                try {
                                    sendNotification(
                                        title = "New User Registration Pending",
                                        body = "New registration from ${entity.name} (${entity.email}) awaiting approval.",
                                        target = NotificationTarget.ADMIN,
                                        d = entity.district,
                                        s = entity.station
                                    )
                                } catch (e: Exception) {
                                    Log.e("RegisterUser", "Failed to send notification: ${e.message}")
                                    // Don't fail registration if notification fails
                                }
                            }
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

    fun resetPendingStatus() {
        _pendingStatus.value = OperationStatus.Idle
    }

    // =========================================================
    //  USEFUL LINKS & NOTIFICATIONS
    // =========================================================
    fun fetchUsefulLinks() {
        viewModelScope.launch {
            try {
                val collection = firestore.collection("useful_links")
                val snapshot = collection.get().await()
                val links = snapshot.documents.mapNotNull { doc ->
                    val link = doc.toObject(ExternalLinkInfo::class.java) ?: return@mapNotNull null

                    val resolvedIcon = when {
                        link.iconUrl.isNullOrBlank() && link.playStoreUrl.isNotBlank() -> {
                            try {
                                val fetched = appIconRepository.getOrFetchAppIcon(link.playStoreUrl)
                                if (!fetched.isNullOrBlank()) {
                                    try {
                                        collection.document(doc.id).update("iconUrl", fetched).await()
                                    } catch (updateError: Exception) {
                                        Log.w(
                                            "UsefulLinks",
                                            "⚠️ Failed to persist icon for ${link.name}: ${updateError.message}"
                                        )
                                    }
                                }
                                fetched
                            } catch (fetchError: Exception) {
                                Log.e(
                                    "UsefulLinks",
                                    "❌ Icon fetch failed for ${link.name}: ${fetchError.message}"
                                )
                                null
                            }
                        }

                        else -> link.iconUrl
                    }

                    link.copy(iconUrl = resolvedIcon)
                }

                _usefulLinks.value = links
            } catch (e: Exception) {
                Log.e("Firestore", "❌ Failed to fetch useful links: ${e.message}", e)
                _usefulLinks.value = emptyList()
            }
        }
    }

    fun sendNotification(
        title: String,
        body: String,
        target: NotificationTarget,
        k: String? = null,
        d: String? = null,
        s: String? = null
    ) = viewModelScope.launch {
        val request = hashMapOf(
            "title" to title,
            "body" to body,
            "targetType" to target.name,
            "targetKgid" to k?.takeIf { it.isNotBlank() },
            "targetDistrict" to d?.takeIf { it != "All" },
            "targetStation" to s?.takeIf { it != "All" },
            "timestamp" to System.currentTimeMillis(),
            "requesterKgid" to (_currentUser.value?.kgid ?: "unknown")
        )

        // ✅ Separate collection for admin notifications
        val collectionName = if (target == NotificationTarget.ADMIN)
            "admin_notifications"
        else
            "notifications_queue"

        firestore.collection(collectionName)
            .add(request)
            .addOnSuccessListener {
                _pendingStatus.value = OperationStatus.Success("Notification sent successfully.")
            }
            .addOnFailureListener { e ->
                _pendingStatus.value = OperationStatus.Error("Failed: ${e.message}")
            }
    }


    // =========================================================
    //  FILE UPLOADS
    // =========================================================
    fun uploadPhoto(uri: Uri, kgid: String) = viewModelScope.launch {
        imageRepo.uploadOfficerImage(uri, kgid).collect { status ->
            _uploadStatus.value = status
        }
    }

    // =========================================================
    //  UI CONTROLS
    // =========================================================
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSearchFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }

    fun updateSelectedDistrict(district: String) {
        _selectedDistrict.value = district
    }

    fun updateSelectedStation(station: String) {
        _selectedStation.value = station
    }

    fun adjustFontScale(increase: Boolean) {
        val step = 0.1f
        val current = _fontScale.value
        _fontScale.value = when {
            increase -> (current + step).coerceAtMost(1.8f)
            else -> (current - step).coerceAtLeast(0.8f)
        }
    }
    
    fun setFontScale(scale: Float) {
        _fontScale.value = scale.coerceIn(0.8f, 1.8f)
    }

    // =========================================================
    // ADMIN CHECK
    // =========================================================
    fun checkIfAdmin() {
        val user = auth.currentUser ?: run {
            _isAdmin.value = false
            return
        }
        viewModelScope.launch {
            try {
                val doc = firestore.collection("employees").document(user.uid).get().await()
                _isAdmin.value = doc.exists() && (doc.getBoolean("isAdmin") == true)
            } catch (e: Exception) {
                Log.e("AdminCheck", "❌ Error: ${e.message}")
            }
        }
    }

    // This generic helper can be used if needed, but isn't strictly necessary with the current implementations
    private fun <T> launchOperationForResult(stateFlow: MutableStateFlow<OperationStatus<T>>, block: suspend () -> Flow<RepoResult<T>>) = viewModelScope.launch {
        stateFlow.value = OperationStatus.Loading
        block().collectLatest { result ->
            when (result) {
                is RepoResult.Loading -> stateFlow.value = OperationStatus.Loading
                is RepoResult.Success -> stateFlow.value = OperationStatus.Success(result.data ?: return@collectLatest)
                is RepoResult.Error -> stateFlow.value = OperationStatus.Error(result.message ?: "Unknown error")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userNotificationsListener?.remove()
        userNotificationsListener = null
    }
}
