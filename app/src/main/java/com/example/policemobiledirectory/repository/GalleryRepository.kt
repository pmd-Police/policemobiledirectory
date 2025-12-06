package com.example.policemobiledirectory.repository

import com.example.policemobiledirectory.data.remote.GalleryApiService
import com.example.policemobiledirectory.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepository @Inject constructor(
    private val api: GalleryApiService
) {
    suspend fun fetchGalleryImages(): List<GalleryImage> = api.getGalleryImages()

    suspend fun uploadGalleryImage(request: GalleryUploadRequest) =
        api.uploadGalleryImage(request)

    suspend fun deleteGalleryImage(request: GalleryDeleteRequest) =
        api.deleteGalleryImage(request)
}


