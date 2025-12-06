package com.example.policemobiledirectory.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopAppBar(title: String, navController: NavController) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            if (navController.previousBackStackEntry != null) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            } else {
                Spacer(modifier = Modifier.width(0.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
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
fun EmptySection(
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Description,
    message: String = "No items found"
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

suspend fun uriToBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        // For documents, read as-is (no compression)
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Compress and convert image URI to Base64 for gallery uploads
 * This reduces file size significantly to prevent timeout errors
 */
suspend fun uriToBase64Compressed(
    context: Context,
    uri: Uri,
    maxDimension: Int = 1920,
    quality: Int = 85
): String? = withContext(Dispatchers.IO) {
    var tempFile: File? = null
    try {
        val resolver = context.contentResolver
        
        // Get image dimensions
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { 
            BitmapFactory.decodeStream(it, null, boundsOptions) 
        }
        
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            // Fallback: read as-is if we can't decode dimensions
            val inputStream: InputStream? = resolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            return@withContext bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
        }
        
        // Calculate sample size for efficient loading
        val inSampleSize = calculateInSampleSize(boundsOptions, maxDimension)
        
        // Decode bitmap with sampling
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        
        val decodedBitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return@withContext null
        
        // Scale down if still too large
        val needsScaling = decodedBitmap.width > maxDimension || decodedBitmap.height > maxDimension
        val processedBitmap: Bitmap = if (needsScaling) {
            val ratio = minOf(
                maxDimension.toFloat() / decodedBitmap.width.toFloat(),
                maxDimension.toFloat() / decodedBitmap.height.toFloat()
            )
            val targetWidth = (decodedBitmap.width * ratio).roundToInt().coerceAtLeast(1)
            val targetHeight = (decodedBitmap.height * ratio).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decodedBitmap, targetWidth, targetHeight, true)
        } else {
            decodedBitmap
        }
        
        // Compress to JPEG and convert to Base64
        val outputStream = ByteArrayOutputStream()
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val compressedBytes = outputStream.toByteArray()
        
        // Clean up
        if (processedBitmap !== decodedBitmap) {
            decodedBitmap.recycle()
        }
        processedBitmap.recycle()
        outputStream.close()
        
        Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        tempFile?.delete()
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, maxDimension: Int): Int {
    var inSampleSize = 1
    val height = options.outHeight
    val width = options.outWidth
    
    if (height > maxDimension || width > maxDimension) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        
        while ((halfHeight / inSampleSize) >= maxDimension && 
               (halfWidth / inSampleSize) >= maxDimension) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

/**
 * Converts Google Drive URLs to direct image URLs for display
 * Handles various Drive URL formats:
 * - https://drive.google.com/file/d/FILE_ID/view?usp=sharing
 * - https://drive.google.com/file/d/FILE_ID/view
 * - https://drive.google.com/open?id=FILE_ID
 * - Already direct URLs (returns as-is)
 */
fun convertDriveUrlToDirectImageUrl(driveUrl: String): String {
    return try {
        // If already a direct image URL, return as-is
        if (driveUrl.contains("drive.google.com/uc?export=view")) {
            return driveUrl
        }
        
        // Extract file ID from various Drive URL formats
        val fileId = when {
            // Format: https://drive.google.com/file/d/FILE_ID/view...
            driveUrl.contains("/file/d/") -> {
                val startIndex = driveUrl.indexOf("/file/d/") + 8
                val endIndex = driveUrl.indexOf("/", startIndex).let { 
                    if (it == -1) driveUrl.indexOf("?", startIndex).let { q -> if (q == -1) driveUrl.length else q }
                    else it
                }
                driveUrl.substring(startIndex, endIndex)
            }
            // Format: https://drive.google.com/open?id=FILE_ID
            driveUrl.contains("?id=") -> {
                driveUrl.substringAfter("?id=").substringBefore("&")
            }
            // Format: https://drive.google.com/file/d/FILE_ID (no trailing path)
            else -> {
                val match = Regex("/([a-zA-Z0-9_-]{25,})").find(driveUrl)
                match?.groupValues?.get(1) ?: return driveUrl
            }
        }
        
        // Return direct image URL
        "https://drive.google.com/uc?export=view&id=$fileId"
    } catch (e: Exception) {
        // If conversion fails, return original URL
        driveUrl
    }
}