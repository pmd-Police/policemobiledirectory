package com.example.policemobiledirectory.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.policemobiledirectory.helper.FirebaseStorageHelper
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    navController: NavController,
    viewModel: EmployeeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isAdmin by viewModel.isAdmin.collectAsState()
    val scope = rememberCoroutineScope()

    var isUploading by remember { mutableStateOf(false) }
    var galleryImages by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) } // (id, url)
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var fullScreenImage by remember { mutableStateOf<String?>(null) }
    var deleteDialogImageId by remember { mutableStateOf<String?>(null) }

    val firestore = FirebaseFirestore.getInstance()

    // 🔹 Real-time listener for gallery updates
    LaunchedEffect(Unit) {
        firestore.collection("gallery")
            .orderBy("uploadedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                galleryImages = snapshot?.documents?.mapNotNull {
                    val url = it.getString("imageUrl")
                    val id = it.id
                    if (url != null) id to url else null
                } ?: emptyList()
            }
    }

    // 🔹 Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            scope.launch {
                try {
                    val downloadUrl = FirebaseStorageHelper.uploadPhoto(uri)
                    val data = hashMapOf(
                        "imageUrl" to downloadUrl,
                        "uploadedAt" to com.google.firebase.Timestamp.now()
                    )
                    firestore.collection("gallery").add(data)
                    Toast.makeText(context, "✅ Uploaded successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "❌ Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { if (!isUploading) imagePickerLauncher.launch("image/*") },
                    containerColor = if (isUploading)
                        MaterialTheme.colorScheme.secondary
                    else
                        MaterialTheme.colorScheme.primary
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(Icons.Default.Upload, contentDescription = "Upload Image")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                galleryImages.isEmpty() && !isUploading -> {
                    Text(
                        text = if (isAdmin)
                            "No images yet. Upload using the + button!"
                        else
                            "Gallery is empty. Stay tuned for updates!",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                isUploading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Uploading image...", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 130.dp),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(galleryImages) { (id, imageUrl) ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { fullScreenImage = imageUrl }
                            ) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Gallery Image",
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (isAdmin) {
                                    IconButton(
                                        onClick = { deleteDialogImageId = id },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.5f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 🔹 Delete Confirmation Dialog
        if (deleteDialogImageId != null) {
            AlertDialog(
                onDismissRequest = { deleteDialogImageId = null },
                title = { Text("Delete Image?") },
                text = { Text("This will permanently remove the image from gallery.") },
                confirmButton = {
                    TextButton(onClick = {
                        val id = deleteDialogImageId
                        if (id != null) {
                            firestore.collection("gallery").document(id).delete()
                            Toast.makeText(context, "🗑️ Image deleted", Toast.LENGTH_SHORT).show()
                        }
                        deleteDialogImageId = null
                    }) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteDialogImageId = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 🔹 Fullscreen Image Dialog
        if (fullScreenImage != null) {
            Dialog(onDismissRequest = { fullScreenImage = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = fullScreenImage,
                        contentDescription = "Full Screen Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }
    }
}
