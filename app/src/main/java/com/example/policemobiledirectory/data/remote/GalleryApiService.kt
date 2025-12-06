package com.example.policemobiledirectory.data.remote

import com.example.policemobiledirectory.model.*
import retrofit2.http.*

interface GalleryApiService {

    @GET("exec?action=getGallery")
    suspend fun getGalleryImages(): List<GalleryImage>

    @POST("exec?action=uploadGallery")
    suspend fun uploadGalleryImage(@Body request: GalleryUploadRequest): ApiResponse

    @POST("exec?action=deleteGallery")
    suspend fun deleteGalleryImage(@Body request: GalleryDeleteRequest): ApiResponse
}


