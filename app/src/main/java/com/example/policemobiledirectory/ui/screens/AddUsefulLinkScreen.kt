package com.example.policemobiledirectory.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import java.io.ByteArrayOutputStream
import java.util.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUsefulLinkScreen(
    navController: NavController,
    viewModel: EmployeeViewModel
) {
    val context = LocalContext.current
    val pendingStatus by viewModel.pendingStatus.collectAsState()
    var hasAttemptedSave by remember { mutableStateOf(false) }
    var inlineError by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Form State
    var name by remember { mutableStateOf("") }
    var playStoreUrl by remember { mutableStateOf("") }
    var apkUrl by remember { mutableStateOf("") }
    var iconUrl by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var apkFileUri by remember { mutableStateOf<Uri?>(null) }
    var apkFileName by remember { mutableStateOf<String?>(null) }
    var apkSelectionMessage by remember { mutableStateOf<String?>(null) }

    // Reset any stale status when the screen opens so we don't auto-close due to old Success/Error values
    LaunchedEffect(Unit) {
        viewModel.resetPendingStatus()
        hasAttemptedSave = false
        inlineError = null
        apkSelectionMessage = null
        successMessage = null
    }

    // --- Image Pickers ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { imageUri = it } }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let { imageUri = getImageUri(context, it) }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            apkFileUri = uri
            apkFileName = getFileName(context, uri)
            apkSelectionMessage = apkFileName?.let { "APK selected: $it. Uploading to storage..." }
        }
    }

    LaunchedEffect(apkSelectionMessage) {
        if (apkSelectionMessage != null) {
            delay(6000)
            apkSelectionMessage = null
        }
    }

    // --- Handle Success/Error ---
    LaunchedEffect(pendingStatus, hasAttemptedSave) {
        if (!hasAttemptedSave) return@LaunchedEffect
        when (val status = pendingStatus) {
            is OperationStatus.Success -> {
                successMessage = "✅ Link added successfully!"
                viewModel.resetPendingStatus()
                inlineError = null
                hasAttemptedSave = false
                // Show toast for immediate feedback (LENGTH_LONG = 3.5 seconds)
                Toast.makeText(context, "✅ Link added successfully!", Toast.LENGTH_LONG).show()
            }
            is OperationStatus.Error -> {
                inlineError = status.message ?: "Failed to add link"
                Toast.makeText(context, inlineError, Toast.LENGTH_LONG).show()
                viewModel.resetPendingStatus()
                hasAttemptedSave = false
            }
            else -> {}
        }
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            delay(5000)
            successMessage = null
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Link") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            successMessage?.let { message ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            inlineError?.let { message ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { inlineError = null }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // 1. Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("App / Website Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 2. Play Store Input with Auto-Name Logic
            OutlinedTextField(
                value = playStoreUrl,
                onValueChange = {
                    playStoreUrl = it
                    // Auto-fill name if user pastes a Play Store link
                    if (name.isBlank() && it.contains("id=")) {
                        val pkg = getPackageNameFromPlayUrl(it)
                        if (pkg != null) {
                            name = pkg.substringAfterLast(".").replaceFirstChar { c -> c.uppercase() }
                        }
                    }
                },
                label = { Text("Play Store / Website URL") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            Text(
                text = "Tip: Paste a Play Store link to auto-fetch the name.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp)
            )

            // 3. APK URL Input
            OutlinedTextField(
                value = apkUrl,
                onValueChange = { apkUrl = it },
                label = { Text("Direct APK URL (Optional)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(apkFileName?.let { "APK Selected: $it" } ?: "Upload APK File")
            }

            apkSelectionMessage?.let { message ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("APK Selected", fontWeight = FontWeight.SemiBold)
                            apkFileName?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text(
                                text = "It will upload to storage and auto-fill the URL.",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Image Preview
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                when {
                    imageUri != null -> Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Selected",
                        modifier = Modifier.fillMaxSize()
                    )
                    iconUrl.isNotEmpty() -> Image(
                        painter = rememberAsyncImagePainter(iconUrl),
                        contentDescription = "Url Icon",
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> Text("No Logo", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Image Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Gallery")
                }
                OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Camera")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Save Button
            Button(
                onClick = {
                    // Two separate flows:
                    // Flow 1: Play Store link + name (no APK needed)
                    // Flow 2: APK file + logo upload (Play Store link optional)
                    val hasPlayStoreLink = playStoreUrl.isNotBlank()
                    val hasApkFile = apkFileUri != null
                    val hasDirectApkUrl = apkUrl.isNotBlank()
                    
                    // Validation: Need name and either Play Store link OR APK file/URL
                    if (name.isBlank()) {
                        Toast.makeText(context, "Enter App/Website Name", Toast.LENGTH_LONG).show()
                    } else if (!hasPlayStoreLink && !hasApkFile && !hasDirectApkUrl) {
                        Toast.makeText(
                            context, 
                            "Provide either Play Store URL OR upload APK file/URL", 
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        hasAttemptedSave = true
                        // Pass data to ViewModel. ViewModel handles upload logic separately.
                        viewModel.addUsefulLink(
                            name = name,
                            playStoreUrl = playStoreUrl,
                            apkUrl = apkUrl,
                            iconUrl = iconUrl,
                            apkFileUri = apkFileUri,
                            imageUri = imageUri
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = pendingStatus !is OperationStatus.Loading
            ) {
                if (pendingStatus is OperationStatus.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Link")
                }
            }
        }
    }
}

// Helper for Camera
private fun getImageUri(context: Context, bitmap: Bitmap): Uri {
    val bytes = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
    val path = MediaStore.Images.Media.insertImage(
        context.contentResolver,
        bitmap,
        "Temp_Logo_${UUID.randomUUID()}",
        null
    )
    return Uri.parse(path)
}

private fun getFileName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
    } ?: uri.lastPathSegment
}

// Helper logic for Play Store URL
// Removed duplicate private function since it's defined in UsefulLinksScreen.kt as public or we can make this one private and use it if we resolve ambiguity
// But the error says it conflicts with public one in UsefulLinksScreen.kt
// Since both are top level functions in the same package 'com.example.policemobiledirectory.ui.screens', they conflict if one is public and other is private but visible to same file?
// Actually, Kotlin allows private to shadow public?
// The error says: "Overload resolution ambiguity"
// The issue is that both files are in the same package. `UsefulLinksScreen.kt` defines `fun getPackageNameFromPlayUrl`. `AddUsefulLinkScreen.kt` defines `private fun getPackageNameFromPlayUrl`.
// Since they are in the same package, `AddUsefulLinkScreen.kt` sees the public one from `UsefulLinksScreen.kt`. It also sees its own private one.
// To fix this, I should remove the duplicate definition in `AddUsefulLinkScreen.kt` and use the one from `UsefulLinksScreen.kt`.

// I'll remove the function definition at the bottom of AddUsefulLinkScreen.kt
