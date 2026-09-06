package com.grappim.kit.testing

import com.grappim.kit.domain.PendingCertTrust
import com.grappim.kit.storage.cert.TrustedCertStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** An empty pin list is the honest default: a device that has trusted nothing yet. */
class FakeTrustedCertStorage : TrustedCertStorage {

    private val _pins = MutableStateFlow<List<PendingCertTrust>>(emptyList())
    val pins: StateFlow<List<PendingCertTrust>> = _pins.asStateFlow()

    var trustCalledWith: PendingCertTrust? = null
    var untrustCalledWith: Pair<String, String>? = null

    override suspend fun isTrusted(host: String, sha256Fingerprint: String): Boolean =
        _pins.value.any { it.host == host && it.sha256Fingerprint == sha256Fingerprint }

    override suspend fun trust(pendingCertTrust: PendingCertTrust) {
        trustCalledWith = pendingCertTrust
        _pins.value = _pins.value.filterNot {
            it.host == pendingCertTrust.host && it.sha256Fingerprint == pendingCertTrust.sha256Fingerprint
        } + pendingCertTrust
    }

    override fun getAllFlow(): StateFlow<List<PendingCertTrust>> = _pins

    override suspend fun untrust(host: String, sha256Fingerprint: String) {
        untrustCalledWith = host to sha256Fingerprint
        _pins.value = _pins.value.filterNot { it.host == host && it.sha256Fingerprint == sha256Fingerprint }
    }
}
