package com.grappim.kit.storage.cert

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.grappim.kit.domain.PendingCertTrust
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * The certificates the user has accepted, pinned to the host they were accepted *for*.
 *
 * The pin is `(host, fingerprint)` rather than the fingerprint alone: a certificate trusted for
 * one server must not silently authenticate a different one that presents the same bytes.
 */
interface TrustedCertStorage {
    suspend fun isTrusted(host: String, sha256Fingerprint: String): Boolean
    suspend fun trust(pendingCertTrust: PendingCertTrust)
    fun getAllFlow(): Flow<List<PendingCertTrust>>
    suspend fun untrust(host: String, sha256Fingerprint: String)
}

/**
 * Takes the `DataStore<Preferences>` and `Json` as constructor params rather than building them
 * itself: a consuming app already owns a DataStore file/builder for its own settings and likely
 * its own `Json` instance, and this stores its one key (`trusted_certs`) into whichever store the
 * app passes in — no DI-framework annotation, same DI-agnostic shape every other `grappim-kit`
 * class uses.
 *
 * Stores the full `PendingCertTrust` JSON-encoded, not just `host|fingerprint`: a settings screen
 * revoking a pin needs the subject/issuer/validity fields to show what it's revoking.
 */
class TrustedCertStorageImpl(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : TrustedCertStorage {

    private val trustedEntriesFlow: Flow<List<PendingCertTrust>> =
        dataStore.data.map { prefs -> decodeEntries(prefs[KEY_TRUSTED_CERTS]) }

    override suspend fun isTrusted(host: String, sha256Fingerprint: String): Boolean =
        trustedEntriesFlow.first().any { it.matches(host, sha256Fingerprint) }

    override suspend fun trust(pendingCertTrust: PendingCertTrust) {
        dataStore.edit { prefs ->
            val entries = decodeEntries(prefs[KEY_TRUSTED_CERTS])
                .filterNot { it.matches(pendingCertTrust.host, pendingCertTrust.sha256Fingerprint) }
            prefs[KEY_TRUSTED_CERTS] = json.encodeToString(entries + pendingCertTrust)
        }
    }

    override fun getAllFlow(): Flow<List<PendingCertTrust>> = trustedEntriesFlow

    override suspend fun untrust(host: String, sha256Fingerprint: String) {
        dataStore.edit { prefs ->
            val entries = decodeEntries(prefs[KEY_TRUSTED_CERTS]).filterNot { it.matches(host, sha256Fingerprint) }
            prefs[KEY_TRUSTED_CERTS] = json.encodeToString(entries)
        }
    }

    private fun decodeEntries(value: String?): List<PendingCertTrust> =
        value?.takeIf { it.isNotBlank() }?.let { json.decodeFromString(it) } ?: emptyList()

    private fun PendingCertTrust.matches(host: String, sha256Fingerprint: String) =
        this.host == host && this.sha256Fingerprint == sha256Fingerprint

    private companion object {
        private val KEY_TRUSTED_CERTS = stringPreferencesKey("trusted_certs")
    }
}
