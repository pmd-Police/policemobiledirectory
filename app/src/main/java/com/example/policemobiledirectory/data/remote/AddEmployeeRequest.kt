package com.example.policemobiledirectory.data.remote

data class UpdateEmployeeRequest(
    val name: String,
    val mobile1: String,
    val mobile2: String? = null,
    val rank: String,
    val station: String,
    val photoUrl: String? = null
)
