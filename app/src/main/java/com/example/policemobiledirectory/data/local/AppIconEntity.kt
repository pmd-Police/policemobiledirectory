package com.example.policemobiledirectory.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_icons")
data class AppIconEntity(
    @PrimaryKey val packageName: String, // extracted from Play Store URL
    val iconUrl: String?,
    val lastUpdated: Long
)
