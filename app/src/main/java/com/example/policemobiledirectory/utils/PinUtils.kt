package com.example.policemobiledirectory.utils

import android.util.Log
import java.security.MessageDigest

private const val TAG = "PasswordUtils_DEBUG"

object PinUtils {

    fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashedBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
            // Convert byte array to hex string
            val hexString = hashedBytes.joinToString("") { "%02x".format(it) }
            Log.d(TAG, "hashPassword: input='$password', output='$hexString'") // Log hash generation
            hexString
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hash password", e)
            throw RuntimeException("Failed to hash password", e)
        }
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        Log.d(TAG, "verifyPassword: Attempting to verify.")
        Log.d(TAG, "verifyPassword: Input Password (raw): '$password'")
        Log.d(TAG, "verifyPassword: Stored Hash (from DB): '$storedHash'")
        return try {
            val newHash = hashPassword(password) // Hash the input password
            Log.d(TAG, "verifyPassword: Newly Generated Hash (from input): '$newHash'")
            
            val result = newHash.equals(storedHash, ignoreCase = true)
            Log.d(TAG, "verifyPassword: Comparison (newHash.equals(storedHash, ignoreCase=true)): $result")
            
            if (newHash.length != storedHash.length) {
                Log.w(TAG, "verifyPassword: HASH LENGTHS DIFFER! newHash length=${newHash.length}, storedHash length=${storedHash.length}")
            }
            if (!result && newHash.length == storedHash.length) {
                var firstDiff = -1
                for (i in newHash.indices) {
                    if (newHash[i].lowercaseChar() != storedHash[i].lowercaseChar()) {
                        firstDiff = i
                        break
                    }
                }
                if (firstDiff != -1) {
                    Log.w(TAG, "verifyPassword: Hashes are same length but differ starting at index $firstDiff. newHash[${firstDiff}]=${newHash[firstDiff]}, storedHash[${firstDiff}]=${storedHash[firstDiff]}")
                } else if (newHash.lowercase() == storedHash.lowercase()) {
                     Log.i(TAG, "verifyPassword: Hashes appear identical after lowercasing when compared char-by-char, yet .equals failed. This is strange.")
                } else {
                     Log.w(TAG, "verifyPassword: Hashes are same length but .equals is false. No difference found by manual char check, this is very strange.")
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "verifyPassword: Exception during verification", e)
            false
        }
    }
}

// Temporary main function has been removed.
