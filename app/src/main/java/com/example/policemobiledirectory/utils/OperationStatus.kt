package com.example.policemobiledirectory.utils
sealed class OperationStatus<out T> {
    object Idle : OperationStatus<Nothing>()
    object Loading : OperationStatus<Nothing>()
    data class Success<out T>(val data: T) : OperationStatus<T>()
    data class Error(val message: String) : OperationStatus<Nothing>()
}
