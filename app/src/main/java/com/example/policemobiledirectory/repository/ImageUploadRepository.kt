package com.example.policemobiledirectory.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import org.json.JSONObject

/**
 * ✅ Uploads cropped image to Google Drive (via Apps Script)
 * Returns: Result.success(publicImageUrl)
 */
object ImageUploadRepository {

    // 🔗 Replace with your deployed Apps Script Web App URL
    private const val UPLOAD_URL =
        "https://script.google.com/macros/s/AKfycbyEqYeeUGeToFPwhdTD2xs7uEWOzlwIjYm1f41KJCWiQYL2Swipgg_y10xRekyV1s2fjQ/exec?action=uploadImage"

    suspend fun uploadImageToDrive(
        context: Context,
        imageUri: Uri,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // --- Convert image to Base64 ---
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.readBytes() ?: return@withContext Result.failure(Exception("Failed to read image"))
            inputStream.close()

            val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)
            val fileName = "IMG_${System.currentTimeMillis()}.jpg"

            // --- Create JSON body ---
            val jsonBody = JSONObject()
            jsonBody.put("image", base64Image)
            jsonBody.put("filename", fileName)
            val bodyBytes = jsonBody.toString().toByteArray(StandardCharsets.UTF_8)

            // --- Open connection ---
            val url = URL(UPLOAD_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.use { os ->
                os.write(bodyBytes)
                os.flush()
            }

            // --- Get response ---
            val responseCode = conn.responseCode
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }

            if (responseCode == 200) {
                val json = JSONObject(responseText)
                if (json.optBoolean("success")) {
                    val imageUrl = json.optString("url")
                    return@withContext Result.success(imageUrl)
                } else {
                    return@withContext Result.failure(Exception(json.optString("error", "Upload failed")))
                }
            } else {
                return@withContext Result.failure(Exception("HTTP Error: $responseCode"))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
