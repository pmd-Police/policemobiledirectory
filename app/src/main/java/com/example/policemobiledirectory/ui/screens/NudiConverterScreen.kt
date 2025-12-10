package com.example.policemobiledirectory.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.activity.compose.BackHandler
import com.example.policemobiledirectory.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import android.graphics.Color as AndroidColor

// --- RENAMED ENUM TO FIX DUPLICATE CLASS ERROR ---
enum class NudiExportFormat {
    TXT, PDF, DOCX
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NudiConverterScreen(
    navController: NavController,
    viewModel: NudiConverterViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Handle back button to navigate to home screen
    BackHandler {
        // Navigate to home screen, clearing back stack up to (but not including) EMPLOYEE_LIST
        navController.navigate(Routes.EMPLOYEE_LIST) {
            popUpTo(Routes.EMPLOYEE_LIST) { inclusive = false }
            launchSingleTop = true
        }
    }

    LaunchedEffect(context) {
        viewModel.attachContext(context)
    }

    var showShareDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Updated to use new Enum
    var pendingSaveFormat by remember { mutableStateOf<NudiExportFormat?>(null) }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> viewModel.handleFile(uri) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        try {
            val format = pendingSaveFormat
            pendingSaveFormat = null
            if (uri == null || format == null) {
                Toast.makeText(context, "Folder not selected", Toast.LENGTH_SHORT).show()
                showSaveDialog = false
                return@rememberLauncherForActivityResult
            }

            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                Log.w("NudiConverter", "Could not take persistable permission", e)
            } catch (e: Exception) {
                Log.w("NudiConverter", "Unexpected error taking permission", e)
            }

            // Move file operations to background thread
            coroutineScope.launch {
                try {
                    val success = withContext(Dispatchers.IO) {
                        try {
                            context.saveToFolder(uri, state.outputText, format)
                        } catch (e: Throwable) {
                            Log.e("NudiConverter", "Fatal error in saveToFolder", e)
                            e.printStackTrace()
                            false
                        }
                    }
                    if (success) {
                        Toast.makeText(context, "Saved to folder", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorMsg = when (format) {
                            NudiExportFormat.DOCX -> "Failed to save DOCX. Check Logcat"
                            NudiExportFormat.PDF -> "Failed to save PDF"
                            NudiExportFormat.TXT -> "Failed to save TXT"
                        }
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("NudiConverter", "Error in save coroutine", e)
                    val errorMsg = when (format) {
                        NudiExportFormat.DOCX -> "DOCX save error: ${e.message}"
                        else -> "Save error: ${e.message ?: "Unknown error"}"
                    }
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                } finally {
                    showSaveDialog = false
                }
            }
        } catch (e: Exception) {
            Log.e("NudiConverter", "Fatal error in folder picker launcher", e)
            Toast.makeText(context, "Unexpected error: ${e.message}", Toast.LENGTH_LONG).show()
            showSaveDialog = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nudi Converter") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Column(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.End
                    ) {
                        ConverterMode.values().forEach { mode ->
                            val selected = state.mode == mode
                            Text(
                                text = if (mode == ConverterMode.ASCII_TO_UNICODE)
                                    "ASCII → Unicode" else "Unicode → ASCII",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    textDecoration = if (selected) TextDecoration.Underline else TextDecoration.None
                                ),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                modifier = Modifier.clickable { viewModel.toggleMode(mode) }
                            )
                        }
                    }

                    TextButton(onClick = { viewModel.toggleSpace() }) {
                        Text(
                            text = if (state.removeSpace) "Remove Space ✓" else "Remove Space",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // INPUT CARD
            ConverterCard(
                text = state.inputText,
                label = "Paste OCR / File Text",
                height = 200.dp,
                onValueChange = { viewModel.updateInput(it) }
            ) {
                state.fileName?.let {
                    Text(
                        text = "File: $it",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { viewModel.reset() }) {
                        Text("RESET")
                    }

                    Spacer(Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            fileLauncher.launch(
                                arrayOf(
                                    "text/plain",
                                    "application/pdf",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                )
                            )
                        }
                    ) {
                        Text("UPLOAD")
                    }

                    Spacer(Modifier.width(8.dp))

                    Text("pdf · docx · txt", style = MaterialTheme.typography.bodySmall)
                }
            }

            // OUTPUT CARD
            ConverterCard(
                text = state.outputText,
                label = "Converted Text",
                height = 300.dp,
                onValueChange = { viewModel.updateOutput(it) }
            )

            Spacer(Modifier.height(12.dp))

            // BUTTON ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton("COPY") {
                    if (state.outputText.isNotBlank()) {
                        clipboard.setText(AnnotatedString(state.outputText))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }
                }

                ActionButton("SAVE") {
                    if (state.outputText.isNotBlank()) showSaveDialog = true
                }

                ActionButton("SHARE") {
                    if (state.outputText.isNotBlank()) showShareDialog = true
                }
            }

