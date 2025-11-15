package com.example.policemobiledirectory.repository

import com.example.policemobiledirectory.data.remote.DocumentsApiService
import com.example.policemobiledirectory.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentsRepository @Inject constructor(
    private val api: DocumentsApiService
) {
    suspend fun fetchDocuments(): List<Document> = api.getDocuments()

    suspend fun uploadDocument(request: DocumentUploadRequest) =
        api.uploadDocument(request)

    suspend fun editDocument(request: DocumentEditRequest) =
        api.editDocument(request)

    suspend fun deleteDocument(request: DocumentDeleteRequest) =
        api.deleteDocument(request)
}
