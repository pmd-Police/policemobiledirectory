package com.example.policemobiledirectory.data.remote

data class EmployeeAddResponse(
    val status: String,
    val message: String,
    val employee: EmployeeDto? // full employee object
)

data class EmployeeDto(
    val kgid: String,
    val name: String,
    val mobile1: String,         // match Employee model
    val mobile2: String?,        // match Employee model
    val rank: String,
    val station: String,
    val photoUrl: String?
)
