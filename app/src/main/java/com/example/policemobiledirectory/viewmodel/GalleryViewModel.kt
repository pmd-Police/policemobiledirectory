package com.example.policemobiledirectory.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.policemobiledirectory.data.local.SessionManager
import com.example.policemobiledirectory.model.*
import com.example.policemobiledirectory.repository.GalleryRepository
import com.example.policemobiledirectory.utils.ErrorHandler
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.utils.PerformanceLogger
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: GalleryRepository,
    private val sessionManager: SessionManager,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // State management with OperationStatus
    private val _galleryImages = MutableStateFlow<List<GalleryImage>>(emptyList())
    val galleryImages: StateFlow<List<GalleryImage>> = _galleryImages.asStateFlow()

    private val _galleryStatus = MutableStateFlow<OperationStatus<List<GalleryImage>>>(OperationStatus.Idle)
    val galleryStatus: StateFlow<OperationStatus<List<GalleryImage>>> = _galleryStatus.asStateFlow()

    private val _uploadStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val uploadStatus: StateFlow<OperationStatus<String>> = _uploadStatus.asStateFlow()

    private val _deleteStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val deleteStatus: StateFlow<OperationStatus<String>> = _deleteStatus.asStateFlow()

    // In-memory cache with timestamp
    private var cachedImages: List<GalleryImage>? = null
    private var cacheTimestamp: Long = 0
    private val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes

    // Computed properties for convenience
    val isLoading: Boolean get() = _galleryStatus.value is OperationStatus.Loading
    val error: String? get() = (_galleryStatus.value as? OperationStatus.Error)?.message

    fun clearStatus() {
        _uploadStatus.value = OperationStatus.Idle
        _deleteStatus.value = OperationStatus.Idle
    }

    /**
     * Fetch gallery images with caching, error handling, and performance tracking
     */
    fun fetchGalleryImages(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Return cached data if available and not expired
            if (!forceRefresh && cachedImages != null && 
                (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION_MS) {
                _galleryImages.value = cachedImages!!
                _galleryStatus.value = OperationStatus.Success(cachedImages!!)
                return@launch
            }

            _galleryStatus.value = OperationStatus.Loading
            
            try {
                val images = PerformanceLogger.measureNetworkOperation("gallery", "GET") {
                    repository.fetchGalleryImages()
                }
                
                val imageList = images ?: emptyList()
                
                // Update cache
                cachedImages = imageList
                cacheTimestamp = System.currentTimeMillis()
                
                _galleryImages.value = imageList
                _galleryStatus.value = OperationStatus.Success(imageList)
                
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleException(e, "GalleryViewModel.fetchGalleryImages")
                
                // Return cached data if available, even if expired
                if (cachedImages != null) {
                    _galleryImages.value = cachedImages!!
                    _galleryStatus.value = OperationStatus.Error(
                        "Using cached data. ${errorInfo.userFriendlyMessage}"
                    )
                } else {
                    _galleryStatus.value = OperationStatus.Error(errorInfo.userFriendlyMessage)
                }
                
                // Retry if error is retryable
                if (errorInfo.shouldRetry) {
                    delay(errorInfo.retryDelay)
                    fetchGalleryImages(forceRefresh = true)
                }
            }
        }
    }

    /**
     * Upload gallery image with error handling and performance tracking
     */
    fun uploadGalleryImage(
        title: String,
        fileBase64: String,
        mimeType: String,
        category: String?,
        description: String?
    ) {
        viewModelScope.launch {
            _uploadStatus.value = OperationStatus.Loading
            
            try {
                val userEmail = sessionManager.userEmail.first()
                val request = GalleryUploadRequest(
                    title = title,
                    fileBase64 = fileBase64,
                    mimeType = mimeType,
                    category = category,
                    description = description,
                    userEmail = userEmail
                )
                
                val response = PerformanceLogger.measureNetworkOperation("gallery/upload", "POST") {
                    repository.uploadGalleryImage(request)
                }
                
                if (response.success) {
                    _uploadStatus.value = OperationStatus.Success("Image uploaded successfully")
                    
                    // Sync to Firestore for real-time updates (non-blocking)
                    val imageUrl = response.url ?: ""
                    if (imageUrl.isNotBlank()) {
                        launch {
                            syncToFirestore(imageUrl)
                        }
                    }
                    
                    // Invalidate cache and refresh
                    invalidateCache()
                    // Retry fetch with exponential backoff if needed
                    retryFetchWithBackoff()
                } else {
                    val errorMsg = response.error ?: "Upload failed"
                    _uploadStatus.value = OperationStatus.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleException(e, "GalleryViewModel.uploadGalleryImage")
                _uploadStatus.value = OperationStatus.Error(errorInfo.userFriendlyMessage)
            }
        }
    }

    /**
     * Delete gallery image with optimistic update and error handling
     */
    fun deleteGalleryImage(title: String) {
        viewModelScope.launch {
            _deleteStatus.value = OperationStatus.Loading
            
            // Optimistic update - remove from UI immediately
            val imageToDelete = _galleryImages.value.find { it.title == title }
            val updatedList = _galleryImages.value.filter { it.title != title }
            _galleryImages.value = updatedList
            
            try {
                val userEmail = sessionManager.userEmail.first()
                val request = GalleryDeleteRequest(
                    title = title,
                    userEmail = userEmail
                )
                
                val response = PerformanceLogger.measureNetworkOperation("gallery/delete", "POST") {
                    repository.deleteGalleryImage(request)
                }
                
                if (response.success) {
                    _deleteStatus.value = OperationStatus.Success("Image deleted successfully")
                    
                    // Delete from Firestore (non-blocking)
                    imageToDelete?.let {
                        launch {
                            deleteFromFirestore(it.url)
                        }
                    }
                    
                    // Invalidate cache and refresh
                    invalidateCache()
                    fetchGalleryImages(forceRefresh = true)
                } else {
                    // Revert optimistic update on failure
                    _galleryImages.value = _galleryImages.value + listOfNotNull(imageToDelete)
                    val errorMsg = response.error ?: "Delete failed"
                    _deleteStatus.value = OperationStatus.Error(errorMsg)
                }
            } catch (e: Exception) {
                // Revert optimistic update on failure
                _galleryImages.value = _galleryImages.value + listOfNotNull(imageToDelete)
                val errorInfo = ErrorHandler.handleException(e, "GalleryViewModel.deleteGalleryImage")
                _deleteStatus.value = OperationStatus.Error(errorInfo.userFriendlyMessage)
            }
        }
    }
    
    /**
     * Invalidate cache to force refresh on next fetch
     */
    private fun invalidateCache() {
        cachedImages = null
        cacheTimestamp = 0
    }
    
    /**
     * Retry fetch with exponential backoff
     */
    private suspend fun retryFetchWithBackoff(maxRetries: Int = 3) {
        var retryCount = 0
        var delayMs = 1000L
        
        while (retryCount < maxRetries) {
            delay(delayMs)
            try {
                fetchGalleryImages(forceRefresh = true)
                return // Success, exit retry loop
            } catch (e: Exception) {
                retryCount++
                delayMs *= 2 // Exponential backoff
            }
        }
    }

    // ✅ Sync uploaded image to Firestore for real-time updates
    private suspend fun syncToFirestore(imageUrl: String) {
        try {
            val data = hashMapOf(
                "imageUrl" to imageUrl,
                "uploadedAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("gallery").add(data).await()
        } catch (e: Exception) {
            // Non-critical, just log
            android.util.Log.e("GalleryViewModel", "Failed to sync to Firestore: ${e.message}")
        }
    }

    // ✅ Delete from Firestore (search by URL)
    private suspend fun deleteFromFirestore(imageUrl: String) {
        try {
            val snapshot = firestore.collection("gallery")
                .whereEqualTo("imageUrl", imageUrl)
                .get()
                .await()
            snapshot.documents.forEach { it.reference.delete().await() }
        } catch (e: Exception) {
            android.util.Log.e("GalleryViewModel", "Failed to delete from Firestore: ${e.message}")
        }
    }
}

