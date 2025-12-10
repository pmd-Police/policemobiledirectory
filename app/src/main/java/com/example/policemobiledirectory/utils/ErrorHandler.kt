package com.example.policemobiledirectory.utils

import android.util.Log
import com.example.policemobiledirectory.R
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Centralized error handling utility
 * Provides consistent error handling, user-friendly messages, and error categorization
 */
object ErrorHandler {
    
    private const val TAG = "ErrorHandler"
    
    /**
     * Error categories for better error handling
     */
    enum class ErrorCategory {
        NETWORK,
        AUTHENTICATION,
        PERMISSION,
        VALIDATION,
        SERVER,
        UNKNOWN
    }
    
    /**
     * Error data class containing error information
     */
    data class ErrorInfo(
        val message: String,
        val category: ErrorCategory,
        val originalException: Throwable? = null,
        val userFriendlyMessage: String = message,
        val shouldRetry: Boolean = false,
        val retryDelay: Long = 0L
    )
    
    /**
     * Handle exception and return user-friendly error message
     */
    fun handleException(exception: Throwable, context: String = ""): ErrorInfo {
        val errorInfo = when (exception) {
            is UnknownHostException -> ErrorInfo(
                message = "Network error: Unable to reach server",
                category = ErrorCategory.NETWORK,
                originalException = exception,
                userFriendlyMessage = "No internet connection. Please check your network and try again.",
                shouldRetry = true,
                retryDelay = 2000L
            )
            
            is SocketTimeoutException -> ErrorInfo(
                message = "Network timeout: Request took too long",
                category = ErrorCategory.NETWORK,
                originalException = exception,
                userFriendlyMessage = "Request timed out. Please try again.",
                shouldRetry = true,
                retryDelay = 3000L
            )
            
            is IOException -> ErrorInfo(
                message = "IO error: ${exception.message}",
                category = ErrorCategory.NETWORK,
                originalException = exception,
                userFriendlyMessage = "Network error. Please check your connection and try again.",
                shouldRetry = true,
                retryDelay = 2000L
            )
            
            is SSLException -> ErrorInfo(
                message = "SSL error: ${exception.message}",
                category = ErrorCategory.NETWORK,
                originalException = exception,
                userFriendlyMessage = "Secure connection failed. Please try again.",
                shouldRetry = true,
                retryDelay = 2000L
            )
            
            is SecurityException -> ErrorInfo(
                message = "Permission denied: ${exception.message}",
                category = ErrorCategory.PERMISSION,
                originalException = exception,
                userFriendlyMessage = "Permission denied. Please check app permissions.",
                shouldRetry = false
            )
            
            is IllegalArgumentException -> ErrorInfo(
                message = "Invalid input: ${exception.message}",
                category = ErrorCategory.VALIDATION,
                originalException = exception,
                userFriendlyMessage = "Invalid input. Please check your data and try again.",
                shouldRetry = false
            )
            
            is IllegalStateException -> ErrorInfo(
                message = "Invalid state: ${exception.message}",
                category = ErrorCategory.VALIDATION,
                originalException = exception,
                userFriendlyMessage = "Operation cannot be performed in current state.",
                shouldRetry = false
            )
            
            else -> ErrorInfo(
                message = exception.message ?: "Unknown error occurred",
                category = ErrorCategory.UNKNOWN,
                originalException = exception,
                userFriendlyMessage = "An unexpected error occurred. Please try again.",
                shouldRetry = false
            )
        }
        
        // Log error with context
        logError(errorInfo, context)
        
        return errorInfo
    }
    
