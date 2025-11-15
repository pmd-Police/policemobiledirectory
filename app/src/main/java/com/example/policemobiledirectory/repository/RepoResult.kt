package com.example.policemobiledirectory.repository

sealed class RepoResult<out T> {
    data class Success<out T>(val data: T? = null) : RepoResult<T>()
    data class Error(val exception: Throwable? = null, val message: String? = null) : RepoResult<Nothing>()
    object Loading : RepoResult<Nothing>()
}