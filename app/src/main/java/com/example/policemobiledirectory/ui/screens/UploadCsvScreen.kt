package com.example.policemobiledirectory.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadCsvScreen(
    navController: NavController, 
    viewModel: EmployeeViewModel
) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var uploadProgress by remember { mutableStateOf<Float?>(null) }
    var uploadStatus by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> selectedUri = uri }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload from CSV") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = { launcher.launch("text/csv") }) {
                Text(text = "Select CSV File")
            }

            selectedUri?.let {
                Text("Selected: ${it.path}", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    selectedUri?.let {
//                        viewModel.uploadCsv(it) {
//                            progress, status ->
//                            uploadProgress = progress
//                            uploadStatus = status
//                        }
                    }
                },
                enabled = selectedUri != null && uploadProgress == null
            ) {
                Text("Upload and Process")
            }

            Spacer(modifier = Modifier.height(16.dp))

            uploadProgress?.let {
                LinearProgressIndicator(progress = { it } , modifier = Modifier.fillMaxWidth())
            }

            uploadStatus?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }

            // Optionally, add a preview of the parsed data or validation results here
            CsvParsingPreview()
        }
    }
}


/**
 * A composable to show the results of CSV parsing for user confirmation.
 */
@Composable
fun CsvParsingPreview() {
    // In a real app, you would get the parsed data from the ViewModel
    // and display it in a LazyColumn for review before final import.
    // For this example, we'll just put a placeholder.

    Box(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "CSV data preview will appear here after selection and parsing.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
