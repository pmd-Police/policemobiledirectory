package com.example.policemobiledirectory.data.remote

import com.example.policemobiledirectory.model.*
import retrofit2.http.*

interface GalleryApiService {

    @GET("exec?action=getGallery")
    suspend fun getGalleryImagesRaw(
        @Query("token") token: String? = null
    ): retrofit2.Response<okhttp3.ResponseBody>

    @POST("exec?action=uploadGallery")
    suspend fun uploadGalleryImage(
        @Query("token") token: String? = null,
        @Body request: GalleryUploadRequest
    ): ApiResponse

    @POST("exec?action=deleteGallery")
    suspend fun deleteGalleryImage(
        @Query("token") token: String? = null,
        @Body request: GalleryDeleteRequest
    ): ApiResponse
}


