package com.example.policemobiledirectory.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.ui.components.CommonEmployeeForm
import com.example.policemobiledirectory.viewmodel.AddEditEmployeeViewModel
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel

@Composable
fun MyProfileEditScreen(
    navController: NavController? = null,
    employeeViewModel: EmployeeViewModel = hiltViewModel(),
    addEditViewModel: AddEditEmployeeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentEmployee by employeeViewModel.currentUser.collectAsState()
    val saveStatus by addEditViewModel.saveStatus.collectAsState()
    val photoUploadStatus by addEditViewModel.photoUploadStatus.collectAsState()
    val isLoading = saveStatus is com.example.policemobiledirectory.repository.RepoResult.Loading || 
                    photoUploadStatus is com.example.policemobiledirectory.utils.OperationStatus.Loading

    // Handle back button to navigate to home screen
    BackHandler(enabled = navController != null) {
        // Navigate to home screen, clearing back stack up to (but not including) EMPLOYEE_LIST
        navController?.navigate(Routes.EMPLOYEE_LIST) {
            popUpTo(Routes.EMPLOYEE_LIST) { inclusive = false }
            launchSingleTop = true
        }
    }

    // Show feedback messages
    LaunchedEffect(saveStatus) {
        android.util.Log.e("MyProfileScreen", "═══════════════════════════════════════")
        android.util.Log.e("MyProfileScreen", "📊📊📊 saveStatus CHANGED: $saveStatus")
        android.util.Log.e("MyProfileScreen", "saveStatus type: ${saveStatus?.javaClass?.simpleName}")
        when (val status = saveStatus) {
            is RepoResult.Success -> {
                android.util.Log.e("MyProfileScreen", "✅✅✅ SUCCESS! Showing toast")
                Toast.makeText(
                    context,
                    "Profile updated successfully",
                    Toast.LENGTH_SHORT
                ).show()
                addEditViewModel.resetSaveStatus()
                // ✅ Refresh current user to show updated data (including metal number)
                employeeViewModel.refreshCurrentUser()
                employeeViewModel.refreshEmployees()
                // ✅ Navigate back after successful save
                navController?.popBackStack()
            }
            is RepoResult.Error -> {
                android.util.Log.e("MyProfileScreen", "❌❌❌ ERROR RECEIVED!")
                android.util.Log.e("MyProfileScreen", "Error message: ${status.message}")
                android.util.Log.e("MyProfileScreen", "Error exception: ${status.exception?.message}")
                android.util.Log.e("MyProfileScreen", "Full error: $status")
                Toast.makeText(
                    context,
                    status.message ?: "Failed to update profile",
                    Toast.LENGTH_LONG
                ).show()
                addEditViewModel.resetSaveStatus()
            }
            is RepoResult.Loading -> {
                android.util.Log.e("MyProfileScreen", "⏳⏳⏳ LOADING...")
            }
            else -> {
                android.util.Log.e("MyProfileScreen", "📊 Other status: $status")
            }
        }
    }
    
    // Also log photo upload status changes
    LaunchedEffect(photoUploadStatus) {
        android.util.Log.e("MyProfileScreen", "📸📸📸 photoUploadStatus CHANGED: $photoUploadStatus")
        android.util.Log.e("MyProfileScreen", "photoUploadStatus type: ${photoUploadStatus.javaClass.simpleName}")
    }

    CommonEmployeeForm(
        isAdmin = false,
        isSelfEdit = true,
        isRegistration = false,
        initialEmployee = currentEmployee,
        onNavigateToTerms = null,
        onSubmit = { emp: Employee, photo: Uri? ->
            android.util.Log.e("MyProfileScreen", "═══════════════════════════════════════")
            android.util.Log.e("MyProfileScreen", "📤📤📤 onSubmit CALLBACK INVOKED! 📤📤📤")
            android.util.Log.e("MyProfileScreen", "Employee KGID: ${emp.kgid}")
            android.util.Log.e("MyProfileScreen", "Has photo: ${photo != null}")
            android.util.Log.e("MyProfileScreen", "Photo URI: $photo")
            android.util.Log.e("MyProfileScreen", "About to call addEditViewModel.saveEmployee()...")
            try {
                addEditViewModel.saveEmployee(emp, photo)
                android.util.Log.e("MyProfileScreen", "✅✅✅ saveEmployee() call completed (no exception)")
            } catch (e: Exception) {
                android.util.Log.e("MyProfileScreen", "❌❌❌ EXCEPTION calling saveEmployee: ${e.message}", e)
                e.printStackTrace()
            }
        },
        onRegisterSubmit = null,
        isLoading = isLoading
    )
}
