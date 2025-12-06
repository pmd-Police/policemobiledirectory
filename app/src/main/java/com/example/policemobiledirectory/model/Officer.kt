package com.example.policemobiledirectory.model

/**
 * Officer - Read-only contact information for police officers
 * Admin-managed, no authentication required
 * Separate from Employee collection
 */
data class Officer(
    val agid: String = "",           // Auto-generated ID (from Apps Script)
    val name: String = "",
    val email: String? = null,
    val mobile: String? = null,
    val landline: String? = null,
    val rank: String? = null,
    val station: String? = null,
    val district: String? = null,
    val photoUrl: String? = null
) {
    val primaryPhone: String?
        get() = mobile ?: landline

    val secondaryPhone: String?
        get() = if (!mobile.isNullOrBlank() && !landline.isNullOrBlank()) landline else null

    /**
     * Optimized matching function (query is already lowercase)
     * Supports filters: name, agid, rank, mobile, district, station, email
     */
    fun matchesOptimized(queryLower: String, filter: String): Boolean {
        return when (filter.lowercase()) {
            "name" -> {
                val nameLower = name.lowercase()
                nameLower.startsWith(queryLower) || nameLower.contains(queryLower)
            }
            "agid" -> {
                val agidLower = agid.lowercase()
                agidLower.startsWith(queryLower) || agidLower.contains(queryLower)
            }
            "rank" -> (rank ?: "").lowercase().contains(queryLower)
            "mobile" -> listOfNotNull(mobile, landline).any { it.lowercase().contains(queryLower) }
            "district" -> (district ?: "").lowercase().contains(queryLower)
            "station" -> (station ?: "").lowercase().contains(queryLower)
            "email" -> (email ?: "").lowercase().contains(queryLower)
            else -> listOfNotNull(
                name, agid, rank, mobile, landline, district, station, email
            ).any { it.lowercase().contains(queryLower) }
        }
    }
    
    /**
     * Legacy function for backward compatibility
     */
    fun matches(query: String, filter: String): Boolean {
        return matchesOptimized(query.trim().lowercase(), filter)
    }
}

