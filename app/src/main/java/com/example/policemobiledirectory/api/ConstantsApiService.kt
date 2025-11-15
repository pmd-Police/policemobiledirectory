package com.example.policemobiledirectory.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// ✅ Matches your Google Apps Script JSON response
data class RemoteConstantsResponse(
    val ranks: List<String>? = null,
    val bloodGroups: List<String>? = null,
    val districts: List<String>? = null,
    val stationsByDistrict: Map<String, List<String>>? = null,
    val lastUpdated: String? = null
)

// ✅ Retrofit API Interface
interface ConstantsApiService {

    // 🔹 1. For fetching constants (GET)
    @GET("exec")
    suspend fun getRemoteConstants(): RemoteConstantsResponse

    // 🔹 2. (Optional) Upload constants (only if needed again later)
    @POST("exec")
    suspend fun uploadConstants(@Body data: Map<String, Any>): Response<Unit>
}
