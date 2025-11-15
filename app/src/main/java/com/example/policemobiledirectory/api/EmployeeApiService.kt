package com.example.policemobiledirectory.api

import com.example.policemobiledirectory.data.local.EmployeeEntity
import com.example.policemobiledirectory.model.*
import retrofit2.http.*
import com.example.policemobiledirectory.model.ApiResponse

interface EmployeeApiService {

    // === EMPLOYEE ENDPOINTS ===
    @GET("exec?action=getEmployees")
    suspend fun getEmployees(): List<EmployeeEntity>

    @POST("exec?action=addEmployee")
    suspend fun addEmployee(@Body employee: EmployeeEntity)

    @POST("exec?action=updateEmployee")
    suspend fun updateEmployee(@Body employee: EmployeeEntity)

    @POST("exec?action=deleteEmployee")
    suspend fun deleteEmployee(@Query("kgid") kgid: String)


    // === DOCUMENT ENDPOINTS (Google Apps Script Integration) ===

    /** ✅ Fetch all documents from Google Sheet */
    @GET("exec?action=getDocuments")
    suspend fun getDocuments(): List<Document>

    /** ✅ Upload a new document (Base64 + metadata) */
    @POST("exec")
    suspend fun uploadDocument(@Body request: DocumentUploadRequest): ApiResponse

    /** ✅ Edit an existing document (rename/update metadata) */
    @POST("exec")
    suspend fun editDocument(@Body request: DocumentEditRequest): ApiResponse

    /** ✅ Delete a document (soft delete from Sheet + Drive) */
    @POST("exec")
    suspend fun deleteDocument(@Body request: DocumentDeleteRequest): ApiResponse


    // === (Optional) Drive cleanup ===
    @GET("macros/s/YOUR_SCRIPT_ID/exec?action=deleteImage")
    suspend fun deleteImageFromDrive(@Query("fileId") fileId: String): retrofit2.Response<ApiResponse>
}


/**
 * ✅ Unified API Response Model
 * Matches typical Apps Script JSON output:
 * e.g. { "success": true, "action": "upload", "error": null, "url": "..." }
 */
data class ApiResponse(
    val success: Boolean,
    val action: String? = null,
    val uploader: String? = null,
    val deletedBy: String? = null,
    val error: String? = null,
    val url: String? = null
)
