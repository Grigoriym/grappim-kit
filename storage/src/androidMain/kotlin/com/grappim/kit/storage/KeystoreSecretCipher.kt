package com.grappim.kit.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.grappim.kit.logger.LogPriority
import com.grappim.kit.logger.logcat
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES/GCM with a key that never leaves the Android Keystore, so [value] is never sitting in
 * cleartext inside a settings file. Stored form is `v1:base64(iv || ciphertext)`.
 *
 * [keyAlias] is a required constructor param, not hardcoded: two consuming apps sharing a device
 * must not collide on the same Keystore entry.
 *
 * [legacyUnprefixedIsPlaintext] controls how a value with no [CIPHERTEXT_PREFIX] is read —
 * **this only means something for a consumer with pre-existing installs**; a brand-new consumer
 * can leave it at the default. `true` (default) is TaigaMobileNova's case: a real prior-plaintext
 * value written before any cipher existed, so [decrypt] passes it through unchanged rather than
 * failing, and a consuming app migrates it to ciphertext next write. `false` is wallosmobile's
 * case: the consumer's *own* pre-swap cipher already wrote real ciphertext in this same
 * `base64(iv || ciphertext)` shape with no prefix (same alias, same AES/GCM/NoPadding, same
 * 12-byte IV) — for it, an unprefixed value is never plaintext, and the `true` default
 * misclassifies that real ciphertext as legacy plaintext and returns it undecrypted, silently
 * corrupting it. Confirmed 2026-09-12: wallosmobile's swap onto this class shipped with the
 * default and broke the stored API key for every installed user on upgrade (all of
 * v1.0.0-v1.0.3) — see `CONSUMING.md`'s `storage` section.
 */
class KeystoreSecretCipher(
    private val keyAlias: String,
    private val legacyUnprefixedIsPlaintext: Boolean = true
) : SecretCipher {

    override fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.encodeToByteArray())
        return CIPHERTEXT_PREFIX + Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    @Suppress("ReturnCount")
    override fun decrypt(value: String): String? {
        if (legacyUnprefixedIsPlaintext && !value.startsWith(CIPHERTEXT_PREFIX)) return value

        val bytes = try {
            Base64.decode(value.removePrefix(CIPHERTEXT_PREFIX), Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            logcat(priority = LogPriority.WARN, throwable = e) { "Stored value is not valid base64" }
            return null
        }
        if (bytes.size <= IV_LENGTH_BYTES) {
            logcat(priority = LogPriority.WARN) { "Stored value is too short to hold an IV" }
            return null
        }
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(TAG_LENGTH_BITS, bytes, 0, IV_LENGTH_BYTES)
            )
            cipher.doFinal(bytes, IV_LENGTH_BYTES, bytes.size - IV_LENGTH_BYTES).decodeToString()
        } catch (e: GeneralSecurityException) {
            // The Keystore key is gone or no longer matches this ciphertext — a restored backup,
            // or a reinstall. Treated as "no key stored"; the caller re-onboards.
            logcat(priority = LogPriority.WARN, throwable = e) { "Stored value could not be decrypted" }
            null
        } catch (e: IOException) {
            logcat(priority = LogPriority.WARN, throwable = e) { "Keystore is unreadable" }
            null
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec
                .Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH_BYTES = 12
        private const val TAG_LENGTH_BITS = 128
        private const val CIPHERTEXT_PREFIX = "v1:"
    }
}
