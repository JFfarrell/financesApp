package com.example.personalfinances.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/**
 * Plain JVM tests for [PasswordHasher]; run with `./gradlew :app:testDebugUnitTest`.
 * They use a low iteration count where possible so the suite stays fast.
 */
class PasswordHasherTest {
    private val fast = 1_000

    @Test
    fun correctPasswordVerifies() {
        val stored = PasswordHasher.hash("correct horse", fast)
        assertTrue(PasswordHasher.verify("correct horse", stored))
    }

    @Test
    fun wrongPasswordDoesNotVerify() {
        val stored = PasswordHasher.hash("correct horse", fast)
        assertFalse(PasswordHasher.verify("Correct horse", stored))
        assertFalse(PasswordHasher.verify("", stored))
    }

    @Test
    fun samePasswordGivesDifferentHashesBecauseOfTheSalt() {
        assertNotEquals(PasswordHasher.hash("same", fast), PasswordHasher.hash("same", fast))
    }

    @Test
    fun storedValueDoesNotContainThePassword() {
        assertFalse(PasswordHasher.hash("hunter2-secret", fast).contains("hunter2-secret"))
    }

    @Test
    fun oldUnsaltedHashStillVerifiesAndIsFlaggedForUpgrade() {
        val legacy = MessageDigest.getInstance("SHA-256").digest("hunter2".toByteArray())
            .joinToString("") { "%02x".format(it) }

        assertTrue(PasswordHasher.verify("hunter2", legacy))
        assertFalse(PasswordHasher.verify("hunter3", legacy))
        assertTrue(PasswordHasher.needsUpgrade(legacy))
    }

    @Test
    fun currentHashNeedsNoUpgradeButALowIterationOneDoes() {
        assertFalse(PasswordHasher.needsUpgrade(PasswordHasher.hash("x")))
        assertTrue(PasswordHasher.needsUpgrade(PasswordHasher.hash("x", fast)))
    }

    @Test
    fun malformedStoredValuesNeverVerify() {
        assertFalse(PasswordHasher.verify("x", "garbage"))
        assertFalse(PasswordHasher.verify("x", "pbkdf2-sha256\$abc\$AAAA\$BBBB"))
        assertFalse(PasswordHasher.verify("x", "pbkdf2-sha256\$999999999\$AAAA\$BBBB"))
        assertFalse(PasswordHasher.verify("x", "pbkdf2-sha256\$1000\$!!!\$???"))
        assertFalse(PasswordHasher.verify("x", ""))
    }
}
