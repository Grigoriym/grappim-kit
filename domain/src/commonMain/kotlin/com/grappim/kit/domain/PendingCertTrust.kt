package com.grappim.kit.domain

import kotlinx.serialization.Serializable

/**
 * Everything a user needs to see before deciding to trust a certificate the device's CA store
 * rejected — a self-signed one, or a homelab CA's.
 *
 * Deliberately free of `java.security.cert.X509Certificate`: the trust decision is made on a
 * screen, and a screen lives in `commonMain`. `@Serializable` so a storage layer can JSON-encode
 * the accepted list rather than storing a lossy `host|fingerprint` string.
 */
@Serializable
data class PendingCertTrust(
    val host: String,
    val subject: String,
    val issuer: String,
    val notBefore: String,
    val notAfter: String,
    val sha256Fingerprint: String
)
