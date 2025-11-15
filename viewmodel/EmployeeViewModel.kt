package com.example.policemobiledirectory.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.data.local.PendingRegistrationEntity
import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.ExternalLinkInfo
import com.example.policemobiledirectory.repository.EmployeeRepository
import com.example.policemobiledirectory.repository.PendingRegistrationRepository
import com.example.policemobiledirectory.utils.RepoResult
import com.example.policemobiledirectory.ui.screens.GoogleSignInUiEvent
import com.example.policemobiledirectory.ui.screens.NotificationTarget
import com.example.policemobiledirectory.utils.OperationStatus
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmployeeViewModel @Inject constructor(
    private val employeeRepo: EmployeeRepository,
    private val pendingRepo: PendingRegistrationRepository
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // -------------------- GENERIC STATE --------------------
    private fun <T> Flow<RepoResult<T>>.asOperationState(): Flow<OperationStatus<T>> = map {
        when(it){
            is RepoResult.Loading -> OperationStatus.Loading
            is RepoResult.Success -> OperationStatus.Success(it.data)
            is RepoResult.Error -> OperationStatus.Error(it.message)
        }
    }

    // -------------------- AUTH & USER --------------------
    private val _currentUser = MutableStateFlow<Employee?>(null)
    val currentUser: StateFlow<Employee?> = _currentUser.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = _currentUser.map { it != null }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _googleSignInUiEvent = MutableStateFlow<GoogleSignInUiEvent>(GoogleSignInUiEvent.Idle)
    val googleSignInUiEvent: StateFlow<GoogleSignInUiEvent> = _googleSignInUiEvent.asStateFlow()

    private val _passwordChangeState = MutableStateFlow<PasswordChangeUiState>(PasswordChangeUiState.Idle)
    val passwordChangeState: StateFlow<PasswordChangeUiState> = _passwordChangeState

    private val _authStatus = MutableStateFlow<OperationStatus<Any>>(OperationStatus.Idle)
    val authStatus: StateFlow<OperationStatus<Any>> = _authStatus.asStateFlow()

    fun loginWithPin(email: String, pin: String) = launchOperation(_authStatus) {
        employeeRepo.loginUserWithPin(email, pin)
    }

    fun loginUser(email: String, password: String) = launchOperation(_authStatus) {
        employeeRepo.loginUser(email, password)
    }

    fun handleGoogleSignIn(email: String) = viewModelScope.launch {
        _authStatus.value = OperationStatus.Loading
        employeeRepo.getUserByEmail(email).collect { result ->
            when(result){
                is RepoResult.Success -> {
                    _currentUser.value = result.data
                    _isAdmin.value = result.data?.isAdmin ?: false
                    _authStatus.value = OperationStatus.Success(result.data!!)
                }
                is RepoResult.Error -> {
                    if(result.message.contains("not found", ignoreCase = true))
                        _googleSignInUiEvent.value = GoogleSignInUiEvent.UserNotFoundInFirebase(email)
                    else
                        _googleSignInUiEvent.value = GoogleSignInUiEvent.Error(result.message)
                    _authStatus.value = OperationStatus.Error(result.message)
                }
                else -> {}
            }
        }
    }

    fun logout() = viewModelScope.launch {
        employeeRepo.logout()
        _currentUser.value = null
        _isAdmin.value = false
        _authStatus.value = OperationStatus.Idle
    }

    fun resetGoogleSignInEvent() { _googleSignInUiEvent.value = GoogleSignInUiEvent.Idle }

    // -------------------- EMPLOYEES & FILTER --------------------
    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _searchFilter = MutableStateFlow(SearchFilter.ALL)
    private val _selectedDistrict = MutableStateFlow("All")
    private val _selectedStation = MutableStateFlow("All")

    // Reactive filtered employees
    val filteredEmployees: StateFlow<List<Employee>> = combine(
        _employees, _searchQuery, _searchFilter, _selectedDistrict, _selectedStation
    ) { list, query, filter, district, station ->
        list.filter { emp ->
            (district == "All" || emp.district == district) &&
                    (station == "All" || emp.station == station) &&
                    (query.isBlank() || emp.matches(query, filter))
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        refreshEmployees()
        refreshPendingRegistrations()
    }

    // Update filters
    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateSearchFilter(filter: SearchFilter) { _searchFilter.value = filter }
    fun updateSelectedDistrict(district: String) { _selectedDistrict.value = district }
    fun updateSelectedStation(station: String) { _selectedStation.value = station }

    private fun Employee.matches(query: String, filter: SearchFilter): Boolean {
        val q = query.lowercase()
        return when(filter) {
            SearchFilter.ALL -> listOf(name, kgid, mobile1, mobile2, rank, station, district).any { it?.lowercase()?.contains(q) == true }
            SearchFilter.NAME -> name?.lowercase()?.contains(q) == true
            SearchFilter.KGID -> kgid?.lowercase()?.contains(q) == true
            SearchFilter.MOBILE -> mobile1?.contains(q) == true || mobile2?.contains(q) == true
            SearchFilter.STATION -> station?.lowercase()?.contains(q) == true
            SearchFilter.RANK, SearchFilter.RANK -> rank?.lowercase()?.contains(q) == true
            SearchFilter.METAL_NUMBER -> metalNumber?.lowercase()?.contains(q) == true
        }
    }

    fun refreshEmployees() = launchOperation(_employees) {
        employeeRepo.getEmployees().map { it.data ?: emptyList() }
    }

    fun addOrUpdateEmployee(emp: Employee) = viewModelScope.launch {
        employeeRepo.addOrUpdateEmployee(emp).collect { refreshEmployees() }
    }

    fun deleteEmployee(kgid: String) = viewModelScope.launch {
        employeeRepo.deleteEmployee(kgid).collect { refreshEmployees() }
    }

    // -------------------- PROFILE PHOTO --------------------
    private val _uploadStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val uploadStatus: StateFlow<OperationStatus<String>> = _uploadStatus.asStateFlow()

    fun uploadPhoto(uri: Uri, kgid: String) = launchOperation(_uploadStatus) {
        employeeRepo.uploadProfilePhoto(uri, kgid)
    }

    fun resetUploadStatus() { _uploadStatus.value = OperationStatus.Idle }

    // -------------------- PENDING REGISTRATIONS --------------------
    private val _pendingRegistrations = MutableStateFlow<List<PendingRegistrationEntity>>(emptyList())
    val pendingRegistrations: StateFlow<List<PendingRegistrationEntity>> = _pendingRegistrations.asStateFlow()

    private val _pendingStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val pendingStatus: StateFlow<OperationStatus<String>> = _pendingStatus.asStateFlow()

    fun refreshPendingRegistrations() = launchOperation(_pendingRegistrations) {
        pendingRepo.getPendingRegistrations().map { it.data ?: emptyList() }
    }

    fun approveRegistration(entity: PendingRegistrationEntity) = launchOperation(_pendingStatus) {
        pendingRepo.approveRegistration(entity)
    }

    fun rejectRegistration(entity: PendingRegistrationEntity, reason: String) = launchOperation(_pendingStatus) {
        pendingRepo.rejectRegistration(entity, reason)
    }

    fun resetPendingStatus() { _pendingStatus.value = OperationStatus.Idle }

    // -------------------- NOTIFICATIONS --------------------
    fun sendNotification(title: String, body: String, target: NotificationTarget, k: String?=null, d: String?=null, s: String?=null) = viewModelScope.launch {
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
        firestore.collection("notifications_queue").add(request)
            .addOnSuccessListener { _pendingStatus.value = OperationStatus.Success("Notification sent") }
            .addOnFailureListener { e -> _pendingStatus.value = OperationStatus.Error("Failed: ${e.message}") }
    }

    // -------------------- USEFUL LINKS --------------------
    private val _usefulLinks = MutableStateFlow<List<ExternalLinkInfo>>(emptyList())
    val usefulLinks: StateFlow<List<ExternalLinkInfo>> = _usefulLinks.asStateFlow()

    fun fetchUsefulLinks() = firestore.collection("useful_links").addSnapshotListener { snap, _ ->
        if (snap != null) _usefulLinks.value = snap.documents.mapNotNull { it.toObject(ExternalLinkInfo::class.java) }
    }

    fun addUsefulLink(url: String) = viewModelScope.launch {
        val pkg = Uri.parse(url).getQueryParameter("id") ?: return@launch
        val name = pkg.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        firestore.collection("useful_links").add(ExternalLinkInfo(name, pkg, url))
            .addOnSuccessListener { _pendingStatus.value = OperationStatus.Success("Link added") }
            .addOnFailureListener { e -> _pendingStatus.value = OperationStatus.Error("Failed: ${e.message}") }
    }

    // -------------------- HELPERS --------------------
    private fun <T> launchOperation(stateFlow: MutableStateFlow<OperationStatus<T>>, block: suspend () -> Flow<RepoResult<T>>) = viewModelScope.launch {
        stateFlow.value = OperationStatus.Loading
        block().asOperationState().collectLatest { stateFlow.value = it }
    }

    private fun <T> launchOperation(stateFlow: MutableStateFlow<List<T>>, block: suspend () -> Flow<List<T>>) = viewModelScope.launch {
        block().collectLatest { stateFlow.value = it }
    }
}
