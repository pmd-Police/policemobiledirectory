package com.example.policemobiledirectory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.Officer
import com.example.policemobiledirectory.repository.EmployeeRepository
import com.example.policemobiledirectory.repository.OfficerRepository
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.utils.PerformanceLogger
import com.example.policemobiledirectory.utils.SearchEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for employee and officer list operations:
 * - Employee CRUD operations
 * - Employee search and filtering
 * - Officer list management
 * - Combined contacts (employees + officers)
 */
@HiltViewModel
class EmployeeListViewModel @Inject constructor(
    private val employeeRepo: EmployeeRepository,
    private val officerRepo: OfficerRepository
) : ViewModel() {

    // Employee State
    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    private val _employeeStatus = MutableStateFlow<OperationStatus<List<Employee>>>(OperationStatus.Loading)
    val employeeStatus: StateFlow<OperationStatus<List<Employee>>> = _employeeStatus.asStateFlow()

    // Officers State (read-only contacts)
    private val _officers = MutableStateFlow<List<Officer>>(emptyList())
    val officers: StateFlow<List<Officer>> = _officers.asStateFlow()

    private val _officerStatus = MutableStateFlow<OperationStatus<List<Officer>>>(OperationStatus.Loading)
    val officerStatus: StateFlow<OperationStatus<List<Officer>>> = _officerStatus.asStateFlow()

    // Combined contacts (employees + officers) for unified search
    data class Contact(
        val employee: Employee? = null,
        val officer: Officer? = null
    ) {
        val name: String get() = employee?.name ?: officer?.name ?: ""
        val id: String get() = employee?.kgid ?: officer?.agid ?: ""
        val rank: String? get() = employee?.rank ?: officer?.rank
        val station: String? get() = employee?.station ?: officer?.station
        val district: String? get() = employee?.district ?: officer?.district
        val mobile1: String? get() = employee?.mobile1 ?: officer?.primaryPhone
        val photoUrl: String? get() = employee?.photoUrl ?: employee?.photoUrlFromGoogle ?: officer?.photoUrl
    }

    // Search and Filter State
    private val _searchQuery = MutableStateFlow("")
    private val _debouncedSearchQuery = MutableStateFlow("")
    private val _searchFilter = MutableStateFlow(SearchFilter.NAME)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    private val _selectedDistrict = MutableStateFlow("All")
    private val _selectedStation = MutableStateFlow("All")
    private val _selectedRank = MutableStateFlow("All")
    val selectedRank: StateFlow<String> = _selectedRank.asStateFlow()

    private data class SearchFilters(
        val query: String,
        val filter: SearchFilter,
        val district: String,
        val station: String,
        val rank: String
    )

    private val searchFiltersFlow = combine(
        _debouncedSearchQuery,
        _searchFilter,
        _selectedDistrict,
        _selectedStation,
        _selectedRank
    ) { query, filter, district, station, rank ->
        SearchFilters(query, filter, district, station, rank)
    }

    // Admin state (needed for filtering approved employees)
    private val _isAdmin = MutableStateFlow(false)
    fun setIsAdmin(isAdmin: Boolean) {
        _isAdmin.value = isAdmin
    }

    // Combined contacts with admin-aware filtering
    val allContacts: StateFlow<List<Contact>> = combine(_employees, _officers, _isAdmin) { employees, officers, isAdmin ->
        // Filter employees: show only approved ones for regular users, all for admins
        val filteredEmployees = if (isAdmin) {
            employees // Admins see all employees
        } else {
            employees.filter { it.isApproved } // Regular users see only approved employees
        }

        val employeeContacts = filteredEmployees.map { Contact(employee = it) }
        val officerContacts = officers.map { Contact(officer = it) }
        val allContactsList = employeeContacts + officerContacts

        Log.d("ContactsFilter", "isAdmin: $isAdmin, Total employees: ${employees.size}, Approved: ${filteredEmployees.size}, Officers: ${officers.size}, Total contacts: ${allContactsList.size}")

        allContactsList
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Enhanced search with relevance scoring and performance tracking
    val filteredContacts: StateFlow<List<Contact>> = combine(
        allContacts,
        searchFiltersFlow
    ) { contacts, filters ->
        // Early exit if no contacts
        if (contacts.isEmpty()) return@combine emptyList<Contact>()

        // Step 1: Fast pre-filtering by district/station/rank
        val preFiltered = contacts.filter { contact ->
            val districtMatch = filters.district == "All" ||
                (contact.district?.equals(filters.district, ignoreCase = true) == true)
            val stationMatch = filters.station == "All" ||
                (contact.station?.equals(filters.station, ignoreCase = true) == true)
            val rankMatch = filters.rank == "All" ||
                (contact.rank?.equals(filters.rank, ignoreCase = true) == true)

            districtMatch && stationMatch && rankMatch
        }

        // Early exit if no matches after pre-filtering
        if (preFiltered.isEmpty()) return@combine emptyList<Contact>()

        // Step 2: Enhanced text search with relevance scoring
        if (filters.query.isBlank()) {
            return@combine preFiltered
        }

        val queryLower = filters.query.lowercase().trim()
        if (queryLower.isEmpty()) return@combine preFiltered

        // Use enhanced search engine with performance tracking
        val startTime = System.currentTimeMillis()
        
        // Use enhanced search engine for employees
        val employeeContacts = preFiltered.filter { it.employee != null }
        val officerContacts = preFiltered.filter { it.officer != null }

        val employeeResults = if (employeeContacts.isNotEmpty()) {
            val employees = employeeContacts.mapNotNull { it.employee }
            SearchEngine.searchEmployees(employees, queryLower, filters.filter)
                .map { SearchEngine.SearchResult(Contact(employee = it.item), it.score, it.matchedFields) }
        } else emptyList()

        val officerResults = if (officerContacts.isNotEmpty()) {
            val officers = officerContacts.mapNotNull { it.officer }
            val filterString = when (filters.filter) {
                SearchFilter.NAME -> "name"
                SearchFilter.KGID -> "agid"
                SearchFilter.MOBILE -> "mobile"
                SearchFilter.STATION -> "station"
                SearchFilter.RANK -> "rank"
                SearchFilter.METAL_NUMBER -> "" // Officers don't have metal numbers
            }
            if (filterString.isNotBlank()) {
                SearchEngine.searchOfficers(officers, queryLower, filterString)
                    .map { SearchEngine.SearchResult(Contact(officer = it.item), it.score, it.matchedFields) }
            } else emptyList()
        } else emptyList()

        // Log performance
        val duration = System.currentTimeMillis() - startTime
        if (duration > 100) { // Only log if search takes > 100ms
            Log.d("SearchPerformance", "Search took ${duration}ms for query: $queryLower, results: ${employeeResults.size + officerResults.size}")
        }

        // Combine and sort by relevance score
        (employeeResults + officerResults)
            .sortedByDescending { it.score }
            .map { it.item }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Filtered employees (admin-aware)
    val filteredEmployees: StateFlow<List<Employee>> = combine(_employees, searchFiltersFlow, _isAdmin) { employees, filters, isAdmin ->
        // Early exit if no employees
        if (employees.isEmpty()) return@combine emptyList<Employee>()

        // Filter by approval status first
        val approvedEmployees = if (isAdmin) {
            employees // Admins see all employees
        } else {
            employees.filter { it.isApproved } // Regular users see only approved employees
        }

        // Step 1: Fast pre-filtering by district/station/rank
        val preFiltered = approvedEmployees.filter { emp ->
            (filters.district == "All" || emp.district == filters.district) &&
            (filters.station == "All" || emp.station == filters.station) &&
            (filters.rank == "All" || emp.rank == filters.rank)
        }

        // Early exit if no matches after pre-filtering
        if (preFiltered.isEmpty()) return@combine emptyList<Employee>()

        // Step 2: Text search only on pre-filtered results
        if (filters.query.isBlank()) {
            return@combine preFiltered
        }

        val queryLower = filters.query.lowercase().trim()
        if (queryLower.isEmpty()) return@combine preFiltered

        preFiltered.filter { emp ->
            emp.matchesOptimized(queryLower, filters.filter)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Debounce search query (300ms) to avoid searching on every keystroke
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .collect { query ->
                    _debouncedSearchQuery.value = query
                }
        }
    }

    // =========================================================
    // EMPLOYEE OPERATIONS
    // =========================================================

    fun refreshEmployees() = viewModelScope.launch {
        _employeeStatus.value = OperationStatus.Loading
        try {
            PerformanceLogger.measureDatabaseOperation("employees", "refresh") {
                employeeRepo.refreshEmployees()
                val result = employeeRepo.getEmployees()
                    .filterNot { it is RepoResult.Loading }
                    .firstOrNull()
                when (result) {
                    is RepoResult.Success -> {
                        val list = result.data ?: emptyList()
                        _employees.value = list
                        _employeeStatus.value = OperationStatus.Success(list)
                    }
                    is RepoResult.Error -> _employeeStatus.value = OperationStatus.Error(result.message ?: "Failed to load employees")
                    else -> _employeeStatus.value = OperationStatus.Error("Failed to load employees")
                }
            }
        } catch (e: Exception) {
            _employeeStatus.value = OperationStatus.Error("Refresh failed: ${e.message}")
        }
    }

    fun addOrUpdateEmployee(emp: Employee) = viewModelScope.launch {
        employeeRepo.addOrUpdateEmployee(emp).collect {
            refreshEmployees()
        }
    }

    fun deleteEmployee(kgid: String) = viewModelScope.launch {
        Log.d("DeleteEmployee", "Deleting employee $kgid...")
        employeeRepo.deleteEmployee(kgid).collect {
            refreshEmployees()
        }
    }

    // =========================================================
    // OFFICER OPERATIONS
    // =========================================================

    fun refreshOfficers() = viewModelScope.launch {
        _officerStatus.value = OperationStatus.Loading
        try {
            officerRepo.getOfficers().collect { result ->
                when (result) {
                    is RepoResult.Success -> {
                        val list = result.data ?: emptyList()
                        _officers.value = list
                        _officerStatus.value = OperationStatus.Success(list)
                    }
                    is RepoResult.Error -> {
                        _officerStatus.value = OperationStatus.Error(result.message ?: "Failed to load officers")
                    }
                    is RepoResult.Loading -> {
                        _officerStatus.value = OperationStatus.Loading
                    }
                }
            }
        } catch (e: Exception) {
            _officerStatus.value = OperationStatus.Error("Refresh failed: ${e.message}")
        }
    }

    // =========================================================
    // SEARCH AND FILTER OPERATIONS
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

    fun updateSelectedRank(rank: String) {
        _selectedRank.value = rank
    }

    // =========================================================
    // HELPER FUNCTIONS
    // =========================================================

    // Optimized matching function (query is already lowercase)
    private fun Employee.matchesOptimized(queryLower: String, filter: SearchFilter): Boolean {
        return when (filter) {
            SearchFilter.NAME -> {
                val nameLower = name.lowercase()
                nameLower.startsWith(queryLower) || nameLower.contains(queryLower)
            }
            SearchFilter.KGID -> {
                val kgidLower = kgid.lowercase()
                kgidLower.startsWith(queryLower) || kgidLower.contains(queryLower)
            }
            SearchFilter.MOBILE -> {
                mobile1?.contains(queryLower) == true || mobile2?.contains(queryLower) == true
            }
            SearchFilter.STATION -> {
                station?.lowercase()?.contains(queryLower) == true
            }
            SearchFilter.RANK -> {
                rank?.lowercase()?.contains(queryLower) == true
            }
            SearchFilter.METAL_NUMBER -> {
                metalNumber?.lowercase()?.contains(queryLower) == true
            }
        }
    }

}



