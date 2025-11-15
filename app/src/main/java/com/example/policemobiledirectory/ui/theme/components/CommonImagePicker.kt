package com.example.policemobiledirectory.ui.components

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.policemobiledirectory.R
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.result.ActivityResult
import android.content.Intent
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import android.util.Log
import android.widget.Toast

/**
 * ✅ CommonImagePicker
 *
 * Reusable image picker composable for:
 * - Profile screen
 * - Employee cards (admin)
 * - Navigation drawer header
 *
 * Features:
 * - Placeholder (R.drawable.officer)
 * - Crop (square)
 * - Camera or Gallery selection
 * - Upload callback
 * - Progress + Error display
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonImagePicker(
    currentPhotoUrl: String?,
    onUploadRequested: suspend (uri: Uri) -> Result<String>,
    modifier: Modifier = Modifier,
    sizeDp: Int = 120
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- State Variables
    var localSelectedUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var uploadedRemoteUrl by remember { mutableStateOf<String?>(currentPhotoUrl) }
    var cameraTempFileUri by remember { mutableStateOf<Uri?>(null) }
    var uploadProgress by remember { mutableStateOf(0f) } // ✅ define before usage

    // --- 🧩 Crop Launcher
    val cropLauncher = rememberLauncherForActivityResult(
        contract = StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = result.data?.let { UCrop.getOutput(it) }
            resultUri?.let { uri ->
                coroutineScope.launch {
                    isUploading = true
                    uploadError = null
                    uploadProgress = 0f

                    // --- Fake Progress Animation (can replace with actual upload % later)
                    val progressJob = launch {
                        var p = 0f
                        while (isUploading && p < 1f) {
                            delay(150)
                            p += 0.05f
                            uploadProgress = p.coerceAtMost(1f)
                        }
                    }

                    val resultUpload = try {
                        onUploadRequested(uri)
                    } catch (e: Exception) {
                        Result.failure<String>(e)
                    }

                    isUploading = false
                    uploadProgress = 1f
                    progressJob.cancel()

                    if (resultUpload.isSuccess) {
                        uploadedRemoteUrl = resultUpload.getOrNull()
                        localSelectedUri = null
                    } else {
                        uploadError = resultUpload.exceptionOrNull()?.message ?: "Upload failed"
                    }
                }
            }
        }
    }

    // --- 🖼️ Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val destinationUri = try {
                    val destFile = File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        destFile
                    )
                } catch (e: Exception) {
                    Log.e("CommonImagePicker", "Failed to create destination URI: ${e.message}", e)
                    Toast.makeText(context, "Failed to prepare image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    return@let
                }
                
                val cropIntent = UCrop.of(it, destinationUri)
                    .withAspectRatio(1f, 1f)
                    .withMaxResultSize(800, 800)
                    .getIntent(context)
                cropLauncher.launch(cropIntent)
            } catch (e: Exception) {
                Log.e("CommonImagePicker", "Gallery picker error: ${e.message}", e)
                Toast.makeText(context, "Failed to process image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- 📸 Camera Capture
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            val tempUri = cameraTempFileUri // 👈 Safe local copy
            if (tempUri != null) {
                try {
                    val destinationUri = try {
                        val destFile = File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                        androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            destFile
                        )
                    } catch (e: Exception) {
                        Log.e("CommonImagePicker", "Failed to create destination URI: ${e.message}", e)
                        Toast.makeText(context, "Failed to prepare image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        return@rememberLauncherForActivityResult
                    }
                    
                    val cropIntent = UCrop.of(tempUri, destinationUri)
                        .withAspectRatio(1f, 1f)
                        .withMaxResultSize(800, 800)
                        .getIntent(context)
                    cropLauncher.launch(cropIntent)
                } catch (e: Exception) {
                    Log.e("CommonImagePicker", "Camera capture error: ${e.message}", e)
                    Toast.makeText(context, "Failed to process image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("CommonImagePicker", "Camera temp URI is null")
                Toast.makeText(context, "Camera capture failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("CommonImagePicker", "Camera capture returned failure")
        }
    }

    // --- 📋 Chooser Dialog (Camera / Gallery)
    var showChooser by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showChooser = true },
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = Pair(localSelectedUri, uploadedRemoteUrl)) { pair ->
                val (localUri, remoteUrl) = pair
                when {
                    localUri != null -> {
                        AsyncImage(
                            model = localUri,
                            contentDescription = "Selected image",
                            modifier = Modifier
                                .size(sizeDp.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.officer),
                            error = painterResource(id = R.drawable.officer)
                        )
                    }

                    !remoteUrl.isNullOrEmpty() -> {
                        AsyncImage(
                            model = remoteUrl,
                            contentDescription = "Profile image",
                            modifier = Modifier
                                .size(sizeDp.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.officer),
                            error = painterResource(id = R.drawable.officer)
                        )
                    }

                    else -> {
                        Image(
                            painter = painterResource(id = R.drawable.officer),
                            contentDescription = "Default officer",
                            modifier = Modifier
                                .size((sizeDp - 20).dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            if (isUploading) {
                Surface(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(progress = uploadProgress)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Uploading ${(uploadProgress * 100).toInt()}%",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // 🔴 Upload Error
        uploadError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        // 📸 Chooser dialog
        if (showChooser) {
            AlertDialog(
                onDismissRequest = { showChooser = false },
                confirmButton = {
                    TextButton(onClick = { showChooser = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Choose image") },
                text = {
                    Column {
                        TextButton(onClick = {
                            showChooser = false
                            galleryLauncher.launch("image/*")
                        }) {
                            Text("Choose from gallery")
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        TextButton(onClick = {
                            showChooser = false

                            try {
                                // ✅ Safely create a temp file for the camera image
                                val file = createImageFile(context)
                                val tempUri = try {
                                    androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                } catch (e: Exception) {
                                    Log.e("CommonImagePicker", "FileProvider error: ${e.message}", e)
                                    Toast.makeText(context, "Failed to access file provider", Toast.LENGTH_SHORT).show()
                                    return@TextButton
                                }

                                // ✅ Assign once here, for use after capture
                                cameraTempFileUri = tempUri

                                // ✅ Launch the camera with the local immutable URI
                                takePictureLauncher.launch(tempUri)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.e("CommonImagePicker", "Failed to launch camera: ${e.message}", e)
                                Toast.makeText(context, "Failed to launch camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Take photo (camera)")
                        }
                    }
                }
            )
        }
    }
}

/**
 * Creates a temporary image file for camera use.
 */
fun createImageFile(context: Context): File {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir = context.cacheDir
    return File.createTempFile(imageFileName, ".jpg", storageDir).apply {
        createNewFile()
    }
}
