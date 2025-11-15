package com.example.policemobiledirectory.ui.screens

import android.content.ContentResolver
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadDocumentScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contentResolver = context.contentResolver

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0.0) }

    // 🔹 File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
        uri?.let {
            val path = it.lastPathSegment ?: "document.pdf"
            fileName = path.substringAfterLast('/')
            fileSize = getFileSize(contentResolver, it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Document") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Select and upload PDFs or documents to Firebase",
                    fontWeight = FontWeight.Medium
                )

                // 🔹 File Picker Button
                Button(
                    onClick = { filePickerLauncher.launch("application/pdf") },
                    enabled = !isUploading
                ) {
                    Text(if (selectedUri == null) "Choose Document" else "Change Document")
                }

                if (selectedUri != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

                // 🔹 Upload Button
                Button(
                    onClick = {
                        selectedUri?.let { uri ->
                            isUploading = true
                            uploadProgress = 0.0
                            scope.launch {
                                try {
                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    val uniqueName = "${timestamp}_$fileName"

                                    val storageRef = FirebaseStorage.getInstance()
                                        .reference.child("documents/$uniqueName")

                                    val uploadTask = storageRef.putFile(uri)

                                    uploadTask.addOnProgressListener { snapshot ->
                                        val progress = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount)
                                        uploadProgress = progress
                                    }.addOnSuccessListener {
                                        Toast.makeText(context, "✅ Uploaded successfully!", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }.addOnFailureListener { e ->
                                        Toast.makeText(context, "❌ Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    }.addOnCompleteListener {
                                        isUploading = false
                                    }
                                } catch (e: Exception) {
                                    isUploading = false
                                    Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } ?: Toast.makeText(context, "Please select a file first", Toast.LENGTH_SHORT).show()
                    },
                    enabled = selectedUri != null && !isUploading
                ) {
                    if (isUploading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uploading... ${uploadProgress.toInt()}%")
                        }
                    } else {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Document")
                    }
                }

                // 🔹 Linear Progress Bar
                if (isUploading && uploadProgress > 0) {
                    LinearProgressIndicator(
                        progress = (uploadProgress / 100).toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .padding(horizontal = 16.dp),
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