    /**
     * Handle HTTP error codes
     */
    fun handleHttpError(code: Int, message: String? = null): ErrorInfo {
        val errorInfo = when (code) {
            400 -> ErrorInfo(
                message = "Bad Request: $message",
                category = ErrorCategory.VALIDATION,
                userFriendlyMessage = "Invalid request. Please check your input.",
                shouldRetry = false
            )
            
            401 -> ErrorInfo(
                message = "Unauthorized: $message",
                category = ErrorCategory.AUTHENTICATION,
                userFriendlyMessage = "Authentication failed. Please login again.",
                shouldRetry = false
            )
            
            403 -> ErrorInfo(
                message = "Forbidden: $message",
                category = ErrorCategory.PERMISSION,
                userFriendlyMessage = "You don't have permission to perform this action.",
                shouldRetry = false
            )
            
            404 -> ErrorInfo(
                message = "Not Found: $message",
                category = ErrorCategory.SERVER,
                userFriendlyMessage = "Resource not found. Please check and try again.",
                shouldRetry = false
            )
            
            408 -> ErrorInfo(
                message = "Request Timeout: $message",
                category = ErrorCategory.NETWORK,
                userFriendlyMessage = "Request timed out. Please try again.",
                shouldRetry = true,
                retryDelay = 3000L
            )
            
            429 -> ErrorInfo(
                message = "Too Many Requests: $message",
                category = ErrorCategory.SERVER,
                userFriendlyMessage = "Too many requests. Please wait a moment and try again.",
                shouldRetry = true,
                retryDelay = 5000L
            )
            
            500 -> ErrorInfo(
                message = "Internal Server Error: $message",
                category = ErrorCategory.SERVER,
                userFriendlyMessage = "Server error. Please try again later.",
                shouldRetry = true,
                retryDelay = 5000L
            )
            
            502, 503 -> ErrorInfo(
                message = "Service Unavailable: $message",
                category = ErrorCategory.SERVER,
                userFriendlyMessage = "Service temporarily unavailable. Please try again later.",
                shouldRetry = true,
                retryDelay = 10000L
            )
            
            else -> ErrorInfo(
                message = "HTTP Error $code: $message",
                category = ErrorCategory.SERVER,
                userFriendlyMessage = "An error occurred. Please try again.",
                shouldRetry = code >= 500, // Retry on server errors
                retryDelay = if (code >= 500) 5000L else 0L
            )
        }
        
        logError(errorInfo, "HTTP $code")
        return errorInfo
    }
    
    /**
     * Handle repository result errors
     */
    fun handleRepoError(errorMessage: String?, exception: Throwable? = null): ErrorInfo {
        return if (exception != null) {
            handleException(exception, "Repository")
        } else {
            ErrorInfo(
                message = errorMessage ?: "Unknown repository error",
                category = ErrorCategory.UNKNOWN,
                userFriendlyMessage = errorMessage ?: "An error occurred. Please try again.",
                shouldRetry = false
            )
        }
    }
    
    /**
     * Log error with appropriate level
     */
    private fun logError(errorInfo: ErrorInfo, context: String) {
        val logMessage = buildString {
            if (context.isNotBlank()) {
                append("[$context] ")
            }
            append(errorInfo.message)
            errorInfo.originalException?.let {
                append(" | Exception: ${it.javaClass.simpleName}")
            }
        }
        
        when (errorInfo.category) {
            ErrorCategory.NETWORK -> Log.w(TAG, logMessage, errorInfo.originalException)
            ErrorCategory.AUTHENTICATION -> Log.e(TAG, logMessage, errorInfo.originalException)
            ErrorCategory.PERMISSION -> Log.w(TAG, logMessage, errorInfo.originalException)
            ErrorCategory.VALIDATION -> Log.d(TAG, logMessage, errorInfo.originalException)
            ErrorCategory.SERVER -> Log.e(TAG, logMessage, errorInfo.originalException)
            ErrorCategory.UNKNOWN -> Log.e(TAG, logMessage, errorInfo.originalException)
        }
    }
    
    /**
     * Check if error is retryable
     */
    fun isRetryable(errorInfo: ErrorInfo): Boolean {
        return errorInfo.shouldRetry
    }
    
    /**
     * Get retry delay for error
     */
    fun getRetryDelay(errorInfo: ErrorInfo): Long {
        return errorInfo.retryDelay
    }
}



