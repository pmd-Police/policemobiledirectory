package com.example.policemobiledirectory.model

/**
 * --- GALLERY API REQUEST MODELS ---
 * Used for Upload and Delete operations with Retrofit.
 */

// 🟢 Upload New Gallery Image
data class GalleryUploadRequest(
    val title: String,
    val fileBase64: String,
    val mimeType: String,
    val category: String?,
    val description: String?,
    val userEmail: String? = null  // ✅ For Apps Script authentication
)

// 🔴 Delete Gallery Image
data class GalleryDeleteRequest(
    val title: String,
    val userEmail: String? = null  // ✅ For Apps Script authentication
)


