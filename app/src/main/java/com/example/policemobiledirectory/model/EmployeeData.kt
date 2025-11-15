package com.example.policemobiledirectory.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val kgid: String,
    val name: String,
    val email: String = "", // Added email field

    val mobile: String,
    val rank: String = "", // Added rank field
    val metalNumber: String? = null, // Added metalNumber field, nullable
    val station: String = "",
    val district: String = "", // Added district field
    val bloodGroup: String = "", // Added bloodGroup field
    val photoUrl: String? = null, // Added photoUrl field
    val fcmToken: String? = null, // Added fcmToken field
    val isAdmin: Boolean = false, // Added isAdmin field
    val firebaseUid: String = "", // Added firebaseUid field
    val photoUrlFromGoogle: String? = null, // Added photoUrlFromGoogle field
    val photoBase64: String = "" // Stores Base64-encoded photo - kept for now if used elsewhere
)
