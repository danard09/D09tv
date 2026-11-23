package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

enum class UserRole {
    NONE,
    USER,
    ADMIN,
}

sealed interface AuthVerificationResult {
    data class Success(val role: UserRole) : AuthVerificationResult
    data class InvalidCode(val remainingAttempts: Int) : AuthVerificationResult
    data class RateLimited(val waitSeconds: Int) : AuthVerificationResult
}

data class StoredCredential(
    val saltHex: String,
    val hashHex: String,
)

/**
 * Handles salted PBKDF2-HMAC-SHA256 access code verification, constant-time comparison,
 * brute-force throttling, and Android Keystore AES-GCM sealing for locally persisted data.
 *
 * Security boundary note: On-device code verification supports standalone/offline TV boxes
 * and preview builds. Production deployments with multi-user or remote playlists must move
 * credential verification, role authorization, and channel mutation APIs to a backend server.
 */
class AccessControlManager {

    companion object {
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 65_536
        private const val KEY_LENGTH_BITS = 256
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val COOLDOWN_MS = 15_000L

        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "d09_tv_session_aes_key"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128

        // Fixed per-role salts for initial verification (no plaintext credentials stored in prefs or UI)
        private const val DEFAULT_USER_SALT_HEX = "64303974765f757365725f73616c7431"
        private const val DEFAULT_ADMIN_SALT_HEX = "64303974765f61646d696e5f73616c74"
    }

    private val random = SecureRandom()
    private var failedAttempts = 0
    private var lockedUntilMs = 0L

    private val defaultUserHashBytes: ByteArray by lazy {
        deriveHashBytes(charArrayOf('0', '9', '0', '9'), hexToBytes(DEFAULT_USER_SALT_HEX))
    }

    private val defaultAdminHashBytes: ByteArray by lazy {
        deriveHashBytes(charArrayOf('2', '0', '0', '9'), hexToBytes(DEFAULT_ADMIN_SALT_HEX))
    }

    fun verifyUserCode(
        enteredCode: String,
        customCredential: StoredCredential?,
        nowMs: Long = System.currentTimeMillis(),
    ): AuthVerificationResult {
        val saltBytes = if (customCredential != null) hexToBytes(customCredential.saltHex) else hexToBytes(DEFAULT_USER_SALT_HEX)
        val expectedBytes = if (customCredential != null) hexToBytes(customCredential.hashHex) else defaultUserHashBytes
        return verifyAgainstRole(
            enteredCode = enteredCode,
            targetRole = UserRole.USER,
            saltBytes = saltBytes,
            expectedBytes = expectedBytes,
            nowMs = nowMs,
        )
    }

    fun verifyAdminCode(
        enteredCode: String,
        customCredential: StoredCredential?,
        nowMs: Long = System.currentTimeMillis(),
    ): AuthVerificationResult {
        val saltBytes = if (customCredential != null) hexToBytes(customCredential.saltHex) else hexToBytes(DEFAULT_ADMIN_SALT_HEX)
        val expectedBytes = if (customCredential != null) hexToBytes(customCredential.hashHex) else defaultAdminHashBytes
        return verifyAgainstRole(
            enteredCode = enteredCode,
            targetRole = UserRole.ADMIN,
            saltBytes = saltBytes,
            expectedBytes = expectedBytes,
            nowMs = nowMs,
        )
    }

    private fun verifyAgainstRole(
        enteredCode: String,
        targetRole: UserRole,
        saltBytes: ByteArray,
        expectedBytes: ByteArray,
        nowMs: Long,
    ): AuthVerificationResult {
        if (nowMs < lockedUntilMs) {
            val waitSeconds = (((lockedUntilMs - nowMs) + 999L) / 1000L).toInt().coerceAtLeast(1)
            return AuthVerificationResult.RateLimited(waitSeconds)
        }

        val clean = enteredCode.trim()
        if (clean.isEmpty()) {
            return recordFailure(nowMs)
        }

        val candidateBytes = deriveHashBytes(clean.toCharArray(), saltBytes)

        return if (MessageDigest.isEqual(candidateBytes, expectedBytes)) {
            failedAttempts = 0
            lockedUntilMs = 0L
            AuthVerificationResult.Success(targetRole)
        } else {
            recordFailure(nowMs)
        }
    }

    private fun recordFailure(nowMs: Long): AuthVerificationResult {
        failedAttempts += 1
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntilMs = nowMs + COOLDOWN_MS
            failedAttempts = 0
            return AuthVerificationResult.RateLimited((COOLDOWN_MS / 1000L).toInt())
        }
        return AuthVerificationResult.InvalidCode(MAX_FAILED_ATTEMPTS - failedAttempts)
    }

    fun resetThrottle() {
        failedAttempts = 0
        lockedUntilMs = 0L
    }

    fun createCredential(newCode: String): StoredCredential {
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val hash = deriveHashBytes(newCode.trim().toCharArray(), salt)
        return StoredCredential(
            saltHex = bytesToHex(salt),
            hashHex = bytesToHex(hash),
        )
    }

    fun generateSessionToken(): String {
        val bytes = ByteArray(32).also { random.nextBytes(it) }
        return bytesToHex(bytes)
    }

    fun sealString(plainText: String): String {
        return runCatching {
            val key = getOrCreateKeystoreSecretKey() ?: return plainText
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(1 + iv.size + ciphertext.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(ciphertext, 0, combined, 1 + iv.size, ciphertext.size)
            "enc:${bytesToHex(combined)}"
        }.getOrDefault(plainText)
    }

    fun unsealString(sealed: String?): String? {
        if (sealed.isNullOrBlank()) return null
        if (!sealed.startsWith("enc:")) return sealed
        return runCatching {
            val key = getOrCreateKeystoreSecretKey() ?: return null
            val payload = hexToBytes(sealed.removePrefix("enc:"))
            val ivLength = payload[0].toInt() and 0xFF
            val iv = payload.copyOfRange(1, 1 + ivLength)
            val ciphertext = payload.copyOfRange(1 + ivLength, payload.size)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun getOrCreateKeystoreSecretKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun deriveHashBytes(chars: CharArray, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val spec = PBEKeySpec(chars, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return try {
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(HEX_CHARS[v ushr 4])
            sb.append(HEX_CHARS[v and 0x0F])
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        val out = ByteArray(clean.length / 2)
        for (i in out.indices) {
            out[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return out
    }
}

private const val HEX_CHARS = "0123456789abcdef"
