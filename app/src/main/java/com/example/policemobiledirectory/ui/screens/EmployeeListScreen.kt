@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.example.policemobiledirectory.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextAlign
import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.ui.theme.*
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import kotlinx.coroutines.launch
import com.example.policemobiledirectory.ui.components.EmployeeCardAdmin
import com.example.policemobiledirectory.ui.components.EmployeeCardUser
import kotlinx.coroutines.CoroutineScope
import com.example.policemobiledirectory.navigation.Routes
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.TextFields

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListScreen(
    navController: NavController,
    viewModel: EmployeeViewModel,
    onThemeToggle: () -> Unit
) {
    val context = LocalContext.current
    val filteredEmployees by viewModel.filteredEmployees.collectAsStateWithLifecycle()
    val employeeStatus by viewModel.employeeStatus.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // Notification counts
    val userNotifications by viewModel.userNotifications.collectAsStateWithLifecycle()
    val adminNotifications by viewModel.adminNotifications.collectAsStateWithLifecycle()
    val userNotificationsSeenAt by viewModel.userNotificationsLastSeen.collectAsStateWithLifecycle()
    val adminNotificationsSeenAt by viewModel.adminNotificationsLastSeen.collectAsStateWithLifecycle()
    val notificationCount = if (isAdmin) {
        adminNotifications.count { (it.timestamp ?: 0L) > adminNotificationsSeenAt }
    } else {
        userNotifications.count { (it.timestamp ?: 0L) > userNotificationsSeenAt }
    }

    LaunchedEffect(Unit) { viewModel.checkIfAdmin() }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PMD Home")
                        Spacer(Modifier.width(6.dp))
                        Box {
                            IconButton(onClick = { navController.navigate(Routes.NOTIFICATIONS) }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications"
                                )
                            }
                            // Notification badge
                            if (notificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .offset(x = 12.dp, y = (-12).dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.error,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshEmployees() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    
                    // Single Font Size Button with Dropdown Menu
                    FontSizeSelectorButton(
                        currentFontScale = fontScale,
                        onFontScaleSelected = { scale ->
                            viewModel.setFontScale(scale)
                        },
                        onFontScaleToggle = {
                            // Cycle through common presets: 0.8, 1.0, 1.2, 1.4, 1.6, 1.8
                            val presets = listOf(0.8f, 1.0f, 1.2f, 1.4f, 1.6f, 1.8f)
                            val current = fontScale
                            val currentIndex = presets.indexOfFirst { 
                                kotlin.math.abs(it - current) < 0.05f 
                            }
                            val nextIndex = if (currentIndex >= 0 && currentIndex < presets.size - 1) {
                                currentIndex + 1
                            } else {
                                0 // Cycle back to first
                            }
                            viewModel.setFontScale(presets[nextIndex])
                        }
                    )
                    
                    IconButton(onClick = onThemeToggle) {
                        Icon(Icons.Default.Brightness6, contentDescription = "Toggle Theme")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { navController.navigate("${Routes.ADD_EMPLOYEE}?employeeId=") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Employee")
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            EmployeeListContent(
                navController = navController,
                viewModel = viewModel,
                context = context,
                isAdmin = isAdmin,
                fontScale = fontScale,
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope
            )
        }
    }
}

@Composable
private fun EmployeeListContent(
    navController: NavController,
    viewModel: EmployeeViewModel,
    context: Context,
    isAdmin: Boolean,
    fontScale: Float,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    val filteredEmployees by viewModel.filteredEmployees.collectAsStateWithLifecycle()
    val employeeStatus by viewModel.employeeStatus.collectAsStateWithLifecycle()
    val searchFilter by viewModel.searchFilter.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedDistrict by remember { mutableStateOf("All") }
    var districtExpanded by remember { mutableStateOf(false) }
    val districts = listOf("All") + Constants.districtsList

    var selectedStation by remember { mutableStateOf("All") }
    var stationExpanded by remember { mutableStateOf(false) }
    val stationsForDistrict = remember(selectedDistrict) {
        if (selectedDistrict == "All") listOf("All")
        else listOf("All") + (Constants.stationsByDistrictMap[selectedDistrict] ?: emptyList())
    }

    val searchFields = SearchFilter.values().toList()
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // 🔸 DISTRICT & STATION DROPDOWNS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // District Dropdown
            ExposedDropdownMenuBox(
                expanded = districtExpanded,
                onExpandedChange = { districtExpanded = !districtExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedDistrict,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("District/Unit") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = districtExpanded,
                    onDismissRequest = { districtExpanded = false }
                ) {
                    districts.forEach { district ->
                        DropdownMenuItem(
                            text = { Text(district) },
                            onClick = {
                                selectedDistrict = district
                                selectedStation = "All"
                                districtExpanded = false
                                viewModel.updateSelectedDistrict(district)
                                viewModel.updateSelectedStation("All")
                            }
                        )
                    }
                }
            }

            // Station Dropdown
            ExposedDropdownMenuBox(
                expanded = stationExpanded,
                onExpandedChange = {
                    if (selectedDistrict != "All") stationExpanded = !stationExpanded
                },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedStation,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Station/Unit") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationExpanded)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    enabled = selectedDistrict != "All"
                )
                ExposedDropdownMenu(
                    expanded = stationExpanded,
                    onDismissRequest = { stationExpanded = false }
                ) {
                    stationsForDistrict.forEach { station ->
                        DropdownMenuItem(
                            text = { Text(station) },
                            onClick = {
                                selectedStation = station
                                stationExpanded = false
                                viewModel.updateSelectedStation(station)
                            }
                        )
                    }
                }
            }
        }

        // 🔹 SEARCH BAR
        val searchLabel = when (searchFilter) {
            SearchFilter.METAL_NUMBER -> "Metal"
            SearchFilter.KGID -> "KGID"
            SearchFilter.MOBILE -> "Mobile"
            SearchFilter.STATION -> "Station"
            SearchFilter.RANK -> "Rank"
            SearchFilter.NAME -> "Name"
            else -> searchFilter.name.lowercase().replaceFirstChar { it.uppercase() }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.updateSearchQuery(it)
            },
            placeholder = { Text("Search by $searchLabel") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        viewModel.updateSearchQuery("")
                    }) { Icon(Icons.Default.Clear, contentDescription = "Clear") }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            singleLine = true,
            shape = RoundedCornerShape(20.dp)
        )

        // 🔹 FILTER CHIPS (Compact Wrapping Layout)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp)
        ) {
            searchFields.forEach { filter ->
                if (filter == SearchFilter.KGID && !isAdmin) return@forEach

                val selected = searchFilter == filter
                val labelText = when (filter) {
                    SearchFilter.METAL_NUMBER -> "Metal"
                    SearchFilter.KGID -> "KGID"
                    else -> filter.name.lowercase().replaceFirstChar { it.uppercase() }
                }

                FilterChip(
                    selected = selected,
                    onClick = { viewModel.updateSearchFilter(filter) },
                    label = {
                        Text(
                            labelText,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }


        // 🔹 EMPLOYEE LIST
        Box(modifier = Modifier.weight(1f)) {
            when (val status = employeeStatus) {
                is OperationStatus.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                is OperationStatus.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Error: ${status.message}", color = MaterialTheme.colorScheme.error)
                }
                is OperationStatus.Success, is OperationStatus.Idle -> {
                    if (filteredEmployees.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("No employees found.", color = MaterialTheme.colorScheme.onBackground)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filteredEmployees, key = { it.kgid ?: it.hashCode() }) { emp ->
                                if (isAdmin) {
                                    // 🧑‍💼 Show the full admin version
                                    EmployeeCardAdmin(
                                        employee = emp,
                                        isAdmin = true,
                                        fontScale = fontScale,
                                        navController = navController,
                                        onDelete = { employeeToDelete = it },
                                        context = context
                                    )
                                } else {
                                    // 👮‍♂️ Show the elegant user version
                                    EmployeeCardUser(
                                        employee = emp,
                                        onClick = {
                                            Toast.makeText(context, "${emp.name} selected", Toast.LENGTH_SHORT).show()
                                        },
                                        fontScale = fontScale
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 🔹 DELETE CONFIRMATION + SNACKBAR (Improved with Auto Refresh)
        if (employeeToDelete != null) {
            AlertDialog(
                onDismissRequest = { employeeToDelete = null },
                title = { Text("Confirm Delete") },
                text = { Text("Delete ${employeeToDelete?.name}? This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        val emp = employeeToDelete
                        employeeToDelete = null
                        if (emp != null) {
                            coroutineScope.launch {
                                try {
                                    // ✅ Delete and refresh the employee list
                                    viewModel.deleteEmployee(emp.kgid, emp.photoUrl)
                                    viewModel.refreshEmployees()

                                    // ✅ Show success message
                                    snackbarHostState.showSnackbar(
                                        message = "${emp.name} deleted successfully",
                                        withDismissAction = true
                                    )
                                } catch (e: Exception) {
                                    // 🚨 Handle failure gracefully
                                    snackbarHostState.showSnackbar(
                                        message = "Failed to delete ${emp.name}: ${e.message}",
                                        withDismissAction = true
                                    )
                                }
                            }
                        }
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { employeeToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

    }
}

/**
 * Single Font Size Selector Button
 * - Click to cycle through common preset sizes (0.8, 1.0, 1.2, 1.4, 1.6, 1.8)
 * - Long press opens menu with all size options for precise selection
 */
@Composable
private fun FontSizeSelectorButton(
    currentFontScale: Float,
    onFontScaleSelected: (Float) -> Unit,
    onFontScaleToggle: () -> Unit
) {
    var showDropdownMenu by remember { mutableStateOf(false) }
    val presetSizes = listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f)
    
    Box {
        // Single button with combined clickable for click and long press
        Box(
            modifier = Modifier
                .combinedClickable(
                    onClick = onFontScaleToggle,
                    onLongClick = { showDropdownMenu = true }
                )
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${(currentFontScale * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Font Size (Click to cycle, Long press for menu)",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        
        // Dropdown Menu
        DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = { showDropdownMenu = false }
        ) {
            presetSizes.forEach { size ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${(size * 100).toInt()}%",
                            fontWeight = if (kotlin.math.abs(size - currentFontScale) < 0.05f) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    },
                    onClick = {
                        onFontScaleSelected(size)
                        showDropdownMenu = false
                    },
                    leadingIcon = if (kotlin.math.abs(size - currentFontScale) < 0.05f) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Current size",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}
