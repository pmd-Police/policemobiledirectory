package com.example.policemobiledirectory.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object MediaStoreImageHelper {

    private const val TAG = "MediaStoreImageHelper"

    suspend fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
                return@withContext BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from URI: $uri", e)
        }
        return@withContext null
    }

    suspend fun saveBitmapToCacheAndCompress(
        context: Context,
        bitmap: Bitmap,
        quality: Int = 80, // default compress to 80%
        maxWidth: Int = 1080,
        maxHeight: Int = 1080
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            // Resize if needed
            val scaledBitmap = if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val newWidth: Int
                val newHeight: Int
                if (ratio > 1) { // Landscape or square where width is the constraint
                    newWidth = maxWidth
                    newHeight = (maxWidth / ratio).toInt().coerceAtLeast(1) // Ensure height is at least 1
                } else { // Portrait or square where height is the constraint
                    newHeight = maxHeight
                    newWidth = (maxHeight * ratio).toInt().coerceAtLeast(1) // Ensure width is at least 1
                }
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else {
                bitmap
            }

            val filename = "CACHE_IMG_${System.currentTimeMillis()}.jpg"
            val cachePath = File(context.cacheDir, "image_cache")
            cachePath.mkdirs() // Ensure the cache directory exists
            val file = File(cachePath, filename)

            FileOutputStream(file).use { output ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                output.flush()
            }

            val fileUri = Uri.fromFile(file)
            Log.d(TAG, "Saved compressed image to cache: $fileUri (quality=$quality)")
            fileUri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save and compress image to cache", e)
            null
        }
    }

    suspend fun deleteCachedImage(fileUri: Uri?): Boolean = withContext(Dispatchers.IO) {
        if (fileUri == null) return@withContext false
        return@withContext try {
            if ("file" == fileUri.scheme) {
                val file = File(fileUri.path ?: return@withContext false)
                if (file.exists()) {
                    val deleted = file.delete()
                    if (deleted) {
                        Log.d(TAG, "Deleted cached image: $fileUri")
                    } else {
                        Log.w(TAG, "Failed to delete cached image file: $fileUri")
                    }
                    deleted
                } else {
                    Log.w(TAG, "Cache file not found for deletion: $fileUri")
                    false
                }
            } else {
                Log.w(TAG, "Cannot delete non-file URI scheme: $fileUri")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete cached image: $fileUri", e)
            false
        }
    }
}
