package com.example.policemobiledirectory.model

/**
 * Unified API response used by the Google Apps Script endpoints.
 * Matches the JSON returned by the script, e.g.:
 * { "success": true, "action": "upload", "uploader": "x@y.com", "url": "https://..." }
 */
data class ApiResponse(
    val success: Boolean = false,
    val action: String? = null,
    val uploader: String? = null,
    val deletedBy: String? = null,
    val error: String? = null,
    val url: String? = null
)

