package com.example.policemobiledirectory.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import com.example.policemobiledirectory.BuildConfig
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App Signature Verifier
 * Verifies that the app hasn't been tampered with or repackaged
 * 
 * ⚠️ SECURITY: This helps prevent malicious repackaging of the app
 */
@Singleton
class AppSignatureVerifier @Inject constructor(
    private val context: Context,
    private val securityConfig: SecurityConfig
) {
    companion object {
        private const val TAG = "AppSignatureVerifier"
    }

    /**
     * Verify app signature matches expected value
     * Returns true if signature is valid, false otherwise
     */
    fun verifySignature(): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            
            val signatures = packageInfo.signatures
            if (signatures.isEmpty()) {
                Log.w(TAG, "No signatures found")
                return false
            }
            
            // Get the first signature (usually the only one)
            val signature = signatures[0].toByteArray()
            
            // Calculate SHA-256 hash
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signature)
            val signatureHash = Base64.encodeToString(digest, Base64.NO_WRAP)
            
            // Get expected hash from BuildConfig (set via CI/CD)
            val expectedHash = getExpectedSignatureHash()
            
            if (expectedHash.isNullOrBlank()) {
                // In development, just verify signature exists
                Log.d(TAG, "Expected signature hash not set (development mode)")
                return signatureHash.isNotBlank()
            }
            
            // Compare signatures
            val isValid = signatureHash == expectedHash
            if (!isValid) {
                Log.e(TAG, "⚠️ SIGNATURE MISMATCH! App may have been tampered with!")
                Log.e(TAG, "Expected: $expectedHash")
                Log.e(TAG, "Actual: $signatureHash")
            } else {
                Log.d(TAG, "✅ App signature verified")
            }
            
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification failed", e)
            false
        }
    }

    /**
     * Get expected signature hash from BuildConfig
     */
    private fun getExpectedSignatureHash(): String? {
        return try {
            val field = BuildConfig::class.java.getDeclaredField("EXPECTED_SIGNATURE_HASH")
            field.isAccessible = true
            val hash = field.get(null) as? String
            hash?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get current app signature hash (for generating expected hash)
     * Use this once to get the hash, then set it in BuildConfig
     */
    fun getCurrentSignatureHash(): String? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            
            val signatures = packageInfo.signatures
            if (signatures.isEmpty()) return null
            
            val signature = signatures[0].toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signature)
            Base64.encodeToString(digest, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get signature hash", e)
            null
        }
    }
}

