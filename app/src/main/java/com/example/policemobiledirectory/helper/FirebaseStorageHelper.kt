package com.example.policemobiledirectory.helper

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FirebaseStorageHelper {

    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    /**
     * Uploads a photo to Firebase Storage and returns its download URL.
     *
     * @param uri The Uri of the file to upload.
     * @param fileName Optional custom name for the file.
     *                 If not provided, a unique UUID name will be used.
     * @return The public download URL as a String.
     * @throws Exception if the upload fails.
     */
    suspend fun uploadPhoto(uri: Uri, fileName: String? = null): String {
        try {
            val safeFileName = fileName ?: UUID.randomUUID().toString()
            val photoRef = storageRef.child("employee_photos/$safeFileName")

            // Upload file
            val uploadTask = photoRef.putFile(uri).await()

            // Check upload metadata (optional safety)
            if (uploadTask.metadata == null) {
                throw Exception("Upload failed: No metadata returned")
            }

            // Get download URL
            return photoRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw Exception("Failed to upload photo: ${e.message}", e)
        }
    }

    /**
     * Deletes a photo from Firebase Storage using its download URL.
     *
     * @param photoUrl The URL of the photo to delete.
     */
    suspend fun deletePhoto(photoUrl: String) {
        try {
            storage.getReferenceFromUrl(photoUrl).delete().await()
        } catch (e: Exception) {
            throw Exception("Failed to delete photo: ${e.message}", e)
        }
    }
}
