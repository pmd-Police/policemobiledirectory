package com.example.policemobiledirectory.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey
    val kgid: String = "",
    val name: String = "",
    val email: String = "",
    val pin: String? = null,
    val mobile1: String? = null,
    val mobile2: String? = null,
    val rank: String? = null,
    val metalNumber: String? = null,
    val district: String? = null,
    val station: String? = null,
    val bloodGroup: String? = null,
    val photoUrl: String? = null,
    val fcmToken: String? = null,
    val isAdmin: Boolean = false,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val firebaseUid: String? = null,
    val photoUrlFromGoogle: String? = null,
    val isApproved: Boolean = true
)
