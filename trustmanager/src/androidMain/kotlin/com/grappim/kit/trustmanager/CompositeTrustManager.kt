package com.grappim.kit.trustmanager

import com.grappim.kit.domain.CertificateHostnameMismatchException
import com.grappim.kit.domain.PendingCertTrust
import com.grappim.kit.domain.UntrustedCertificateException
import com.grappim.kit.storage.cert.TrustedCertStorage
import kotlinx.coroutines.runBlocking
import java.net.Socket
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

/**
 * The device's CA store, plus the certificates the user has accepted for a specific host
 * (trust-on-first-use).
 *
 * Two things it deliberately does *not* do: a pin is scoped to the host it was accepted for, and
 * validity is still checked on every connection, so an accepted certificate does not become
 * trusted forever.
 */
class CompositeTrustManager(
    private val defaultTrustManager: X509TrustManager,
    private val trustedCertStorage: TrustedCertStorage
) : X509ExtendedTrustManager() {

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
        defaultTrustManager.checkClientTrusted(chain, authType)
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String, socket: Socket) {
        defaultTrustManager.checkClientTrusted(chain, authType)
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String, engine: SSLEngine) {
        defaultTrustManager.checkClientTrusted(chain, authType)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        checkServerTrusted(chain, authType, host = null)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, socket: Socket) {
        val host = (socket as? SSLSocket)?.handshakeSession?.peerHost
        checkServerTrusted(chain, authType, host)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, engine: SSLEngine) {
        checkServerTrusted(chain, authType, engine.peerHost)
    }

    // Takes the host explicitly, rather than only the SSLSocket/SSLEngine overrides above, so the
    // pinning decision is testable without a real handshake.
    internal fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, host: String?) {
        val leaf = chain.firstOrNull() ?: throw CertificateException("Empty certificate chain")

        // A JSSE callback has no suspension point, so the pin lookup blocks here rather than
        // making the whole handshake path suspend.
        val isPinned = host != null && runBlocking { trustedCertStorage.isTrusted(host, sha256Fingerprint(leaf)) }
        if (isPinned) {
            // Still checked on a pin hit: accepting a certificate once is not accepting it after
            // it expires.
            leaf.checkValidity()
            return
        }

        try {
            defaultTrustManager.checkServerTrusted(chain, authType)
        } catch (e: CertificateException) {
            // Without a host there's nothing to offer trust-on-first-use for, so let the original
            // failure propagate unchanged instead of wrapping it.
            if (host == null) throw e
            // Pinning a cert whose CN/SAN doesn't cover this host would never let a connection
            // actually succeed — hostname verification runs separately, after this check, and
            // would still reject it — so don't offer TOFU for it, surface a clear error instead.
            // Wrapped in a CertificateException at this JSSE boundary rather than thrown bare:
            // unlike TaigaMobileNova's own exception types, grappim-kit-domain's are plain
            // commonMain Exceptions (portable to iOS/JVM common code), not CertificateException
            // subclasses, so a real X509TrustManager caller — which per the JSSE contract only
            // expects checkServerTrusted to throw CertificateException — needs this wrapping to
            // see a well-formed failure. findPendingCertTrust()/the cause chain is how a caller
            // reads the real exception back out.
            if (!hostMatchesCertificate(host, leaf)) {
                throw CertificateException(
                    CertificateHostnameMismatchException(
                        "Certificate presented for $host does not match its subject/SAN entries",
                        e
                    )
                )
            }
            throw CertificateException(UntrustedCertificateException(pendingCertTrust(host, leaf), e))
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = defaultTrustManager.acceptedIssuers

    private fun pendingCertTrust(host: String, certificate: X509Certificate): PendingCertTrust = PendingCertTrust(
        host = host,
        subject = certificate.subjectX500Principal.name,
        issuer = certificate.issuerX500Principal.name,
        notBefore = formatDate(certificate.notBefore),
        notAfter = formatDate(certificate.notAfter),
        sha256Fingerprint = sha256Fingerprint(certificate)
    )

    private fun formatDate(date: Date): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    private fun hostMatchesCertificate(host: String, certificate: X509Certificate): Boolean {
        val sanValues = runCatching { certificate.subjectAlternativeNames }.getOrNull().orEmpty()
            .mapNotNull { entry ->
                val type = entry.getOrNull(0) as? Int
                (entry.getOrNull(1) as? String).takeIf { type == SAN_DNS_NAME || type == SAN_IP_ADDRESS }
            }
        if (sanValues.isNotEmpty()) return sanValues.any { matchesHostname(host, it) }

        // No SANs present: fall back to the certificate's CN, common for self-signed certs that
        // predate SAN-based hostname verification.
        val commonName = certificate.subjectX500Principal.name
            .split(",")
            .map { it.trim() }
            .firstOrNull { it.startsWith("CN=", ignoreCase = true) }
            ?.substringAfter("=")
        return commonName != null && matchesHostname(host, commonName)
    }

    private fun matchesHostname(host: String, pattern: String): Boolean = when {
        pattern.equals(host, ignoreCase = true) -> true

        pattern.startsWith("*.") ->
            host.length > pattern.length - 1 &&
                host.endsWith(pattern.substring(1), ignoreCase = true)

        else -> false
    }

    private companion object {
        const val SAN_DNS_NAME = 2
        const val SAN_IP_ADDRESS = 7
    }
}

fun sha256Fingerprint(certificate: X509Certificate): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
    return digest.joinToString(":") { byte -> "%02X".format(byte) }
}
