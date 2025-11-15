package com.example.policemobiledirectory.data.local

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = EmployeeEntity::class)
@Entity(tableName = "employees_fts")
data class EmployeeFts(
    val name: String,
    val rank: String,
    val station: String,
    val district: String,
    val email: String
)
