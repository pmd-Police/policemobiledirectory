package com.example.policemobiledirectory.utils

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_SIZE = 16

    fun hashPassword(pin: String): String {
        val salt = ByteArray(SALT_SIZE)
        SecureRandom().nextBytes(salt)

        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hash = factory.generateSecret(spec).encoded

        return "${toHex(salt)}:${toHex(hash)}"
    }

    fun verifyPin(pin: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false

        val salt = fromHex(parts[0])
        val hash = fromHex(parts[1])

        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val testHash = factory.generateSecret(spec).encoded

        return hash.contentEquals(testHash)
    }

    private fun toHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun fromHex(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            val index = i * 2
            val j = Integer.parseInt(hex.substring(index, index + 2), 16)
            result[i] = j.toByte()
        }
        return result
    }
}
