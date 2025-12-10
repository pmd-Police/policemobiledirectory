package com.example.policemobiledirectory.repository

import com.example.policemobiledirectory.data.remote.GalleryApiService
import com.example.policemobiledirectory.model.*
import com.example.policemobiledirectory.utils.SecurityConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class GalleryRepository @Inject constructor(
    private val api: GalleryApiService,
    private val securityConfig: SecurityConfig
) {
    private fun token() = securityConfig.getSecretToken()
    private val gson = Gson()

    suspend fun fetchGalleryImages(): List<GalleryImage> {
        val response: Response<ResponseBody> = api.getGalleryImagesRaw(token = token())
        val bodyStr = response.body()?.string()
            ?: throw IllegalStateException("Empty gallery response")

        // Try parse as array
        try {
            val listType = object : TypeToken<List<GalleryImage>>() {}.type
            return gson.fromJson(bodyStr, listType)
        } catch (_: Exception) {
            // try object with data/error
        }

        data class GalleryApiResponse(
            val success: Boolean? = null,
            val data: List<GalleryImage>? = null,
            val error: String? = null,
            val message: String? = null
        )

        val obj = try {
            gson.fromJson(bodyStr, GalleryApiResponse::class.java)
        } catch (e: Exception) {
            throw IllegalStateException("Unable to parse gallery response: ${e.message}")
        }

        obj.data?.let { return it }

        val err = obj.error ?: obj.message ?: "Gallery load failed"
        throw IllegalStateException(err)
    }

    suspend fun uploadGalleryImage(request: GalleryUploadRequest) =
        api.uploadGalleryImage(token = token(), request = request)

    suspend fun deleteGalleryImage(request: GalleryDeleteRequest) =
        api.deleteGalleryImage(token = token(), request = request)
}


