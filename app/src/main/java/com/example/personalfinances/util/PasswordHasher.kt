package com.example.personalfinances.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Stores and checks the app password without keeping the password itself.
 *
 * Passwords are hashed with PBKDF2-HMAC-SHA256: a slow, salted key-derivation function. The salt
 * is random per password, so two identical passwords produce different hashes and precomputed
 * tables are useless. The slowness (hundreds of thousands of iterations) makes each guess costly
 * for anyone who obtains the stored value.
 *
 * A stored value is `pbkdf2-sha256$<iterations>$<salt>$<hash>` with the salt and hash in Base64,
 * so the parameters travel with the hash and can be raised later: [needsUpgrade] reports values
 * made with fewer iterations, and callers re-hash them after a successful login.
 *
 * Earlier versions of the app stored an unsalted SHA-256 (64 hex characters). [verify] still
 * accepts that format so an existing password keeps working, and [needsUpgrade] flags it so it is
 * replaced on the next login.
 */
object PasswordHasher {
    private const val SCHEME = "pbkdf2-sha256"
    private const val ITERATIONS = 600_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256

    // Bounds on iteration counts read from storage, so a corrupted value cannot hang the app.
    private const val MIN_ITERATIONS = 1_000
    private const val MAX_ITERATIONS = 10_000_000

    /** Returns the storable hash of [password] using a fresh random salt. */
    fun hash(password: String, iterations: Int = ITERATIONS): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val key = derive(password, salt, iterations, KEY_BITS)
        return listOf(SCHEME, iterations.toString(), encode(salt), encode(key)).joinToString("$")
    }

    /** True if [password] matches the [stored] value, in either the current or the legacy format. */
    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split('$')
        if (parts.size == 4 && parts[0] == SCHEME) {
            val iterations = parts[1].toIntOrNull()?.takeIf { it in MIN_ITERATIONS..MAX_ITERATIONS }
                ?: return false
            val salt = decode(parts[2]) ?: return false
            val expected = decode(parts[3])?.takeIf { it.isNotEmpty() } ?: return false
            // isEqual compares in constant time, so timing does not reveal how much matched.
            return MessageDigest.isEqual(derive(password, salt, iterations, expected.size * 8), expected)
        }
        if (isLegacy(stored)) {
            return MessageDigest.isEqual(legacyHash(password).toByteArray(), stored.toByteArray())
        }
        return false
    }

    /** True if [stored] is the old unsalted format or was made with fewer iterations than now. */
    fun needsUpgrade(stored: String): Boolean {
        if (isLegacy(stored)) return true
        val parts = stored.split('$')
        if (parts.size != 4 || parts[0] != SCHEME) return true
        return (parts[1].toIntOrNull() ?: return true) < ITERATIONS
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int, keyBits: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyBits)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().withoutPadding().encodeToString(bytes)

    private fun decode(text: String): ByteArray? =
        try { Base64.getDecoder().decode(text) } catch (e: IllegalArgumentException) { null }

    private fun isLegacy(stored: String): Boolean =
        stored.length == 64 && stored.all { it in '0'..'9' || it in 'a'..'f' }

    private fun legacyHash(password: String): String =
        MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