            Spacer(Modifier.height(100.dp))
        }

        // SAVE DIALOG
        if (showSaveDialog) {
            FormatSelectionDialog(
                title = "Save to Folder",
                onDismiss = { showSaveDialog = false },
                onFormatSelected = {
                    pendingSaveFormat = it
                    folderPickerLauncher.launch(null)
                }
            )
        }

        // SHARE DIALOG
        if (showShareDialog) {
            FormatSelectionDialog(
                title = "Share as",
                onDismiss = { showShareDialog = false },
                onFormatSelected = {
                    // Run generation in background to avoid UI stutters
                    coroutineScope.launch(Dispatchers.IO) {
                        val file = context.generateFile(state.outputText, it)
                        val mime = when (it) {
                            NudiExportFormat.TXT -> "text/plain"
                            NudiExportFormat.PDF -> "application/pdf"
                            NudiExportFormat.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        }
                        withContext(Dispatchers.Main) {
                            if (file != null) {
                                context.shareFile(file, mime)
                            } else {
                                Toast.makeText(context, "Failed to generate file for sharing", Toast.LENGTH_SHORT).show()
                            }
                            showShareDialog = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun FormatSelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    onFormatSelected: (NudiExportFormat) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onFormatSelected(NudiExportFormat.TXT) }) {
                    Text("Text File (.txt)")
                }
                Button(onClick = { onFormatSelected(NudiExportFormat.PDF) }) {
                    Text("PDF Document (.pdf)")
                }
                Button(onClick = { onFormatSelected(NudiExportFormat.DOCX) }) {
                    Text("Word Document (.docx)")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}

@Composable
fun ConverterCard(
    text: String,
    label: String,
    height: androidx.compose.ui.unit.Dp,
    onValueChange: (String) -> Unit,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
            OutlinedTextField(
                value = text,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                maxLines = Int.MAX_VALUE,
                label = { Text(label) }
            )
        }
    }
}

@Composable
fun RowScope.ActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Text(text, maxLines = 1)
    }
}

/* ---------- FILE UTILS ---------- */

private fun Context.conversionsDir(): File {
    val dir = File(filesDir, "conversions")
    if (!dir.exists()) {
        val created = dir.mkdirs()
        if (!created && !dir.exists()) {
            Log.e("NudiConverter", "Failed to create conversions directory")
            throw IllegalStateException("Cannot create conversions directory")
        }
    }
    if (!dir.canWrite()) {
        Log.e("NudiConverter", "Conversions directory is not writable")
        throw IllegalStateException("Conversions directory is not writable")
    }
    return dir
}

private fun Context.timestampName(ext: String): String {
    val ts = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(Date())
    return "PMD_nc_${ts}.$ext"
}

fun Context.generateFile(text: String, format: NudiExportFormat): File? {
    return when (format) {
        NudiExportFormat.TXT -> exportToTxt(text)
        NudiExportFormat.PDF -> exportToPdf(text)
        NudiExportFormat.DOCX -> exportToDocx(text)
    }
}

fun Context.exportToTxt(text: String): File? {
    if (text.isBlank()) return null
    val file = File(conversionsDir(), timestampName("txt"))
    file.writeText(text)
    return file
}

fun Context.exportToPdf(text: String): File? {
    if (text.isBlank()) return null

    val file = File(conversionsDir(), timestampName("pdf"))
    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdf.startPage(pageInfo)

    val canvas = page.canvas
    val paint = Paint().apply {
        textSize = 12f
        color = android.graphics.Color.BLACK
    }

    val margin = 40f
    val maxWidth = pageInfo.pageWidth - margin * 2
    var y = margin + paint.fontSpacing

    for (line in text.lines()) {
        var content = line
        while (content.isNotEmpty()) {
            val length = paint.breakText(content, true, maxWidth, null)
            canvas.drawText(content.substring(0, length), margin, y, paint)
            content = content.substring(length)
            y += paint.fontSpacing
            if (y > pageInfo.pageHeight - margin) break
        }
    }

    pdf.finishPage(page)
    FileOutputStream(file).use { pdf.writeTo(it) }
    pdf.close()

    return file
}

