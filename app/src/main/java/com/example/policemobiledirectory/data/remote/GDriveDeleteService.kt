package com.example.policemobiledirectory.data.remote

import retrofit2.Response
import retrofit2.http.*

/**
 * 🚨 GDriveDeleteService.kt
 *
 * Handles deleting uploaded officer images from Google Drive
 * via Apps Script (using fileId or userId)
 *
 * ➕ Endpoint:
 * https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec?action=deleteImage&fileId=XYZ
 */
interface GDriveDeleteService {

    @FormUrlEncoded
    @POST("exec?action=deleteImage")
    suspend fun deleteByFileId(
        @Field("fileId") fileId: String
    ): Response<GDriveDeleteResponse>

    @FormUrlEncoded
    @POST("exec?action=deleteImage")
    suspend fun deleteByUserId(
        @Field("userId") userId: String
    ): Response<GDriveDeleteResponse>
}

/**
 * ✅ Response from Apps Script
 */
data class GDriveDeleteResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)
