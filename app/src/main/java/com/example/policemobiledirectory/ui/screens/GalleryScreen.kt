package com.example.policemobiledirectory.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.policemobiledirectory.model.GalleryImage
import com.example.policemobiledirectory.ui.viewmodel.GalleryViewModel
import com.example.policemobiledirectory.utils.OperationStatus
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration

enum class ViewMode {
    Grid, List
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    navController: NavController,
    viewModel: GalleryViewModel = hiltViewModel(),
    isAdmin: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val galleryImages by viewModel.galleryImages.collectAsState()
    val galleryStatus by viewModel.galleryStatus.collectAsState()
    val uploadStatus by viewModel.uploadStatus.collectAsState()
    val deleteStatus by viewModel.deleteStatus.collectAsState()

    var showUploadDialog by remember { mutableStateOf(false) }
    var fullScreenImage by remember { mutableStateOf<String?>(null) }
    var deleteDialogImageTitle by remember { mutableStateOf<String?>(null) }
    var viewMode by remember { mutableStateOf(ViewMode.Grid) } // Grid or List view
    var columnsPerRow by remember { mutableStateOf(4) } // 1, 2, 4, 6, 8 images per row

    // Get the current back stack entry to detect when screen comes back into focus
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // 🔹 Fetch gallery images on launch and when coming back
    LaunchedEffect(currentRoute) {
        // Only refresh if we're on the gallery screen
        if (currentRoute == com.example.policemobiledirectory.navigation.Routes.GALLERY_SCREEN) {
            viewModel.fetchGalleryImages()
        }
    }

    // Handle upload status
    LaunchedEffect(uploadStatus) {
        when (val status = uploadStatus) {
            is OperationStatus.Success -> {
                Toast.makeText(
                    context,
                    status.data ?: "Image uploaded successfully",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.clearStatus()
            }

            is OperationStatus.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
                viewModel.clearStatus()
            }

            else -> {}
        }
    }

