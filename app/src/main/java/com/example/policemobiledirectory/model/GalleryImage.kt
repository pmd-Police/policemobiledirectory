package com.example.policemobiledirectory.model

import com.google.gson.annotations.SerializedName

data class GalleryImage(
    @SerializedName("Title")
    val title: String,

    @SerializedName("URL")
    val url: String,

    @SerializedName("Category")
    val category: String? = null,

    @SerializedName("Uploaded By")
    val uploadedBy: String? = null,

    @SerializedName("Uploaded Date")
    val uploadedDate: String? = null,

    @SerializedName("Description")
    val description: String? = null,

    @SerializedName("Delete")
    val delete: String? = null
)


