package com.example.policemobiledirectory.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.policemobiledirectory.data.local.SessionManager
import com.example.policemobiledirectory.model.*
import com.example.policemobiledirectory.repository.DocumentsRepository
import com.example.policemobiledirectory.utils.ErrorHandler
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.utils.PerformanceLogger
import kotlinx.coroutines.flow.first

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val repository: DocumentsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // State management with OperationStatus
    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> = _documents.asStateFlow()

    private val _documentsStatus = MutableStateFlow<OperationStatus<List<Document>>>(OperationStatus.Idle)
    val documentsStatus: StateFlow<OperationStatus<List<Document>>> = _documentsStatus.asStateFlow()

    private val _uploadStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val uploadStatus: StateFlow<OperationStatus<String>> = _uploadStatus.asStateFlow()

    private val _deleteStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val deleteStatus: StateFlow<OperationStatus<String>> = _deleteStatus.asStateFlow()

    private val _editStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val editStatus: StateFlow<OperationStatus<String>> = _editStatus.asStateFlow()

    // In-memory cache with timestamp
    private var cachedDocuments: List<Document>? = null
    private var cacheTimestamp: Long = 0
    private val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes

    // Computed properties for convenience
    val isLoading: Boolean get() = _documentsStatus.value is OperationStatus.Loading
    val error: String? get() = (_documentsStatus.value as? OperationStatus.Error)?.message

    fun clearStatus() {
        _uploadStatus.value = OperationStatus.Idle
        _deleteStatus.value = OperationStatus.Idle
        _editStatus.value = OperationStatus.Idle
    }

    /**
     * Fetch documents with caching, error handling, and performance tracking
     */
    fun fetchDocuments(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Return cached data if available and not expired
            if (!forceRefresh && cachedDocuments != null && 
                (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION_MS) {
                _documents.value = cachedDocuments!!
                _documentsStatus.value = OperationStatus.Success(cachedDocuments!!)
                return@launch
            }

            _documentsStatus.value = OperationStatus.Loading
            
            try {
                val docs = PerformanceLogger.measureNetworkOperation("documents", "GET") {
                    repository.fetchDocuments()
                }
                
                val docList = docs ?: emptyList()
                
                // Update cache
                cachedDocuments = docList
                cacheTimestamp = System.currentTimeMillis()
                
                _documents.value = docList
                _documentsStatus.value = OperationStatus.Success(docList)
                
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleException(e, "DocumentsViewModel.fetchDocuments")
                
                // Return cached data if available, even if expired
                if (cachedDocuments != null) {
                    _documents.value = cachedDocuments!!
                    _documentsStatus.value = OperationStatus.Error(
                        "Using cached data. ${errorInfo.userFriendlyMessage}"
                    )
                } else {
                    _documentsStatus.value = OperationStatus.Error(errorInfo.userFriendlyMessage)
                }
                
                // Retry if error is retryable
                if (errorInfo.shouldRetry) {
                    delay(errorInfo.retryDelay)
                    fetchDocuments(forceRefresh = true)
                }
            }
        }
    }

    /**
     * Upload document with error handling and performance tracking
     */
    fun uploadDocument(
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
                val request = DocumentUploadRequest(
                    title = title,
                    fileBase64 = fileBase64,
                    mimeType = mimeType,
                    category = category,
                    description = description,
                    userEmail = userEmail
                )
                
                val response = PerformanceLogger.measureNetworkOperation("documents/upload", "POST") {
                    repository.uploadDocument(request)
                }
                
                if (response.success) {
                    _uploadStatus.value = OperationStatus.Success("Document uploaded successfully")
                    
                    // Invalidate cache and refresh
                    invalidateCache()
                    retryFetchWithBackoff()
                } else {
                    val errorMsg = response.error ?: "Upload failed"
                    _uploadStatus.value = OperationStatus.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleException(e, "DocumentsViewModel.uploadDocument")
                _uploadStatus.value = OperationStatus.Error(errorInfo.userFriendlyMessage)
            }
        }
    }

    /**
     * Edit document with optimistic update and error handling
     */
    fun editDocument(
        oldTitle: String,
        newTitle: String?,
        category: String?,
        description: String?
    ) {
        viewModelScope.launch {
            _editStatus.value = OperationStatus.Loading
            
            // Optimistic update
            val documentToEdit = _documents.value.find { it.title == oldTitle }
            val updatedList = if (documentToEdit != null && newTitle != null) {
                _documents.value.map { doc ->
                    if (doc.title == oldTitle) {
                        doc.copy(title = newTitle, category = category, description = description)
                    } else doc
                }
            } else {
                _documents.value
            }
            _documents.value = updatedList
            
            try {
                val userEmail = sessionManager.userEmail.first()
                val request = DocumentEditRequest(
                    oldTitle = oldTitle,
                    newTitle = newTitle,
                    category = category,
                    description = description,
                    userEmail = userEmail
                )
                
                PerformanceLogger.measureNetworkOperation("documents/edit", "POST") {
                    repository.editDocument(request)
                }
                
                _editStatus.value = OperationStatus.Success("Document updated successfully")
                
                // Invalidate cache and refresh
                invalidateCache()
                fetchDocuments(forceRefresh = true)
            } catch (e: Exception) {
                // Revert optimistic update on failure
                _documents.value = _documents.value.map { doc ->
                    if (doc.title == newTitle && documentToEdit != null) {
                        documentToEdit
                    } else doc
                }
                val errorInfo = ErrorHandler.handleException(e, "DocumentsViewModel.editDocument")
                _editStatus.value = OperationStatus.Error(errorInfo.userFriendlyMessage)
            }
        }
    }

    /**
     * Delete document with optimistic update and error handling
     */
    fun deleteDocument(title: String) {
        viewModelScope.launch {
            _deleteStatus.value = OperationStatus.Loading
            
            // Optimistic update - remove from UI immediately
            val documentToDelete = _documents.value.find { it.title == title }
            val updatedList = _documents.value.filter { it.title != title }
            _documents.value = updatedList
            
            try {
                val userEmail = sessionManager.userEmail.first()
                val request = DocumentDeleteRequest(
                    title = title,
                    userEmail = userEmail
                )
                
                PerformanceLogger.measureNetworkOperation("documents/delete", "POST") {
                    repository.deleteDocument(request)
                }
                
                _deleteStatus.value = OperationStatus.Success("Document deleted successfully")
                
                // Invalidate cache and refresh
                invalidateCache()
                fetchDocuments(forceRefresh = true)
            } catch (e: Exception) {
                // Revert optimistic update on failure
                _documents.value = _documents.value + listOfNotNull(documentToDelete)
                val errorInfo = ErrorHandler.handleException(e, "DocumentsViewModel.deleteDocument")
                _deleteStatus.value = OperationStatus.Error(errorInfo.userFriendlyMessage)
            }
        }
    }
    
    /**
     * Invalidate cache to force refresh on next fetch
     */
    private fun invalidateCache() {
        cachedDocuments = null
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
                fetchDocuments(forceRefresh = true)
                return // Success, exit retry loop
            } catch (e: Exception) {
                retryCount++
                delayMs *= 2 // Exponential backoff
            }
        }
    }
}
