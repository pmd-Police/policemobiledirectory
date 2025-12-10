package com.example.policemobiledirectory.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.policemobiledirectory.data.remote.*
import com.example.policemobiledirectory.utils.OperationStatus
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * 🚓 ImageRepository.kt
 *
 * Handles:
 * 1️⃣ Uploading officer images to Google Drive (via Google Apps Script)
 * 2️⃣ Updating Firestore with the public Drive URL
 * 3️⃣ Deleting officer images from Drive when employee is removed
 */
class ImageRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // Security helper for Apps Script auth token
    private val securityConfig = com.example.policemobiledirectory.utils.SecurityConfig(context)

    // ⚙️ Retrofit setup — your deployed Apps Script base URL (no action params here)
    private val gson = GsonBuilder()
        .setLenient() // ✅ Handle malformed JSON gracefully
        .create()
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://script.google.com/macros/s/AKfycbyEqYeeUGeToFPwhdTD2xs7uEWOzlwIjYm1f41KJCWiQYL2Swipgg_y10xRekyV1s2fjQ/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(okHttpClient)
        .build()

    private val uploadService = retrofit.create(GDriveUploadService::class.java)
    private val deleteService = retrofit.create(GDriveDeleteService::class.java)

    /**
     * Extracts error message from HTML error pages
     */
    private fun extractErrorMessageFromHtml(html: String): String? {
        return try {
            // Try to find common error patterns in HTML
            val patterns = listOf(
                Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE),
                Regex("<div[^>]*style=\"[^\"]*text-align:center[^\"]*\"[^>]*>(.*?)</div>", RegexOption.IGNORE_CASE),
                Regex("Script function not found: (.*?)(?:<|\\n)", RegexOption.IGNORE_CASE),
                Regex("Exception: (.*?)(?:<|\\n)", RegexOption.IGNORE_CASE),
                Regex("Error: (.*?)(?:<|\\n)", RegexOption.IGNORE_CASE),
                Regex("<h1[^>]*>(.*?)</h1>", RegexOption.IGNORE_CASE),
                Regex("<div[^>]*class=\"[^\"]*error[^\"]*\"[^>]*>(.*?)</div>", RegexOption.IGNORE_CASE)
            )
            
            for (pattern in patterns) {
                val match = pattern.find(html)
                if (match != null) {
                    val message = match.groupValues.getOrNull(1)?.trim()
                    if (!message.isNullOrEmpty() && message.length < 300) {
                        Log.d("ImageRepository", "Extracted error message: $message")
                        return message
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * ☁️ Uploads an image to Google Drive via Apps Script
     * and updates Firestore with the returned URL.
     */
    fun uploadOfficerImage(uri: Uri, userId: String): Flow<OperationStatus<String>> = flow {
        Log.d("ImageRepository", "═══════════════════════════════════════")
        Log.d("ImageRepository", "📤 Starting upload for userId: $userId")
        Log.d("ImageRepository", "📤 URI: $uri")
        
        emit(OperationStatus.Loading)
        Log.d("ImageRepository", "✅ Emitted Loading status")

        var tempFile: File? = null
        try {
        Log.d("ImageRepository", "Step 1: Copying & compressing URI to temp file...")
        val originalSize = try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
        } catch (e: Exception) {
            null
        }

        tempFile = withContext(ioDispatcher) { copyAndCompressImage(context, uri) }
        if (tempFile == null || !tempFile.exists()) {
            Log.e("ImageRepository", "❌ Failed to prepare temp file for upload")
            emit(OperationStatus.Error("Failed to process selected image"))
            return@flow
        }

        Log.d("ImageRepository", "✅ Temp file ready: ${tempFile.absolutePath}")
        Log.d("ImageRepository", "📏 Original size: ${originalSize ?: -1} bytes, Compressed size: ${tempFile.length()} bytes")

            // Step 2️⃣ Convert to base64 and prepare JSON upload
            Log.d("ImageRepository", "Step 2: Converting to base64...")
            val fileBytes = tempFile.readBytes()
            val base64String = Base64.encodeToString(fileBytes, Base64.NO_WRAP)
            val base64Image = "data:image/jpeg;base64,$base64String"
            val jsonBody = Base64UploadRequest(
                image = base64Image,
                filename = "$userId.jpg",
                token = securityConfig.getSecretToken(),
                kgid = userId,
                userEmail = null // populated when available
            )
            Log.d("ImageRepository", "✅ Base64 prepared, size: ${base64String.length} chars")

            // Step 3️⃣ Upload to Apps Script using JSON
            Log.d("ImageRepository", "Step 3: Uploading to Apps Script (base64 JSON)...")
            Log.d("ImageRepository", "Upload URL: exec?action=uploadImage")
            val response = uploadService.uploadPhotoJson(jsonBody)
            Log.d("ImageRepository", "✅ Received response from server")

            // ✅ Log response for debugging
            val responseCode = response.code()
            val responseBody: ResponseBody? = response.body()
            val contentType = responseBody?.contentType()?.toString() ?: "unknown"
            Log.d("ImageRepository", "Upload response code: $responseCode, isSuccessful: ${response.isSuccessful}, contentType: $contentType")

            // ✅ Read raw response body (can only be read once!)
            val rawResponseText = try {
                responseBody?.string() ?: ""
            } catch (e: Exception) {
                Log.e("ImageRepository", "Failed to read response body: ${e.message}", e)
                ""
            }

            // ✅ Log full response (not just first 500 chars) - but limit for readability
            Log.d("ImageRepository", "Raw response (full, ${rawResponseText.length} chars): $rawResponseText")

            if (response.isSuccessful && rawResponseText.isNotEmpty()) {
                // ✅ Try to parse as JSON
                val result: GDriveUploadResponse? = try {
                    // Check if response looks like JSON
                    val trimmed = rawResponseText.trim()
                    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                        val parsed = gson.fromJson(trimmed, GDriveUploadResponse::class.java)
                        
                        // ✅ Log debug array if it exists (for debugging Apps Script)
                        try {
                            val jsonObject = gson.fromJson(trimmed, JsonObject::class.java)
                            if (jsonObject.has("debug")) {
                                val debugArray = jsonObject.getAsJsonArray("debug")
                                Log.e("ImageRepository", "══════════════════════════════════════")
                                Log.e("ImageRepository", "🔍 APPS SCRIPT DEBUG INFO:")
                                debugArray.forEachIndexed { index, element ->
                                    Log.e("ImageRepository", "[$index] ${element.asString}")
                                }
                                Log.e("ImageRepository", "══════════════════════════════════════")
                            }
                        } catch (e: Exception) {
                            Log.d("ImageRepository", "Could not parse debug array: ${e.message}")
                        }
                        
                        parsed
                    } else {
                        Log.e("ImageRepository", "Response is not JSON. Content-Type: $contentType")
                        Log.e("ImageRepository", "Response starts with: ${trimmed.take(100)}")
                        null
                    }
                } catch (parseErr: Exception) {
                    Log.e("ImageRepository", "JSON parsing failed: ${parseErr.message}", parseErr)
                    Log.e("ImageRepository", "Failed to parse response: ${rawResponseText.take(200)}")
                    null
                }
                
                if (result != null && result.success && !result.url.isNullOrEmpty()) {
                    val photoUrl = result.url!!
                    Log.d("ImageRepository", "Upload successful, URL: $photoUrl")
                    emit(OperationStatus.Success(photoUrl))
                } else {
                    // Response is not JSON or parsing failed
                    val errorMsg = if (result != null && result.error != null) {
                        result.error!!
                    } else {
                        // Check if response is HTML (Apps Script error page)
                        val trimmedResponse = rawResponseText.trimStart()
                        if (trimmedResponse.startsWith("<!DOCTYPE") || trimmedResponse.startsWith("<html")) {
                            // Try to extract error message from HTML
                            val errorMessage = extractErrorMessageFromHtml(rawResponseText)
                            val htmlError = "Server returned HTML instead of JSON. ${errorMessage ?: "This usually means:"}\n" +
                                    "1. The Apps Script web app is not deployed as 'Execute as: Me' and 'Who has access: Anyone'\n" +
                                    "2. The doPost() function is not handling 'action=uploadImage' correctly\n" +
                                    "3. The script encountered an error. Check Apps Script execution logs"
                            Log.e("ImageRepository", "HTML Response received: ${rawResponseText.take(1000)}")
                            htmlError
                        } else if (rawResponseText.isEmpty()) {
                            "Server returned empty response. Please check server configuration."
                        } else {
                            "Invalid server response format. Expected JSON but got: ${contentType}. Response: ${rawResponseText.take(200)}"
                        }
                    }
                    Log.e("ImageRepository", "Upload failed - result: $result, response type: $contentType")
                    emit(OperationStatus.Error(errorMsg))
                }
            } else {
                // HTTP error (non-2xx response)
                val errorBodyText = try {
                    response.errorBody()?.string() ?: rawResponseText.takeIf { it.isNotEmpty() } ?: "No error details"
                } catch (e: Exception) {
                    rawResponseText.takeIf { it.isNotEmpty() } ?: "Could not read error response"
                }
                Log.e("ImageRepository", "Upload failed - HTTP ${response.code()}, contentType: $contentType, body: ${errorBodyText.take(500)}")
                
                // Check if it's a common error
                val errorMsg = when (response.code()) {
                    404 -> "Upload endpoint not found. Please check server configuration."
                    500 -> "Server error (500). Please try again later."
                    403 -> "Access forbidden. Please check server permissions."
                    400 -> "Bad request. The uploaded file may be invalid."
                    else -> "Upload failed — server error ${response.code()}. Please try again."
                }
                emit(OperationStatus.Error(errorMsg))
            }
        } catch (e: com.google.gson.JsonSyntaxException) {
            // Handle JSON parsing errors specifically
            Log.e("ImageRepository", "JSON parsing error: ${e.message}", e)
            emit(OperationStatus.Error("Server response format error. Please try again."))
        } catch (e: java.io.IOException) {
            // Handle network/IO errors
            Log.e("ImageRepository", "IO error: ${e.message}", e)
            emit(OperationStatus.Error("Network error: ${e.message ?: "Check your internet connection"}"))
        } catch (e: Exception) {
            // Catch all other exceptions
            Log.e("ImageRepository", "Unexpected error: ${e.message}", e)
            val errorMsg = e.message ?: "Unknown error"
            emit(OperationStatus.Error("Upload failed: $errorMsg"))
        } finally {
            // Clean up temp file
            try {
                tempFile?.delete()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }.flowOn(Dispatchers.IO) // Move flowOn to apply to the entire flow

    /**
     * 🗑️ Deletes officer image from Google Drive
     * using fileId (preferred) or userId (fallback)
     */
    fun deleteOfficerImage(
        fileId: String?,
        userId: String?
    ): Flow<OperationStatus<String>> = flow {
        emit(OperationStatus.Loading)

        try {
            val response = when {
                !fileId.isNullOrEmpty() -> deleteService.deleteByFileId(fileId)
                !userId.isNullOrEmpty() -> deleteService.deleteByUserId(userId)
                else -> null
            }

            if (response != null && response.isSuccessful) {
                val result = response.body()
                if (result != null && result.success) {
                    emit(OperationStatus.Success(result.message ?: "Deleted successfully."))
                } else {
                    emit(OperationStatus.Error(result?.error ?: "Delete failed on server."))
                }
            } else {
                emit(OperationStatus.Error("Network or API error during deletion."))
            }

        } catch (e: Exception) {
            emit(OperationStatus.Error("Delete failed: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}

private fun copyAndCompressImage(
    context: Context,
    uri: Uri,
    maxDimension: Int = 1600,
    quality: Int = 80
): File? {
    val resolver = context.contentResolver
    return try {
        val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }

        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            // Fallback: copy as-is
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            } ?: run {
                tempFile.delete()
                throw IllegalStateException("Unable to open input stream")
            }
            return tempFile
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOptions, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decodedBitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: run {
            tempFile.delete()
            throw IllegalStateException("Unable to decode image stream")
        }

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

        FileOutputStream(tempFile).use { out ->
            processedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }

        if (processedBitmap !== decodedBitmap) {
            decodedBitmap.recycle()
        }
        processedBitmap.recycle()

        tempFile
    } catch (e: Exception) {
        Log.e("ImageRepository", "Image compression failed: ${e.message}", e)
        null
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, maxDimension: Int): Int {
    var inSampleSize = 1
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    if (height > maxDimension || width > maxDimension) {
        var halfHeight = height / 2
        var halfWidth = width / 2

        while ((halfHeight / inSampleSize) >= maxDimension || (halfWidth / inSampleSize) >= maxDimension) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
