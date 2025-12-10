package com.example.policemobiledirectory.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface SyncApiService {

    @GET("exec")
    suspend fun syncFirebaseToSheet(
        @Query("action") action: String = "syncFirebaseToSheet",
        @Query("token") token: String? = null
    ): ResponseBody

    @GET("exec")
    suspend fun syncSheetToFirebase(
        @Query("action") action: String = "syncSheetToFirebase",
        @Query("token") token: String? = null
    ): ResponseBody
    
    // Alternative action name support
    @GET("exec")
    suspend fun syncSheetToFirebaseLatest(
        @Query("action") action: String = "syncSheetToFirebaseLatest",
        @Query("token") token: String? = null
    ): ResponseBody
}

