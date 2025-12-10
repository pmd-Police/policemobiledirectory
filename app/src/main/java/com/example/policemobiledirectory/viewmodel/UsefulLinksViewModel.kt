package com.example.policemobiledirectory.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.model.ExternalLinkInfo
import com.example.policemobiledirectory.repository.AppIconRepository
import com.example.policemobiledirectory.utils.OperationStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import android.content.Context

/**
 * ViewModel responsible for useful links operations:
 * - Fetching useful links
 * - Adding/deleting useful links
 * - Icon management
 */
@HiltViewModel
class UsefulLinksViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val firestoreInstance = FirebaseFirestore.getInstance()
    private val appIconRepository by lazy { AppIconRepository.create(context) }

    private val _usefulLinks = MutableStateFlow<List<ExternalLinkInfo>>(emptyList())
    val usefulLinks: StateFlow<List<ExternalLinkInfo>> = _usefulLinks.asStateFlow()

    private val _operationStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val operationStatus: StateFlow<OperationStatus<String>> = _operationStatus.asStateFlow()

    fun fetchUsefulLinks() {
        viewModelScope.launch {
            try {
                val collection = firestoreInstance.collection("useful_links")
                val snapshot = try {
                    collection.get(Source.SERVER).await()
                } catch (serverError: Exception) {
                    Log.w("UsefulLinks", "Server fetch failed (${serverError.message}), falling back to cache")
                    collection.get(Source.CACHE).await()
                }

                // Temporary list for immediate show (no icons yet)
                _usefulLinks.value = snapshot.documents.mapNotNull { doc ->
                    val link = doc.toObject(ExternalLinkInfo::class.java) ?: return@mapNotNull null
                    link.copy(documentId = doc.id)
                }

                // Fetch icons in background
                val updatedLinks = snapshot.documents.mapNotNull { doc ->
                    val link = doc.toObject(ExternalLinkInfo::class.java) ?: return@mapNotNull null

                    val icon = if (!link.playStoreUrl.isNullOrBlank()) {
                        try {
                            val fetched = appIconRepository.getOrFetchAppIcon(link.playStoreUrl)

                            if (!fetched.isNullOrBlank()) {
                                if (link.iconUrl != fetched) {
                                    try {
                                        collection.document(doc.id).update("iconUrl", fetched).await()
                                        Log.d("IconUpdate", "Updated icon for ${link.name}")
                                    } catch (e: Exception) {
                                        Log.w("IconUpdate", "Failed to save icon for ${link.name}: ${e.message}")
                                    }
                                }
                                fetched
                            } else {
                                link.iconUrl
                            }
                        } catch (e: Exception) {
                            Log.e("IconFetch", "Error fetching icon for ${link.name}: ${e.message}")
                            link.iconUrl
                        }
                    } else {
                        link.iconUrl
                    }

                    link.copy(
                        iconUrl = icon,
                        documentId = doc.id
                    )
                }

                _usefulLinks.value = updatedLinks

            } catch (e: Exception) {
                Log.e("Firestore", "Failed to fetch useful links: ${e.message}")
                _usefulLinks.value = emptyList()
            }
        }
    }

    fun deleteUsefulLink(documentId: String) = viewModelScope.launch {
        try {
            _operationStatus.value = OperationStatus.Loading
            firestoreInstance.collection("useful_links").document(documentId).delete().await()

            // Remove from local state immediately
            _usefulLinks.value = _usefulLinks.value.filter { it.documentId != documentId }
            _operationStatus.value = OperationStatus.Success("Link deleted successfully")

            Log.d("UsefulLinks", "✅ Deleted link: $documentId")
        } catch (e: Exception) {
            Log.e("UsefulLinks", "❌ Failed to delete link: ${e.message}", e)
            _operationStatus.value = OperationStatus.Error("Failed to delete: ${e.message}")
        }
    }

    fun addUsefulLink(
        name: String,
        playStoreUrl: String,
        apkUrl: String,
        iconUrl: String,
        apkFileUri: Uri?,
        imageUri: Uri?
    ) = viewModelScope.launch {
        _operationStatus.value = OperationStatus.Loading

        try {
            var finalIconUrl = iconUrl.trim().takeIf { it.isNotBlank() }
            var finalApkUrl = apkUrl.trim().takeIf { it.isNotBlank() }

            // Upload APK file if provided
            if (finalApkUrl.isNullOrBlank() && apkFileUri != null) {
                Log.d("UsefulLinks", "Uploading APK file: $apkFileUri")
                finalApkUrl = uploadUsefulLinkApk(apkFileUri, name)
                if (finalApkUrl == null) {
                    throw Exception("Failed to upload APK file. Please check your internet connection and try again.")
                }
                Log.d("UsefulLinks", "APK uploaded successfully: $finalApkUrl")
            }

            // Upload icon image if provided
            if (finalIconUrl.isNullOrBlank() && imageUri != null) {
                Log.d("UsefulLinks", "Uploading icon image: $imageUri")
                finalIconUrl = uploadUsefulLinkIcon(imageUri, name)
                if (finalIconUrl == null) {
                    Log.w("UsefulLinks", "Icon upload failed, continuing without icon")
                } else {
                    Log.d("UsefulLinks", "Icon uploaded successfully: $finalIconUrl")
                }
            }

            // Fetch icon from Play Store if no icon provided
            if (finalIconUrl.isNullOrBlank() && playStoreUrl.isNotBlank()) {
                try {
                    finalIconUrl = appIconRepository.getOrFetchAppIcon(playStoreUrl)
                    Log.d("UsefulLinks", "Fetched icon from Play Store: $finalIconUrl")
                } catch (e: Exception) {
                    Log.w("UsefulLinks", "Icon fetch fallback failed: ${e.message}")
                }
            }

            val data = mutableMapOf<String, Any>(
                "name" to name.trim(),
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )

            if (playStoreUrl.isNotBlank()) data["playStoreUrl"] = playStoreUrl.trim()
            finalApkUrl?.let {
                data["apkUrl"] = it
                Log.d("UsefulLinks", "Saving link with APK URL: $it")
            }
            finalIconUrl?.let { data["iconUrl"] = it }

            // Two separate flows validated:
            // Flow 1: Play Store link (APK optional)
            // Flow 2: APK file/URL (Play Store link optional)
            if (!data.containsKey("playStoreUrl") && !data.containsKey("apkUrl")) {
                throw IllegalArgumentException("Provide either Play Store URL OR APK file/URL")
            }

            Log.d("UsefulLinks", "Saving to Firestore: $data")
            firestoreInstance.collection("useful_links").add(data).await()
            Log.d("UsefulLinks", "✅ Link saved successfully to Firestore")

            _operationStatus.value = OperationStatus.Success("Link added")
            fetchUsefulLinks()
        } catch (e: Exception) {
            Log.e("UsefulLinks", "❌ Failed to add link: ${e.message}", e)
            _operationStatus.value = OperationStatus.Error(e.message ?: "Failed to add link")
        }
    }

    private suspend fun uploadUsefulLinkApk(apkUri: Uri, entryName: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val safeName = entryName.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .ifBlank { "link" }
            val fileName = "${safeName}_${System.currentTimeMillis()}_${UUID.randomUUID()}.apk"
            val storageRef = FirebaseStorage.getInstance()
                .reference
                .child("useful_links/apks/$fileName")

            storageRef.putFile(apkUri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("UsefulLinks", "APK upload failed: ${e.message}", e)
            null
        }
    }

    private suspend fun uploadUsefulLinkIcon(imageUri: Uri, entryName: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val safeName = entryName.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .ifBlank { "link" }
            val fileName = "${safeName}_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
            val storageRef = FirebaseStorage.getInstance()
                .reference
                .child("useful_links/icons/$fileName")

            storageRef.putFile(imageUri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("UsefulLinks", "Icon upload failed: ${e.message}", e)
            null
        }
    }

    fun resetOperationStatus() {
        _operationStatus.value = OperationStatus.Idle
    }
}