    // Handle delete status
    LaunchedEffect(deleteStatus) {
        when (val status = deleteStatus) {
            is OperationStatus.Success -> {
                Toast.makeText(
                    context,
                    status.data ?: "Image deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.clearStatus()
            }

            is OperationStatus.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
                viewModel.clearStatus()
            }

            else -> {}
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
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White,
                    actionIconContentColor = androidx.compose.ui.graphics.Color.White
                ),
                actions = {
                    // View mode toggle (Grid/List) - Grid button cycles through column counts
                    ViewModeToggle(
                        currentMode = viewMode,
                        currentColumns = if (viewMode == ViewMode.Grid) columnsPerRow else 4,
                        onModeChange = { viewMode = it },
                        onGridTap = {
                            // Cycle through: 1 → 2 → 4 → 6 → 8 → 1
                            val columnOptions = listOf(1, 2, 4, 6, 8)
                            val currentIndex = columnOptions.indexOf(columnsPerRow)
                            val nextIndex = (currentIndex + 1) % columnOptions.size
                            columnsPerRow = columnOptions[nextIndex]
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = { viewModel.fetchGalleryImages(forceRefresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = { showUploadDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Upload Image")
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val currentStatus = galleryStatus
            when (currentStatus) {
                is OperationStatus.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                is OperationStatus.Error -> ErrorSection(
                    currentStatus.message,
                    onRetry = { viewModel.fetchGalleryImages(forceRefresh = true) }
                )

                is OperationStatus.Success -> {
                    if (galleryImages.isEmpty()) {
                        EmptySection(icon = Icons.Default.PhotoLibrary, message = "No images found")
                    } else {
                        when (viewMode) {
                            ViewMode.Grid -> {
                                // Calculate min size based on columns per row
                                val screenWidth =
                                    androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
                                val padding = 16.dp // Total horizontal padding
                                val spacing =
                                    8.dp * (columnsPerRow - 1) // Total spacing between items
                                val minSize = (screenWidth - padding - spacing) / columnsPerRow

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(columnsPerRow),
                                    contentPadding = PaddingValues(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(galleryImages) { image ->
                                        GalleryImageItem(
                                            image = image,
                                            isAdmin = isAdmin,
                                            onClick = { fullScreenImage = image.url },
                                            onDelete = { deleteDialogImageTitle = image.title }
                                        )
                                    }
                                }
                            }

                            ViewMode.List -> {
                                LazyColumn(
                                    contentPadding = PaddingValues(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(galleryImages) { image ->
                                        GalleryImageListItem(
                                            image = image,
                                            isAdmin = isAdmin,
                                            onClick = { fullScreenImage = image.url },
                                            onDelete = { deleteDialogImageTitle = image.title }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is OperationStatus.Idle -> EmptySection(icon = Icons.Default.PhotoLibrary, message = "No images loaded")
            }

            // 📤 Upload dialog with Camera/Gallery/File Manager
            if (showUploadDialog) {
                UploadGalleryDialog(
                    onDismiss = { showUploadDialog = false },
                    onUpload = { title: String, uri: Uri, mimeType: String, category: String?, description: String? ->
                        scope.launch {
                            // Use compressed version for gallery images to prevent timeout
                            val base64 = uriToBase64Compressed(context, uri)
                            if (base64 != null) {
                                viewModel.uploadGalleryImage(
                                    title,
                                    base64,
                                    mimeType,
                                    category,
                                    description
                                )
                                showUploadDialog = false
                            } else {
                                Toast.makeText(context, "Failed to read image", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                )
            }

            // 👀 Fullscreen image dialog
            val currentFullScreenImage = fullScreenImage
            if (currentFullScreenImage != null) {
                // ✅ Convert Drive URL to direct image URL for fullscreen display
                val fullScreenImageUrl = remember(currentFullScreenImage) {
                    convertDriveUrlToDirectImageUrl(currentFullScreenImage)
                }

                Dialog(onDismissRequest = { fullScreenImage = null }) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Black.copy(alpha = 0.9f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = fullScreenImageUrl,
                                contentDescription = "Full Screen Image",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit
                            )
                            IconButton(
                                onClick = { fullScreenImage = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 🗑️ Delete confirmation dialog
            if (deleteDialogImageTitle != null) {
                AlertDialog(
                    onDismissRequest = { deleteDialogImageTitle = null },
                    title = { Text("Delete Image?") },
                    text = { Text("This will permanently remove the image from gallery.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                deleteDialogImageTitle?.let { title ->
                                    viewModel.deleteGalleryImage(title)
                                    deleteDialogImageTitle = null
                                }
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteDialogImageTitle = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Custom Grid Icon - Creates a visual grid representation based on columns
 */
@Composable
fun GridIcon(columns: Int, modifier: Modifier = Modifier) {
        val rows = when (columns) {
            1 -> 1
            2 -> 1
            4 -> 2
            6 -> 2
            8 -> 2
            else -> 2
        }

        Box(
            modifier = modifier.size(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(columns / rows) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }

/**
 * Columns Per Row Selector - Dropdown to select 1, 2, 4, 6, or 8 columns with grid icons
 */
@Composable
fun ColumnsPerRowSelector(
        currentColumns: Int,
        onColumnsChange: (Int) -> Unit,
        modifier: Modifier = Modifier
    ) {
        var expanded by remember { mutableStateOf(false) }
        val options = listOf(1, 2, 4, 6, 8)

        Box(modifier = modifier) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { expanded = true }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GridIcon(
                    columns = currentColumns,
                    modifier = Modifier.size(20.dp)
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Select columns",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { count ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GridIcon(
                                    columns = count,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("$count per row")
                            }
                        },
                        onClick = {
                            onColumnsChange(count)
                            expanded = false
                        },
                        leadingIcon = {
                            if (currentColumns == count) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Spacer(modifier = Modifier.size(24.dp))
                            }
                        }
                    )
                }
            }
        }
    }

/**
 * View Mode Toggle - Segmented control for Grid/List view
 * Grid button cycles through column counts: 1 → 2 → 4 → 6 → 8 → 1
 */
@Composable
fun ViewModeToggle(
        currentMode: ViewMode,
        currentColumns: Int = 4,
        onModeChange: (ViewMode) -> Unit,
        onGridTap: () -> Unit = {},
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // List View Button
            Box(
                modifier = Modifier
                    .clickable { onModeChange(ViewMode.List) }
                    .background(
                        if (currentMode == ViewMode.List)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            Color.Transparent
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .border(
                        width = 0.5.dp,
                        color = Color.Gray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (currentMode == ViewMode.List) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "List View",
                        modifier = Modifier.size(18.dp),
                        tint = if (currentMode == ViewMode.List)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Grid View Button - Cycles through column counts on tap
            Box(
                modifier = Modifier
                    .clickable {
                        if (currentMode == ViewMode.Grid) {
                            // If already in grid mode, cycle columns
                            onGridTap()
                        } else {
                            // If in list mode, switch to grid
                            onModeChange(ViewMode.Grid)
                        }
                    }
                    .background(
                        if (currentMode == ViewMode.Grid)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            Color.Transparent
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .border(
                        width = 0.5.dp,
                        color = Color.Gray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (currentMode == ViewMode.Grid) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Show grid icon with column indicator
                    if (currentMode == ViewMode.Grid) {
                        GridIcon(
                            columns = currentColumns,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = "Grid View",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }

@Composable
fun GalleryImageItem(
        image: GalleryImage,
        isAdmin: Boolean = false,
        onClick: () -> Unit,
        onDelete: () -> Unit
    ) {
        // ✅ Convert Drive URL to direct image URL for display
        val imageUrl = remember(image.url) {
            convertDriveUrlToDirectImageUrl(image.url)
        }

        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Gallery Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Only show delete button for admins
            if (isAdmin) {
                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

/**
 * Gallery Image Item for List View - Horizontal layout with image and details
 */
@Composable
fun GalleryImageListItem(
        image: GalleryImage,
        isAdmin: Boolean = false,
        onClick: () -> Unit,
        onDelete: () -> Unit
    ) {
        // ✅ Convert Drive URL to direct image URL for display
        val imageUrl = remember(image.url) {
            convertDriveUrlToDirectImageUrl(image.url)
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image thumbnail
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Gallery Image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                // Image details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = image.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    image.category?.let {
                        Text(
                            text = "Category: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    image.description?.let {
                        if (it.isNotBlank()) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 2
                            )
                        }
                    }
                }

                // Delete button (only for admins)
                if (isAdmin) {
                    IconButton(
                        onClick = { onDelete() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

@Composable
fun UploadGalleryDialog(
        onDismiss: () -> Unit,
        onUpload: (String, Uri, String, String?, String?) -> Unit
    ) {
        val context = LocalContext.current
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var imageUri by remember { mutableStateOf<Uri?>(null) }
        var mimeType by remember { mutableStateOf("image/jpeg") }
        var showPickerOptions by remember { mutableStateOf(false) }

        // 📷 Camera launcher
        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && imageUri != null) {
                mimeType = "image/jpeg"
            }
        }

        // 🖼️ Gallery launcher
        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                imageUri = it
                mimeType = context.contentResolver.getType(it) ?: "image/jpeg"
            }
        }

        // 📁 File Manager launcher
        val fileManagerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                imageUri = it
                mimeType = context.contentResolver.getType(it) ?: "image/jpeg"
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = imageUri
                        if (title.isNotBlank() && uri != null) {
                            onUpload(
                                title,
                                uri,
                                mimeType,
                                category.takeIf { it.isNotBlank() },
                                description.takeIf { it.isNotBlank() })
                        } else {
                            Toast.makeText(
                                context,
                                "Please enter title and select an image",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text("Upload")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
            title = { Text("Upload Gallery Image") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (showPickerOptions) {
                        // Camera, Gallery, File Manager options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val tempFile = java.io.File(
                                        context.cacheDir,
                                        "temp_camera_${System.currentTimeMillis()}.jpg"
                                    )
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        tempFile
                                    )
                                    imageUri = uri
                                    cameraLauncher.launch(uri)
                                    showPickerOptions = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Camera", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = {
                                    galleryLauncher.launch("image/*")
                                    showPickerOptions = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Gallery", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = {
                                    fileManagerLauncher.launch("image/*")
                                    showPickerOptions = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Files", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    } else {
                        Button(
                            onClick = { showPickerOptions = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (imageUri == null) "Select Image" else "Change Image")
                        }
                    }

                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        )
    }

