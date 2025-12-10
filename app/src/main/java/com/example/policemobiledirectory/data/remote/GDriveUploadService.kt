package com.example.policemobiledirectory.data.remote

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * 🚓 GDriveUploadService.kt
 *
 * Uploads files to your Google Drive via Google Apps Script Web App.
 *
 * 🔗 Deployed Script URL:
 * https://script.google.com/macros/s/AKfycbyEqYeeUGeToFPwhdTD2xs7uEWOzlwIjYm1f41KJCWiQYL2Swipgg_y10xRekyV1s2fjQ/exec
 *
 * ⚙️ Retrofit baseUrl (from ImageRepository.kt):
 * .baseUrl("https://script.google.com/macros/s/AKfycbyEqYeeUGeToFPwhdTD2xs7uEWOzlwIjYm1f41KJCWiQYL2Swipgg_y10xRekyV1s2fjQ/")
 *
 * ✅ Then append "?action=uploadImage" here.
 */
interface GDriveUploadService {

    // ✅ NEW: Base64 JSON upload (simpler for Apps Script)
    @POST("exec?action=uploadImage")
    suspend fun uploadPhotoJson(
        @Body body: Base64UploadRequest
    ): Response<ResponseBody>

    // Keep multipart as backup
    @Multipart
    @POST("exec?action=uploadImage")
    suspend fun uploadPhoto(
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>
}

/**
 * ✅ Base64 JSON request body with security token
 */
data class Base64UploadRequest(
    val image: String, // data:image/jpeg;base64,...
    val filename: String, // e.g., "98765.jpg"
    val token: String? = null, // Secret token for authentication
    val kgid: String? = null, // User's KGID for ownership verification
    val userEmail: String? = null // User's email for rate limiting
)

/**
 * ✅ Response data class for Google Drive upload
 */
data class GDriveUploadResponse(
    val success: Boolean,
    val url: String?,
    val id: String? = null,
    val error: String? = null
)
