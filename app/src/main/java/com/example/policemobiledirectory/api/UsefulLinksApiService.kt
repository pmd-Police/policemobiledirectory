package com.example.policemobiledirectory.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Useful Links API Service
 * Handles communication with Apps Script for useful links management
 */
interface UsefulLinksApiService {

    /**
     * Get all useful links
     * @param token Secret token for authentication
     */
    @GET("exec?action=getLinks")
    suspend fun getLinks(
        @Query("token") token: String? = null
    ): Response<UsefulLinksResponse>

    /**
     * Add a new useful link
     * @param token Secret token for authentication
     */
    @POST("exec?action=addLink")
    suspend fun addLink(
        @Query("token") token: String? = null,
        @Body request: AddLinkRequest
    ): Response<UsefulLinksResponse>

    /**
     * Delete a useful link
     * @param token Secret token for authentication
     * @param linkId ID of the link to delete
     */
    @POST("exec?action=deleteLink")
    suspend fun deleteLink(
        @Query("token") token: String? = null,
        @Query("linkId") linkId: String
    ): Response<UsefulLinksResponse>
}

/**
 * Response from useful links API
 */
data class UsefulLinksResponse(
    val success: Boolean,
    val data: List<UsefulLinkData>? = null,
    val error: String? = null
)

/**
 * Useful link data structure
 */
data class UsefulLinkData(
    val id: String? = null,
    val name: String,
    val playStoreUrl: String? = null,
    val apkUrl: String? = null,
    val iconUrl: String? = null,
    val category: String? = null
)

/**
 * Request to add a new link
 */
data class AddLinkRequest(
    val name: String,
    val playStoreUrl: String? = null,
    val apkUrl: String? = null,
    val iconUrl: String? = null,
    val category: String? = null
)

