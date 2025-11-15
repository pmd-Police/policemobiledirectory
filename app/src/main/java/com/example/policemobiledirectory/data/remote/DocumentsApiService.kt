package com.example.policemobiledirectory.data.remote

import com.example.policemobiledirectory.model.*
import retrofit2.http.*

interface DocumentsApiService {

    @GET("exec?action=getDocuments")
    suspend fun getDocuments(): List<Document>

    @POST("exec?action=uploadDocument")
    suspend fun uploadDocument(@Body request: DocumentUploadRequest): ApiResponse

    @POST("exec?action=editDocument")
    suspend fun editDocument(@Body request: DocumentEditRequest): ApiResponse

    @POST("exec?action=deleteDocument")
    suspend fun deleteDocument(@Body request: DocumentDeleteRequest): ApiResponse
}
