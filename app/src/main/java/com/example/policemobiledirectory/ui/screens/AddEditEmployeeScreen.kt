package com.example.policemobiledirectory.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.ui.components.CommonEmployeeForm
import com.example.policemobiledirectory.viewmodel.AddEditEmployeeViewModel
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel

@Composable
fun AddEditEmployeeScreen(
    employeeId: String?,
    navController: NavController,
    addEditViewModel: AddEditEmployeeViewModel = hiltViewModel(),
    employeeViewModel: EmployeeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val employee by addEditViewModel.employee.collectAsState()
    val saveStatus by addEditViewModel.saveStatus.collectAsState()
    val isNewEmployee = employeeId.isNullOrBlank()
    val isSaving = saveStatus is RepoResult.Loading

    LaunchedEffect(saveStatus) {
        when (val status = saveStatus) {
            is RepoResult.Success -> {
                Toast.makeText(
                    context,
                    if (isNewEmployee) "Employee saved successfully" else "Employee updated successfully",
                    Toast.LENGTH_SHORT
                ).show()
                employeeViewModel.refreshEmployees()
                addEditViewModel.resetSaveStatus()
                navController.popBackStack()
            }
            is RepoResult.Error -> {
                Toast.makeText(
                    context,
                    status.message ?: "Failed to save employee",
                    Toast.LENGTH_LONG
                ).show()
                addEditViewModel.resetSaveStatus()
            }
            else -> Unit
        }
    }

    CommonEmployeeForm(
        isAdmin = true,
        isSelfEdit = false,
        isRegistration = false,
        initialEmployee = employee,
        initialKgid = employeeId,
        onNavigateToTerms = null,
        onSubmit = { emp: Employee, photo: Uri? ->
            addEditViewModel.saveEmployee(emp, photo)
        },
        onRegisterSubmit = null,
        isLoading = isSaving
    )
}
