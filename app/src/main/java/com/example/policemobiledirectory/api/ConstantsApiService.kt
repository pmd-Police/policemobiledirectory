package com.example.policemobiledirectory.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Data class matching PRD format:
 * {
 *   "success": true,
 *   "data": {
 *     "ranks": [...],
 *     "districts": [...],
 *     "stationsbydistrict": { "Bagalkot": [...], ... },
 *     "bloodgroups": [...],
 *     "lastupdated": "2025-12-01T12:00:00Z"
 *   }
 * }
 */
data class ConstantsData(
    val ranks: List<String>,
    val districts: List<String>,
    val stationsbydistrict: Map<String, List<String>>,
    val bloodgroups: List<String>,
    val lastupdated: String,
    val version: Int // Version number from server - should match LOCAL_CONSTANTS_VERSION
)

data class ConstantsApiResponse(
    val success: Boolean,
    val data: ConstantsData?
)

// ✅ Retrofit API Interface
interface ConstantsApiService {

    // 🔹 Fetch constants (GET) - matches PRD format
    @GET("exec")
    suspend fun getConstants(
        @Query("token") token: String? = null
    ): ConstantsApiResponse
}
