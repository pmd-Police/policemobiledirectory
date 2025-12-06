package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import com.example.policemobiledirectory.model.NotificationTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendNotificationScreen(
    navController: NavController,
    viewModel: EmployeeViewModel,
    constantsViewModel: ConstantsViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val pendingStatus by viewModel.pendingStatus.collectAsState()
    val employees by viewModel.employees.collectAsState()
    
    // Get dynamic constants from ConstantsViewModel
    val stationsByDistrict by constantsViewModel.stationsByDistrict.collectAsStateWithLifecycle()
    val districts by constantsViewModel.districts.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var target by remember { mutableStateOf(NotificationTarget.ALL) }

    // For single user target
    var searchKgid by remember { mutableStateOf("") }
    val kGidSuggestions = employees.map { it.kgid }
    var kGidExpanded by remember { mutableStateOf(false) }

    // For district/station targets
    var selectedDistrict by remember { mutableStateOf("All") }
    var districtExpanded by remember { mutableStateOf(false) }

    var selectedStation by remember { mutableStateOf("All") }
    var stationExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }

    val stationsForDistrict = remember(selectedDistrict, stationsByDistrict) {
        if (selectedDistrict == "All") listOf("All")
        else listOf("All") + (stationsByDistrict[selectedDistrict] ?: emptyList())
    }

    // Reset dependent fields when target changes
    LaunchedEffect(target) {
        if (target != NotificationTarget.SINGLE) {
            searchKgid = ""
        }
        if (target != NotificationTarget.DISTRICT && target != NotificationTarget.STATION) {
            selectedDistrict = "All"
            selectedStation = "All"
        }
    }

    LaunchedEffect(pendingStatus) {
        if (pendingStatus is OperationStatus.Success) {
            snackbarHostState.showSnackbar("Notification sent successfully!")
            viewModel.resetPendingStatus()
            navController.popBackStack()
        } else if (pendingStatus is OperationStatus.Error) {
            snackbarHostState.showSnackbar((pendingStatus as OperationStatus.Error).message)
            viewModel.resetPendingStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Send Notification") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Body") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            // Target selection dropdown
            ExposedDropdownMenuBox(
                expanded = targetExpanded,
                onExpandedChange = { targetExpanded = !targetExpanded }
            ) {
                OutlinedTextField(
                    value = target.name.replaceFirstChar { it.uppercaseChar() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Send to") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = targetExpanded,
                    onDismissRequest = { targetExpanded = false }
                ) {
                    NotificationTarget.values().forEach { notificationTarget ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    when (notificationTarget) {
                                        NotificationTarget.ALL -> "All Users"
                                        NotificationTarget.SINGLE -> "Single User (by KGID)"
                                        NotificationTarget.DISTRICT -> "District/Unit"
                                        NotificationTarget.STATION -> "Police Station"
                                        NotificationTarget.ADMIN -> "Admin Users"
                                    }
                                )
                            },
                            onClick = {
                                target = notificationTarget
                                targetExpanded = false
                            }
                        )
                    }
                }
            }

            // --- Single KGID selector ---
            if (target == NotificationTarget.SINGLE) {
                ExposedDropdownMenuBox(expanded = kGidExpanded, onExpandedChange = { kGidExpanded = !kGidExpanded }) {
                    OutlinedTextField(
                        value = searchKgid,
                        onValueChange = { searchKgid = it.filter { ch -> ch.isDigit() }; kGidExpanded = true },
                        label = { Text("Employee KGID") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = kGidExpanded, onDismissRequest = { kGidExpanded = false }) {
                        kGidSuggestions.filter { it.contains(searchKgid, ignoreCase = true) }.forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { searchKgid = it; kGidExpanded = false })
                        }
                    }
                }
            }

            // --- District selector ---
            if (target == NotificationTarget.DISTRICT || target == NotificationTarget.STATION) {
                ExposedDropdownMenuBox(expanded = districtExpanded, onExpandedChange = { districtExpanded = !districtExpanded }) {
                    OutlinedTextField(
                        value = selectedDistrict,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("District/Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = districtExpanded, onDismissRequest = { districtExpanded = false }) {
                        districts.forEach { district ->
                            DropdownMenuItem(text = { Text(district) }, onClick = {
                                selectedDistrict = district
                                districtExpanded = false
                            })
                        }
                    }
                }
            }

            // --- Station selector ---
            if (target == NotificationTarget.STATION) {
                ExposedDropdownMenuBox(expanded = stationExpanded, onExpandedChange = { stationExpanded = !stationExpanded }) {
                    OutlinedTextField(
                        value = selectedStation,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Police Station") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = stationExpanded, onDismissRequest = { stationExpanded = false }) {
                        stationsForDistrict.forEach { station ->
                            DropdownMenuItem(text = { Text(station) }, onClick = {
                                selectedStation = station
                                stationExpanded = false
                            })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // Validate inputs based on target type
                    val isValid = when (target) {
                        NotificationTarget.SINGLE -> searchKgid.isNotBlank()
                        NotificationTarget.DISTRICT -> selectedDistrict != "All"
                        NotificationTarget.STATION -> selectedDistrict != "All" && selectedStation != "All"
                        else -> true // ALL and ADMIN don't need additional validation
                    }
                    
                    if (isValid) {
                        viewModel.sendNotification(
                            title, body, target,
                            k = if (target == NotificationTarget.SINGLE) searchKgid else null,
                            d = if (target == NotificationTarget.DISTRICT || target == NotificationTarget.STATION) selectedDistrict else null,
                            s = if (target == NotificationTarget.STATION) selectedStation else null
                        )
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                when (target) {
                                    NotificationTarget.SINGLE -> "Please select an employee KGID"
                                    NotificationTarget.DISTRICT -> "Please select a district"
                                    NotificationTarget.STATION -> "Please select a district and station"
                                    else -> "Please fill all required fields"
                                }
                            )
                        }
                    }
                },
                enabled = title.isNotBlank() && body.isNotBlank() && pendingStatus != OperationStatus.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (pendingStatus is OperationStatus.Loading) "Sending..." else "Send Notification")
            }
        }
    }
}
