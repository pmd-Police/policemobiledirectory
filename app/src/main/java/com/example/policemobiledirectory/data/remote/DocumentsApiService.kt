package com.example.policemobiledirectory.data.remote

import com.example.policemobiledirectory.model.*
import retrofit2.http.*

interface DocumentsApiService {

    @GET("exec?action=getDocuments")
    suspend fun getDocumentsRaw(
        @Query("token") token: String? = null
    ): retrofit2.Response<okhttp3.ResponseBody>

    @POST("exec?action=uploadDocument")
    suspend fun uploadDocument(
        @Query("token") token: String? = null,
        @Body request: DocumentUploadRequest
    ): ApiResponse

    @POST("exec?action=editDocument")
    suspend fun editDocument(
        @Query("token") token: String? = null,
        @Body request: DocumentEditRequest
    ): ApiResponse

    @POST("exec?action=deleteDocument")
    suspend fun deleteDocument(
        @Query("token") token: String? = null,
        @Body request: DocumentDeleteRequest
    ): ApiResponse
}
