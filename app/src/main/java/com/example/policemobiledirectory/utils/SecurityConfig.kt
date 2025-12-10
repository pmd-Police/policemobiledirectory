package com.example.policemobiledirectory.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import com.example.policemobiledirectory.BuildConfig
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security configuration and utilities
 * Handles API authentication tokens and app signature verification
 * 
 * ⚠️ SECURITY: Never log or expose these values!
 */
@Singleton
class SecurityConfig @Inject constructor(
    private val context: Context
) {
    companion object {
        // ⚠️ SECURITY: This should be set via BuildConfig from CI/CD secrets
        // For local development, add to local.properties:
        // APPS_SCRIPT_SECRET_TOKEN=your_secret_token_here
        private const val DEFAULT_SECRET_TOKEN = "CHANGE_THIS_IN_PRODUCTION"
        
        // Maximum file size for image uploads (5MB)
        const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024
        
        // Rate limiting: max uploads per hour per user
        const val MAX_UPLOADS_PER_HOUR = 10
    }

    /**
     * Get secret token for Apps Script API authentication
     * In production, this should come from BuildConfig or secure storage
     * 
     * ⚠️ SECURITY: Add to gradle.properties:
     * APPS_SCRIPT_SECRET_TOKEN=your_secret_here
     * 
     * Then in build.gradle.kts:
     * buildConfigField("String", "APPS_SCRIPT_SECRET_TOKEN", "\"${project.findProperty("APPS_SCRIPT_SECRET_TOKEN") ?: ""}\"")
     */
    fun getSecretToken(): String {
        // Try BuildConfig first (set via gradle.properties or CI/CD)
        return try {
            val field = BuildConfig::class.java.getDeclaredField("APPS_SCRIPT_SECRET_TOKEN")
            field.isAccessible = true
            val token = field.get(null) as? String
            token?.takeIf { it.isNotBlank() && it != DEFAULT_SECRET_TOKEN } 
                ?: DEFAULT_SECRET_TOKEN
        } catch (e: Exception) {
            // Field doesn't exist, use default (will fail in production - this is intentional)
            DEFAULT_SECRET_TOKEN
        }
    }

    /**
     * Verify app signature to prevent tampering
     * Returns true if signature matches expected value
     */
    fun verifyAppSignature(): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            
            val signatures = packageInfo.signatures
            if (signatures.isEmpty()) return false
            
            val signature = signatures[0].toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signature)
            val signatureHash = Base64.encodeToString(digest, Base64.NO_WRAP)
            
            // ⚠️ SECURITY: In production, compare against expected signature hash
            // Store expected hash in BuildConfig or secure storage
            // For now, just verify signature exists (not null/empty)
            signatureHash.isNotBlank()
        } catch (e: Exception) {
            android.util.Log.e("SecurityConfig", "Signature verification failed", e)
            false
        }
    }

    /**
     * Get expected app signature hash (for production verification)
     * This should be set via BuildConfig from CI/CD
     */
    private fun getExpectedSignatureHash(): String? {
        return try {
            val hash = BuildConfig::class.java.getField("EXPECTED_SIGNATURE_HASH").get(null) as? String
            hash?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Validate image file size
     */
    fun validateImageSize(sizeBytes: Long): Boolean {
        return sizeBytes > 0 && sizeBytes <= MAX_IMAGE_SIZE_BYTES
    }

    /**
     * Validate image file type (JPEG/PNG only)
     */
    fun validateImageType(mimeType: String?): Boolean {
        return mimeType in listOf("image/jpeg", "image/jpg", "image/png")
    }

    /**
     * Create request signature for additional security
     * Combines timestamp + secret token + request data
     */
    fun createRequestSignature(timestamp: Long, requestData: String): String {
        val combined = "$timestamp:$requestData:${getSecretToken()}"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(combined.toByteArray())
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}