// --- DOCX EXPORT WITH CRASH FIXES ---
fun Context.exportToDocx(text: String): File? {
    // 1. Initialize System properties for StAX
    ensureStaxConfigured()
    // 2. Initialize Temp dir
    ensurePoiTempDir()
    // 3. CRITICAL FIX: Initialize ClassLoader for XMLBeans
    ensureXmlBeansClassLoader()

    var doc: org.apache.poi.xwpf.usermodel.XWPFDocument? = null
    var outputStream: FileOutputStream? = null

    return try {
        // Double check classes exist
        Class.forName("org.apache.poi.xwpf.usermodel.XWPFDocument")

        val dir = conversionsDir()
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, timestampName("docx"))
        Log.d("NudiConverter", "Creating DOCX file: ${file.absolutePath}")

        // Safely instantiate XWPFDocument
        try {
            doc = org.apache.poi.xwpf.usermodel.XWPFDocument()
        } catch (e: Error) { // Catch generic Error because FactoryConfigurationError extends Error, not Exception
            Log.e("NudiConverter", "StAX Error caught, attempting manual override", e)
            // Last ditch effort for StAX
            System.setProperty("javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl")
            System.setProperty("javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl")
            System.setProperty("javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl")

            // Retry
            try {
                doc = org.apache.poi.xwpf.usermodel.XWPFDocument()
            } catch (retryEx: Exception) {
                Log.e("NudiConverter", "Retry failed", retryEx)
                return null
            }
        }

        val para = doc!!.createParagraph()
        val run = para.createRun()
        run.setText(text)

        outputStream = FileOutputStream(file)
        doc.write(outputStream)
        outputStream.flush()

        if (!file.exists() || file.length() == 0L) {
            Log.e("NudiConverter", "DOCX file was not created or is empty")
            file.delete()
            return null
        }

        Log.d("NudiConverter", "DOCX file created successfully")
        file
    } catch (e: Exception) {
        Log.e("NudiConverter", "Error creating DOCX file", e)
        null
    } finally {
        try { outputStream?.close() } catch (e: Exception) {}
        try { doc?.close() } catch (e: Exception) {}
    }
}

fun Context.saveToFolder(treeUri: Uri, text: String, format: NudiExportFormat): Boolean {
    if (text.isBlank()) return false

    var tempFile: File? = null

    return try {
        val targetDir = DocumentFile.fromTreeUri(this, treeUri)
        if (targetDir == null || !targetDir.canWrite()) {
            Log.e("NudiConverter", "Target directory is null or not writable")
            return false
        }

        val mime = when (format) {
            NudiExportFormat.TXT -> "text/plain"
            NudiExportFormat.PDF -> "application/pdf"
            NudiExportFormat.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        }

        tempFile = generateFile(text, format)
        if (tempFile == null || !tempFile.exists()) {
            Log.e("NudiConverter", "Failed to generate temp file for format: $format")
            return false
        }

        val targetFile = targetDir.createFile(mime, tempFile.name)
        if (targetFile == null) {
            Log.e("NudiConverter", "Failed to create target file in selected folder")
            return false
        }

        contentResolver.openOutputStream(targetFile.uri)?.use { output ->
            tempFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }
        true
    } catch (e: Exception) {
        Log.e("NudiConverter", "Error saving file to folder", e)
        e.printStackTrace()
        false
    } finally {
        tempFile?.delete()
    }
}

fun Context.shareFile(file: File, mime: String) {
    try {
        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider", // Ensure this matches your AndroidManifest
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share via"))
    } catch (e: Exception) {
        Log.e("NudiConverter", "Share Error", e)
        Toast.makeText(this, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// --- HELPER FUNCTIONS (CRITICAL FOR POI ON ANDROID) ---

private val staxConfigured = AtomicBoolean(false)

private fun ensureStaxConfigured() {
    if (staxConfigured.compareAndSet(false, true)) {
        try {
            val factories = listOf(
                "javax.xml.stream.XMLEventFactory",
                "javax.xml.stream.XMLInputFactory",
                "javax.xml.stream.XMLOutputFactory",
                "org.apache.poi.javax.xml.stream.XMLEventFactory",
                "org.apache.poi.javax.xml.stream.XMLInputFactory",
                "org.apache.poi.javax.xml.stream.XMLOutputFactory"
            )

            val impls = listOf(
                "com.fasterxml.aalto.stax.EventFactoryImpl",
                "com.fasterxml.aalto.stax.InputFactoryImpl",
                "com.fasterxml.aalto.stax.OutputFactoryImpl",
                "com.fasterxml.aalto.stax.EventFactoryImpl",
                "com.fasterxml.aalto.stax.InputFactoryImpl",
                "com.fasterxml.aalto.stax.OutputFactoryImpl"
            )

            for (i in factories.indices) {
                System.setProperty(factories[i], impls[i])
            }
            Log.d("NudiConverter", "StAX providers configured")
        } catch (e: Exception) {
            Log.e("NudiConverter", "StAX config failed", e)
        }
    }
}

private fun ensurePoiTempDir() {
    val tmp = System.getProperty("java.io.tmpdir")
    if (tmp == null || !File(tmp).exists()) {
        runCatching {
            val fallback = File("/data/local/tmp")
            if (fallback.exists()) System.setProperty("java.io.tmpdir", fallback.absolutePath)
        }
    }
}

// THIS FUNCTION FIXES "SchemaTypeLoaderException" on Android
private fun ensureXmlBeansClassLoader() {
    try {
        // We must use the ClassLoader that loaded the POI Schemas
        // to allow XMLBeans to resolve types like 'xml:space'
        val schemaClass = Class.forName("org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody")
        val classLoader = schemaClass.classLoader
        Thread.currentThread().contextClassLoader = classLoader
    } catch (e: ClassNotFoundException) {
        Log.e("NudiConverter", "POI Schemas JAR missing", e)
    } catch (e: Exception) {
        Log.w("NudiConverter", "Failed to set ContextClassLoader", e)
    }
}
