package com.grappim.kit.trustmanager

import com.grappim.kit.domain.CertificateHostnameMismatchException
import com.grappim.kit.domain.PendingCertTrust
import com.grappim.kit.domain.findPendingCertTrust
import com.grappim.kit.testing.FakeTrustedCertStorage
import kotlinx.coroutines.test.runTest
import java.security.cert.CertificateException
import java.security.cert.CertificateExpiredException
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CompositeTrustManagerTest {

    private val certA = FakeX509Certificate(byteArrayOf(1, 2, 3))
    private val certB = FakeX509Certificate(byteArrayOf(4, 5, 6))

    private fun createSut(
        defaultTrustManager: FakeX509TrustManager = FakeX509TrustManager(),
        trustedCertStorage: FakeTrustedCertStorage = FakeTrustedCertStorage()
    ) = CompositeTrustManager(defaultTrustManager, trustedCertStorage)

    private suspend fun FakeTrustedCertStorage.trust(host: String, sha256Fingerprint: String) {
        trust(
            PendingCertTrust(
                host = host,
                subject = "CN=$host",
                issuer = "CN=Test CA",
                notBefore = "2026-01-01",
                notAfter = "2027-01-01",
                sha256Fingerprint = sha256Fingerprint
            )
        )
    }

    @Test
    fun `unpinned cert delegates to the default trust manager`() = runTest {
        val defaultTrustManager = FakeX509TrustManager(serverTrustedThrows = false)
        val sut = createSut(defaultTrustManager = defaultTrustManager)

        sut.checkServerTrusted(arrayOf(certA), "RSA", "example.com")

        assertTrue(defaultTrustManager.checkServerTrustedCalled)
    }

    @Test
    fun `unpinned cert rejected by the default trust manager throws`() = runTest {
        val sut = createSut(defaultTrustManager = FakeX509TrustManager(serverTrustedThrows = true))

        assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(certA), "RSA", "example.com")
        }
    }

    @Test
    fun `unpinned cert with a known host is wrapped with the presented cert's details for TOFU`() = runTest {
        val sut = createSut(defaultTrustManager = FakeX509TrustManager(serverTrustedThrows = true))

        val exception = assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(certA), "RSA", "example.com")
        }

        val pendingCertTrust = assertNotNull(exception.findPendingCertTrust())
        assertEquals("example.com", pendingCertTrust.host)
        assertEquals(sha256Fingerprint(certA), pendingCertTrust.sha256Fingerprint)
    }

    @Test
    fun `pinned host and fingerprint is trusted without consulting the default trust manager`() = runTest {
        val trustedCertStorage = FakeTrustedCertStorage()
        trustedCertStorage.trust("example.com", sha256Fingerprint(certA))
        val defaultTrustManager = FakeX509TrustManager(serverTrustedThrows = true)
        val sut = createSut(defaultTrustManager = defaultTrustManager, trustedCertStorage = trustedCertStorage)

        sut.checkServerTrusted(arrayOf(certA), "RSA", "example.com")

        assertFalse(defaultTrustManager.checkServerTrustedCalled)
    }

    @Test
    fun `pin for one host does not trust a different host presenting the same certificate`() = runTest {
        val trustedCertStorage = FakeTrustedCertStorage()
        trustedCertStorage.trust("example.com", sha256Fingerprint(certA))
        val sut = createSut(
            defaultTrustManager = FakeX509TrustManager(serverTrustedThrows = true),
            trustedCertStorage = trustedCertStorage
        )

        assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(certA), "RSA", "other.example.com")
        }
    }

    @Test
    fun `pin for a host does not trust a different certificate presented by that same host`() = runTest {
        val trustedCertStorage = FakeTrustedCertStorage()
        trustedCertStorage.trust("example.com", sha256Fingerprint(certA))
        val sut = createSut(
            defaultTrustManager = FakeX509TrustManager(serverTrustedThrows = true),
            trustedCertStorage = trustedCertStorage
        )

        assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(certB), "RSA", "example.com")
        }
    }

    @Test
    fun `pinned but expired certificate still throws instead of being silently trusted`() = runTest {
        val expiredCert = FakeX509Certificate(
            encodedBytes = byteArrayOf(7, 8, 9),
            notBefore = Date(0),
            notAfter = Date(1)
        )
        val trustedCertStorage = FakeTrustedCertStorage()
        trustedCertStorage.trust("example.com", sha256Fingerprint(expiredCert))
        val sut = createSut(
            defaultTrustManager = FakeX509TrustManager(serverTrustedThrows = true),
            trustedCertStorage = trustedCertStorage
        )

        assertFailsWith<CertificateExpiredException> {
            sut.checkServerTrusted(arrayOf(expiredCert), "RSA", "example.com")
        }
    }

    @Test
    fun `null host cannot be pinned and always falls back to the default trust manager`() = runTest {
        val trustedCertStorage = FakeTrustedCertStorage()
        trustedCertStorage.trust("example.com", sha256Fingerprint(certA))
        val defaultTrustManager = FakeX509TrustManager(serverTrustedThrows = true)
        val sut = createSut(defaultTrustManager = defaultTrustManager, trustedCertStorage = trustedCertStorage)

        assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(certA), "RSA", host = null)
        }
        assertTrue(defaultTrustManager.checkServerTrustedCalled)
    }

    @Test
    fun `null host failure is not wrapped since there's no host to offer TOFU for`() = runTest {
        val sut = createSut(defaultTrustManager = FakeX509TrustManager(serverTrustedThrows = true))

        // The original failure propagates as a plain CertificateException, not the
        // UntrustedCertificateException/CertificateHostnameMismatchException subtypes checkServerTrusted
        // throws when there's a host to offer TOFU for.
        val exception = assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(certA), "RSA", host = null)
        }

        assertEquals("untrusted", exception.message)
    }

    @Test
    fun `unpinned cert whose CN does not cover the connecting host is not offered for TOFU`() = runTest {
        val cert = FakeX509Certificate(byteArrayOf(9, 9, 9), commonName = "other-host.example.com")
        val sut = createSut(defaultTrustManager = FakeX509TrustManager(serverTrustedThrows = true))

        val exception = assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(cert), "RSA", "example.com")
        }

        assertTrue(exception.cause is CertificateHostnameMismatchException)
    }

    @Test
    fun `unpinned cert with a SAN IP matching the connecting host is still offered for TOFU`() = runTest {
        val cert = FakeX509Certificate(
            byteArrayOf(9, 9, 9),
            subjectAlternativeNames = listOf(listOf(7, "192.168.0.241"))
        )
        val sut = createSut(defaultTrustManager = FakeX509TrustManager(serverTrustedThrows = true))

        val exception = assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(cert), "RSA", "192.168.0.241")
        }

        assertNotNull(exception.findPendingCertTrust())
    }

    @Test
    fun `unpinned cert with a SAN IP not matching the connecting host is not offered for TOFU`() = runTest {
        val cert = FakeX509Certificate(
            byteArrayOf(9, 9, 9),
            subjectAlternativeNames = listOf(listOf(7, "192.168.0.241"))
        )
        val sut = createSut(defaultTrustManager = FakeX509TrustManager(serverTrustedThrows = true))

        val exception = assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(cert), "RSA", "192.168.0.248")
        }

        assertTrue(exception.cause is CertificateHostnameMismatchException)
    }
}
