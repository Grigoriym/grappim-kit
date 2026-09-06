package com.grappim.kit.storage

/**
 * Encrypts a string before it reaches disk-backed storage, which stores plaintext otherwise.
 *
 * A platform seam rather than an `expect`/`actual`: the Android implementation talks to the
 * Android Keystore, which does not exist in a host test, so every storage test would have to be
 * an instrumented one. As an interface it can be faked, and code that stores the encrypted value
 * stays in `commonMain`.
 *
 * Named "secret", not "token": this is a general-purpose encrypt/decrypt-a-string module, not
 * specific to auth tokens.
 */
interface SecretCipher {

    /** Throws if the platform key store is unavailable — a genuine failure the caller surfaces. */
    fun encrypt(value: String): String

    /**
     * `null` when [value] cannot be read back: the key was invalidated, or the ciphertext came
     * from a device backup restored onto another device. That is an expected state, not an
     * error — it means "no key", and the caller re-onboards.
     */
    fun decrypt(value: String): String?
}

/** Passes every value through unchanged — a default/test double for a platform with no Keystore. */
class NoopSecretCipher : SecretCipher {
    override fun encrypt(value: String): String = value
    override fun decrypt(value: String): String = value
}
