package com.example.policemobiledirectory.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.viewmodel.AddEditEmployeeViewModel
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmployeeScreen(
    navController: NavController,
    employeeId: String?,
    viewModel: AddEditEmployeeViewModel = hiltViewModel(),
    constantsViewModel: ConstantsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val employee by viewModel.employee.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()
    val photoStatus by viewModel.photoUploadStatus.collectAsState()
    
    // Get constants from ViewModel
    val ranks by constantsViewModel.ranks.collectAsStateWithLifecycle()
    val districts by constantsViewModel.districts.collectAsStateWithLifecycle()
    val stationsByDistrict by constantsViewModel.stationsByDistrict.collectAsStateWithLifecycle()
    val bloodGroups by constantsViewModel.bloodGroups.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var kgid by remember { mutableStateOf("") }
    var mobile1 by remember { mutableStateOf("") }
    var mobile2 by remember { mutableStateOf("") }
    var rank by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var station by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var currentPhotoUrl by remember { mutableStateOf<String?>(null) }

    val isEditMode = employeeId != null

    LaunchedEffect(employee) {
        employee?.let { employeeData ->
            name = employeeData.name
            kgid = employeeData.kgid
            mobile1 = employeeData.mobile1 ?: ""
            mobile2 = employeeData.mobile2 ?: ""
            rank = employeeData.rank ?: ""
            bloodGroup = employeeData.bloodGroup ?: ""
            station = employeeData.station ?: ""
            district = employeeData.district ?: ""
            currentPhotoUrl = employeeData.photoUrl
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> photoUri = uri }
    )

    Scaffold(
        topBar = {
            CommonTopAppBar(title = if (isEditMode) "Edit Employee" else "Add Employee", navController = navController)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Image(
                    painter = when {
                        photoUri != null -> rememberAsyncImagePainter(photoUri)
                        currentPhotoUrl != null -> rememberAsyncImagePainter(currentPhotoUrl)
                        else -> painterResource(R.drawable.officer)
                    },
                    contentDescription = "Profile Photo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") }
                )
            }

            item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = kgid, onValueChange = { kgid = it.filter { ch -> ch.isDigit() } }, label = { Text("KGID") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), readOnly = isEditMode) }
            item { OutlinedTextField(value = mobile1, onValueChange = { mobile1 = it }, label = { Text("Mobile 1") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = mobile2, onValueChange = { mobile2 = it }, label = { Text("Mobile 2 (Optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()) }

            // Dropdowns
            item { CustomDropdownMenu(label = "Rank", selectedOption = rank, options = ranks, onOptionSelected = { rank = it }) }
            item { CustomDropdownMenu(label = "Blood Group", selectedOption = bloodGroup, options = bloodGroups, onOptionSelected = { bloodGroup = it }) }
            item { CustomDropdownMenu(label = "District", selectedOption = district, options = districts, onOptionSelected = { district = it; station = "" }) } // Reset station on district change

            item {
                val stationOptions = remember(district, stationsByDistrict) {
                    if (district.isNotEmpty()) {
                        // Try exact match first, then case-insensitive match
                        stationsByDistrict[district]
                            ?: stationsByDistrict.keys.find { it.equals(district, ignoreCase = true) }?.let { stationsByDistrict[it] }
                            ?: emptyList()
                    } else {
                        emptyList()
                    }
                }
                CustomDropdownMenu(
                    label = "Station",
                    selectedOption = station,
                    options = stationOptions,
                    onOptionSelected = { station = it },
                    enabled = stationOptions.isNotEmpty()
                )
            }


            item {
                val isPhotoUploading = photoStatus is OperationStatus.Loading
                val isSaving = saveStatus is RepoResult.Loading

                if (isPhotoUploading || isSaving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (photoStatus is OperationStatus.Success) {
                    val url = (photoStatus as OperationStatus.Success).data
                    LaunchedEffect(url) {
                        currentPhotoUrl = url
                        viewModel.resetPhotoStatus()
                    }
                } else if (photoStatus is OperationStatus.Error) {
                    val message = (photoStatus as OperationStatus.Error).message
                    LaunchedEffect(message) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        viewModel.resetPhotoStatus()
                    }
                }

                when (val status = saveStatus) {
                    is RepoResult.Error -> {
                        val message = status.message ?: "Failed to save employee"
                        LaunchedEffect(message) {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            viewModel.resetSaveStatus()
                        }
                    }
                    is RepoResult.Success -> {
                        LaunchedEffect(status) {
                            Toast.makeText(
                                context,
                                if (isEditMode) "Employee updated successfully" else "Employee added successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.resetSaveStatus()
                            navController.popBackStack()
                        }
                    }
                    else -> Unit
                }

                Button(
                    onClick = {
                        val targetKgid = kgid.ifBlank { "TEMP-${System.currentTimeMillis()}" }
                        val newEmployee = Employee(
                            name = name.trim(),
                            kgid = targetKgid,
                            mobile1 = mobile1.trim(),
                            mobile2 = mobile2.trim().takeIf { it.isNotBlank() },
                            rank = rank.ifBlank { null },
                            bloodGroup = bloodGroup.ifBlank { null },
                            station = station.ifBlank { null },
                            district = district.ifBlank { null },
                            photoUrl = currentPhotoUrl,
                            email = employee?.email ?: ""
                        )
                        viewModel.saveEmployee(newEmployee, photoUri)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && kgid.isNotBlank() && mobile1.isNotBlank() &&
                            photoStatus !is OperationStatus.Loading && saveStatus !is RepoResult.Loading
                ) {
                    Text(if (isEditMode) "Update Employee" else "Add Employee")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDropdownMenu(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if(enabled) expanded = !expanded }) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            enabled = enabled
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
