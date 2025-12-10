package com.example.policemobiledirectory.api

import com.example.policemobiledirectory.data.local.EmployeeEntity
import retrofit2.http.*

interface EmployeeApiService {

    // === EMPLOYEE ENDPOINTS ===
    @GET("exec?action=getEmployees")
    suspend fun getEmployees(
        @Query("token") token: String? = null
    ): List<EmployeeEntity>

    @POST("exec?action=addEmployee")
    suspend fun addEmployee(
        @Query("token") token: String? = null,
        @Body employee: EmployeeEntity
    )

    @POST("exec?action=updateEmployee")
    suspend fun updateEmployee(
        @Query("token") token: String? = null,
        @Body employee: EmployeeEntity
    )

    @POST("exec?action=deleteEmployee")
    suspend fun deleteEmployee(
        @Query("token") token: String? = null,
        @Query("kgid") kgid: String
    )

    // === Drive cleanup ===
    @GET("macros/s/YOUR_SCRIPT_ID/exec?action=deleteImage")
    suspend fun deleteImageFromDrive(
        @Query("token") token: String? = null,
        @Query("fileId") fileId: String
    ): retrofit2.Response<com.example.policemobiledirectory.model.ApiResponse>
}
