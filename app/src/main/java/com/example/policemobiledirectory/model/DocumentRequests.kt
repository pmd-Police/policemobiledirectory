package com.example.policemobiledirectory.model

/**
 * --- DOCUMENT API REQUEST MODELS ---
 * Used for Upload, Edit, and Delete operations with Retrofit.
 */

// 🟢 Upload New Document
data class DocumentUploadRequest(
    val title: String,
    val fileBase64: String,
    val mimeType: String,
    val category: String?,
    val description: String?,
    val userEmail: String? = null  // ✅ For Apps Script authentication
)

// 🟡 Edit Existing Document (optional fields)
data class DocumentEditRequest(
    val oldTitle: String,
    val newTitle: String?,
    val category: String?,
    val description: String?,
    val userEmail: String? = null  // ✅ For Apps Script authentication
)

// 🔴 Delete Document
data class DocumentDeleteRequest(
    val title: String,
    val userEmail: String? = null  // ✅ For Apps Script authentication
)
