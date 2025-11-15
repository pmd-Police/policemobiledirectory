package com.example.policemobiledirectory.model

import com.google.gson.annotations.SerializedName

data class Document(
    @SerializedName("Title")
    val title: String,

    @SerializedName("URL")
    val url: String,

    @SerializedName("Category")
    val category: String? = null,

    // ✅ Renamed field, mapped to “Uploaded By” in JSON
    @SerializedName("Uploaded By")
    val uploadedBy: String? = null,

    // ✅ Renamed field, mapped to “Uploaded Date” in JSON
    @SerializedName("Uploaded Date")
    val uploadedDate: String? = null,

    @SerializedName("Description")
    val description: String? = null,

    @SerializedName("Delete")
    val delete: String? = null
)
