@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.background
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.policemobiledirectory.model.Document
import com.example.policemobiledirectory.ui.viewmodel.DocumentsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import androidx.navigation.NavController
import androidx.compose.material3.TopAppBar

@Composable
fun DocumentsScreen(
    navController: NavController,
    viewModel: DocumentsViewModel,
    isAdmin: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    val documents by viewModel.documents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val deleteSuccess by viewModel.deleteSuccess.collectAsState()
    val uploadSuccess by viewModel.uploadSuccess.collectAsState()

    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }

    // 🔍 Preview state
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var previewMimeType by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchDocuments()
    }

    val filteredDocs = remember(searchQuery, documents) {
        documents.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.category?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    LaunchedEffect(deleteSuccess) {
        deleteSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uploadSuccess) {
        uploadSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Documents") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchDocuments() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = { showUploadDialog = true }) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Upload")
                }
            }
        }
    ) { innerPadding ->

        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // 🔍 Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by title or category") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                when {
                    isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    error != null -> ErrorSection(error!!, onRetry = { viewModel.fetchDocuments() })

                    filteredDocs.isEmpty() -> EmptySection()

                    else -> LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredDocs) { doc ->
                            DocumentItem(
                                doc = doc,
                                isAdmin = isAdmin,
                                onViewClick = {
                                    previewUrl = doc.url

                                    // Smarter MIME detection
                                    previewMimeType = when {
                                        doc.url.contains(".pdf", ignoreCase = true) ||
                                                doc.url.contains("drive.google.com", ignoreCase = true) -> "application/pdf"

                                        doc.url.contains(".jpg", ignoreCase = true) ||
                                                doc.url.contains(".jpeg", ignoreCase = true) -> "image/jpeg"

                                        doc.url.contains(".png", ignoreCase = true) -> "image/png"

                                        doc.url.contains(".doc", ignoreCase = true) ||
                                                doc.url.contains(".docx", ignoreCase = true) -> "application/msword"

                                        doc.url.contains(".xls", ignoreCase = true) ||
                                                doc.url.contains(".xlsx", ignoreCase = true) -> "application/vnd.ms-excel"

                                        doc.url.contains(".ppt", ignoreCase = true) ||
                                                doc.url.contains(".pptx", ignoreCase = true) -> "application/vnd.ms-powerpoint"

                                        else -> "application/octet-stream"
                                    }
                                },

                                onDownloadClick = { scope.launch { downloadFile(context, doc.url, doc.title) } },
                                onDeleteClick = { showDeleteConfirm = doc.title }
                            )


                        }
                    }
                }
            }

            // 🗑 Delete confirmation
            if (showDeleteConfirm != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = null },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                viewModel.deleteDocument(showDeleteConfirm!!)
                                showDeleteConfirm = null
                            }
                        }) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
                    },
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    title = { Text("Delete Document") },
                    text = { Text("Are you sure you want to delete '${showDeleteConfirm}'?") }
                )
            }

            // 📤 Upload dialog
            if (showUploadDialog) {
                UploadDocumentDialog(
                    onDismiss = { showUploadDialog = false },
                    onUpload = { title, uri, mimeType, category, description ->
                        scope.launch {
                            val base64 = uriToBase64(context, uri)
                            if (base64 != null) {
                                viewModel.uploadDocument(title, base64, mimeType, category, description)
                                showUploadDialog = false
                            } else {
                                Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            // 👀 Fullscreen preview dialog
            if (previewUrl != null && previewMimeType != null) {
                FullscreenPreviewDialog(
                    url = previewUrl!!,
                    mimeType = previewMimeType!!,
                    onDismiss = {
                        previewUrl = null
                        previewMimeType = null
                    }
                )
            }
        }
    }
}

@Composable
fun ErrorSection(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("Failed to load: $error", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
fun EmptySection() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("No documents found", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun DocumentItem(
    doc: Document,
    isAdmin: Boolean,
    onViewClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewClick() },
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(doc.title, fontWeight = FontWeight.Medium)
                }
                Row {
                    IconButton(onClick = onViewClick) { Icon(Icons.Default.Visibility, null) }
                    IconButton(onClick = onDownloadClick) { Icon(Icons.Default.FileDownload, null) }
                    if (isAdmin) IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, null) }
                }
            }
            Spacer(Modifier.height(8.dp))
            doc.category?.let { Text("Category: $it", style = MaterialTheme.typography.bodySmall) }
            doc.description?.let { Text("Description: $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun FullscreenPreviewDialog(
    url: String,
    mimeType: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Auto-hide loader after 10 seconds (fallback for stuck Google Docs viewer)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(10_000)
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                // ✅ Top Bar
                CenterAlignedTopAppBar(
                    title = { Text("Preview") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close")
                        }
                    },
                    actions = {
                        IconButton(onClick = { downloadFile(context, url, "Document") }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Download")
                        }
                    }
                )

                // ✅ Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        hasError -> Text(
                            text = "⚠️ Unable to preview this file.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // ✅ PDF, Drive & Office Docs
                        mimeType.contains("pdf", ignoreCase = true) ||
                                url.contains("drive.google.com", ignoreCase = true) ||
                                mimeType.contains("word", ignoreCase = true) ||
                                mimeType.contains("excel", ignoreCase = true) ||
                                mimeType.contains("powerpoint", ignoreCase = true) -> {

                            AndroidView(factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            isLoading = false
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            errorCode: Int,
                                            description: String?,
                                            failingUrl: String?
                                        ) {
                                            hasError = true
                                            isLoading = false
                                        }
                                    }

                                    // ✅ Smarter PDF/Drive Viewer Handling
                                    val viewerUrl = when {
                                        url.contains("drive.google.com") ->
                                            url.replace("view?usp=sharing", "preview")
                                        url.endsWith(".pdf", true) ->
                                            url // Direct PDF links
                                        else ->
                                            "https://docs.google.com/gview?embedded=true&url=$url"
                                    }

                                    loadUrl(viewerUrl)
                                }
                            }, modifier = Modifier.fillMaxSize())
                        }

                        // ✅ Images
                        mimeType.startsWith("image/") -> {
                            Image(
                                painter = rememberAsyncImagePainter(url),
                                contentDescription = "Preview Image",
                                modifier = Modifier.fillMaxSize()
                            )
                            isLoading = false
                        }

                        // ❌ Other file types
                        else -> {
                            Text(
                                text = "Preview not available for this file type.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            isLoading = false
                        }
                    }

                    // ✅ Loader overlay
                    if (isLoading && !hasError) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(8.dp))
                                Text("Loading preview...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun openDocument(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open file", Toast.LENGTH_SHORT).show()
    }
}

fun downloadFile(context: Context, url: String, title: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(title)
            .setDescription("Downloading file...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "Downloading $title...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to download", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun UploadDocumentDialog(
    onDismiss: () -> Unit,
    onUpload: (String, Uri, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var mimeType by remember { mutableStateOf("application/pdf") }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        fileUri = uri
        uri?.let { mimeType = context.contentResolver.getType(it) ?: "application/pdf" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() && fileUri != null) {
                        onUpload(title, fileUri!!, mimeType, category, description)
                    } else {
                        Toast.makeText(context, "Please select a file and title", Toast.LENGTH_SHORT).show()
                    }
                }
            ) { Text("Upload") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
        title = { Text("Upload New Document") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (optional)") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (optional)") })
                Button(onClick = { filePicker.launch("*/*") }) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (fileUri == null) "Select File" else "Change File")
                }
                if (fileUri != null) {
                    Text("Selected: ${fileUri.toString().substringAfterLast('/')}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    )
}

suspend fun uriToBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
