package com.example.policemobiledirectory.ui.screens

import android.content.ContentResolver
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.ui.viewmodel.DocumentsViewModel
import com.example.policemobiledirectory.ui.screens.uriToBase64
import com.example.policemobiledirectory.utils.OperationStatus
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadDocumentScreen(
    navController: NavController,
    viewModel: DocumentsViewModel = hiltViewModel(),
    isAdmin: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contentResolver = context.contentResolver

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var mimeType by remember { mutableStateOf("application/pdf") }
    
    val uploadStatus by viewModel.uploadStatus.collectAsState()
    val isLoading = uploadStatus is OperationStatus.Loading

    // 🔹 File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
        uri?.let {
            val path = it.lastPathSegment ?: "document.pdf"
            fileName = path.substringAfterLast('/')
            fileSize = getFileSize(contentResolver, it)
            mimeType = context.contentResolver.getType(it) ?: "application/pdf"
            // Auto-fill title if empty
            if (title.isBlank()) {
                title = fileName.substringBeforeLast('.')
            }
        }
    }
    
    // Handle upload status
    LaunchedEffect(uploadStatus) {
        when (val status = uploadStatus) {
            is OperationStatus.Success -> {
                Toast.makeText(context, status.data ?: "Document uploaded successfully", Toast.LENGTH_SHORT).show()
                viewModel.clearStatus()
                navController.popBackStack()
            }
            is OperationStatus.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                viewModel.clearStatus()
            }
            else -> {}
        }
    }

    if (!isAdmin) {
        // Non-admin users should not access upload UI
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Upload Document") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = androidx.compose.ui.graphics.Color.White,
                        navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Only admins can upload documents.")
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Document") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Upload Document to Google Drive",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleMedium
                )

                // 🔹 File Picker Button
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.AttachFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedUri == null) "Choose Document" else "Change Document")
                }

                if (selectedUri != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📄 $fileName",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (fileSize.isNotEmpty()) {
                                Text(
                                    text = "Size: $fileSize",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 🔹 Form Fields
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    isError = title.isBlank() && selectedUri != null
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    maxLines = 3
                )

                // 🔹 Upload Button
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        selectedUri?.let { uri ->
                            scope.launch {
                                val base64 = uriToBase64(context, uri)
                                if (base64 != null) {
                                    viewModel.uploadDocument(
                                        title = title,
                                        fileBase64 = base64,
                                        mimeType = mimeType,
                                        category = category.takeIf { it.isNotBlank() },
                                        description = description.takeIf { it.isNotBlank() }
                                    )
                                } else {
                                    Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } ?: Toast.makeText(context, "Please select a file first", Toast.LENGTH_SHORT).show()
                    },
                    enabled = selectedUri != null && title.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uploading...")
                    } else {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Document")
                    }
                }

                // 🔹 Linear Progress Bar
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Utility to get file size from URI in human-readable format.
 */
private fun getFileSize(contentResolver: ContentResolver, uri: Uri): String {
    return try {
        val cursor = contentResolver.query(uri, null, null, null, null)
        val sizeIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.SIZE)
        var size = 0L
        if (cursor != null && sizeIndex != null && cursor.moveToFirst()) {
            size = cursor.getLong(sizeIndex)
        }
        cursor?.close()

        val kb = size / 1024.0
        val mb = kb / 1024.0
        val df = DecimalFormat("#.##")
        if (mb >= 1) "${df.format(mb)} MB" else "${df.format(kb)} KB"
    } catch (e: Exception) {
        "Unknown size"
    }
}
