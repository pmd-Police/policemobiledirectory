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
    val mobile1: String? = null,   // STRING after migration
    val mobile2: String? = null,
    val rank: String? = null,
    @get:PropertyName("metalNumber")
    @set:PropertyName("metalNumber")
    var metalNumber: String? = null,   // Firestore field: "metalNumber" (Google Sheet column: "metal")
    val district: String? = null,
    val station: String? = null,
    val bloodGroup: String? = null,
    val photoUrl: String? = null,
    val photoUrlFromGoogle: String? = null,
    val fcmToken: String? = null,
    val firebaseUid: String? = null,
    @get:PropertyName("isAdmin")
    val isAdmin: Boolean = false,      // BOOLEAN after migration
    @get:PropertyName("isApproved")
    val isApproved: Boolean = true,    // BOOLEAN after migration
    @ServerTimestamp
    val createdAt: Date? = null,       // TIMESTAMP after migration
    @ServerTimestamp
    val updatedAt: Date? = null        // TIMESTAMP after migration
) {
    // Computed property for display
    val displayRank: String
        get() {
            val currentRank = rank ?: ""
            return if (currentRank.isNotBlank()) {
                // Always show metal number with rank if metal number exists
                if (!metalNumber.isNullOrBlank()) {
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
