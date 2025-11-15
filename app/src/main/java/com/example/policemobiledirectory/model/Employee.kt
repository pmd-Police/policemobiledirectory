package com.example.policemobiledirectory.model

import com.example.policemobiledirectory.utils.Constants
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Employee(
    val kgid: String = "",
    val name: String = "",
    val email: String = "",
    val pin: String? = null,
    val mobile1: String? = null,
    val mobile2: String? = null,
    val rank: String? = null,          // Rank replaces designation
    val metalNumber: String? = null,   // Optional metal number
    val district: String? = null,
    val station: String? = null,
    val bloodGroup: String? = null,
    val photoUrl: String? = null,
    val fcmToken: String? = null,

    @get:PropertyName("isAdmin")
    val isAdmin: Boolean = false,

    // ✅ New field — approval status (admin controlled)
    @get:PropertyName("isApproved")
    val isApproved: Boolean = true,

    val firebaseUid: String? = null,
    val photoUrlFromGoogle: String? = null,

    @ServerTimestamp
    val createdAt: Date? = null
) {
    // Computed property for display
    val displayRank: String
        get() {
            val currentRank = rank ?: ""
            return if (currentRank.isNotBlank()) {
                if (Constants.ranksRequiringMetalNumber.contains(currentRank) && !metalNumber.isNullOrBlank()) {
                    "$currentRank $metalNumber"
                } else {
                    currentRank
                }
            } else {
                ""
            }
        }

    /**
     * ✅ Case-insensitive search matching
     * Supports filters: name, kgid, rank, mobile, district, station, email
     */
    fun matches(query: String, filter: String): Boolean {
        val q = query.trim().lowercase()

        return when (filter.lowercase()) {
            "name" -> name.lowercase().contains(q)
            "kgid" -> kgid.lowercase().contains(q)
            "rank" -> (rank ?: "").lowercase().contains(q)
            "mobile" -> listOfNotNull(mobile1, mobile2).any { it.lowercase().contains(q) }
            "district" -> (district ?: "").lowercase().contains(q)
            "station" -> (station ?: "").lowercase().contains(q)
            "email" -> email.lowercase().contains(q)
            else -> listOfNotNull(
                name, kgid, rank, mobile1, mobile2, district, station, email
            ).any { it.lowercase().contains(q) }
        }
    }
}
