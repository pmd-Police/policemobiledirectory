package com.example.policemobiledirectory.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.policemobiledirectory.model.*
import com.example.policemobiledirectory.repository.DocumentsRepository

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val repository: DocumentsRepository
) : ViewModel() {

    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> = _documents

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _deleteSuccess = MutableStateFlow<String?>(null)
    val deleteSuccess: StateFlow<String?> = _deleteSuccess

    private val _uploadSuccess = MutableStateFlow<String?>(null)
    val uploadSuccess: StateFlow<String?> = _uploadSuccess

    fun clearMessages() {
        _deleteSuccess.value = null
        _uploadSuccess.value = null
    }

    fun fetchDocuments() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val docs = repository.fetchDocuments()
                _documents.value = docs ?: emptyList()
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to fetch documents"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 🟢 Upload
    fun uploadDocument(
        title: String,
        fileBase64: String,
        mimeType: String,
        category: String?,
        description: String?
    ) {
        viewModelScope.launch {
            try {
                val request = DocumentUploadRequest(
                    title = title,
                    fileBase64 = fileBase64,
                    mimeType = mimeType,
                    category = category,
                    description = description
                )
                repository.uploadDocument(request)
                _uploadSuccess.value = "Document uploaded successfully"
                fetchDocuments()
            } catch (e: Exception) {
                _error.value = "Upload failed: ${e.message}"
            }
        }
    }

    // 🟡 Edit
    fun editDocument(
        oldTitle: String,
        newTitle: String?,
        category: String?,
        description: String?
    ) {
        viewModelScope.launch {
            try {
                val request = DocumentEditRequest(
                    oldTitle = oldTitle,
                    newTitle = newTitle,
                    category = category,
                    description = description
                )
                repository.editDocument(request)
                _uploadSuccess.value = "Document updated successfully"
                fetchDocuments()
            } catch (e: Exception) {
                _error.value = "Edit failed: ${e.message}"
            }
        }
    }

    // 🔴 Delete
    fun deleteDocument(title: String) {
        viewModelScope.launch {
            try {
                val request = DocumentDeleteRequest(title = title)
                repository.deleteDocument(request)
                _deleteSuccess.value = "Document deleted successfully"
                fetchDocuments()
            } catch (e: Exception) {
                _error.value = "Delete failed: ${e.message}"
            }
        }
    }
}
