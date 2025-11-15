package com.example.policemobiledirectory.utils

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

private const val TAG = "FirebaseStorageHelper"

sealed class StorageResult<out T> {
    data class Success<T>(val data: T) : StorageResult<T>()
    data class Error(val exception: Exception) : StorageResult<Nothing>()
}

object FirebaseStorageHelper {

    private const val DEFAULT_FOLDER = "employee_photos"

    /**
     * Uploads an image to Firebase Storage and returns a [StorageResult] containing the download URL.
     */
    suspend fun uploadImageToFirebase(
        imageUri: Uri,
        employeeKgid: String,
        folder: String = DEFAULT_FOLDER
    ): StorageResult<String> {
        var fileName = ""
        return try {
            val storageRef = FirebaseStorage.getInstance().reference

            // Sanitize KGID for safe file naming
            val safeKgId = if (employeeKgid.isNotBlank()) {
                employeeKgid.replace(Regex("[^a-zA-Z0-9_-]"), "")
            } else {
                "unknown"
            }

            // Filename format: KGID_timestamp_UUID.jpg
            fileName = "${safeKgId}_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
            val imagePath = "$folder/$fileName"
            val imageRef = storageRef.child(imagePath)

            Log.d(TAG, "Uploading to Firebase path: ${imageRef.path}, URI: $imageUri")

            // Upload file
            imageRef.putFile(imageUri).await()

            // Fetch download URL
            val downloadUrl = imageRef.downloadUrl.await().toString()
            Log.d(TAG, "Upload success. Download URL: $downloadUrl")

            StorageResult.Success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed for KGID: $employeeKgid at $folder/$fileName", e)
            StorageResult.Error(e)
        }
    }

    /**
     * Deletes an image from Firebase Storage given its [photoUrl].
     */
    suspend fun deleteImage(photoUrl: String?): StorageResult<Boolean> {
        if (photoUrl.isNullOrEmpty()) {
            Log.w(TAG, "Skipping delete: photoUrl is null or empty")
            return StorageResult.Success(false)
        }
        if (!photoUrl.startsWith("https://firebasestorage.googleapis.com")) {
            Log.w(TAG, "Skipping delete: non-Firebase URL: $photoUrl")
            return StorageResult.Success(false)
        }

        return try {
            Log.d(TAG, "Deleting image from Firebase Storage: $photoUrl")
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(photoUrl)
            storageRef.delete().await()
            Log.d(TAG, "Delete success: $photoUrl")
            StorageResult.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed for: $photoUrl", e)
            StorageResult.Error(e)
        }
    }
}
