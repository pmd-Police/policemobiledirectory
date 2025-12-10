package com.example.policemobiledirectory.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface OfficersSyncApiService {
    @GET("exec")
    suspend fun syncOfficersSheetToFirebase(
        @Query("action") action: String = "syncOfficersSheetToFirebase",
        @Query("token") token: String? = null
    ): ResponseBody
}


