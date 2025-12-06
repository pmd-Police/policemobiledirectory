package com.example.policemobiledirectory.data.remote

import com.google.gson.annotations.SerializedName

data class SyncResponse(
    val success: Boolean? = null,
    val error: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("details") val details: String? = null
)

