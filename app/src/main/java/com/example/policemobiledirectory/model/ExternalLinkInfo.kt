package com.example.policemobiledirectory.model

data class ExternalLinkInfo(
    val name: String = "",
    val playStoreUrl: String = "",
    val apkUrl: String? = null,        // ✅ newly added for Firebase APK link
    val iconUrl: String? = null,       // ✅ optional app logo URL
    val category: String = "General",
    val isActive: Boolean = true,
    val documentId: String? = null    // ✅ Firestore document ID for deletion
)
