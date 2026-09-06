package com.grappim.kit.domain

/**
 * The server's certificate doesn't cover the host being connected to — a genuine hostname
 * mismatch, distinct from [UntrustedCertificateException]'s "untrusted but otherwise pinnable"
 * case. There is nothing to offer trust-on-first-use for here: hostname verification runs as a
 * separate step after trust-manager checks and would still reject the connection even if this
 * certificate were pinned, so callers should surface this as a plain error, not a trust dialog.
 */
class CertificateHostnameMismatchException(
    message: String,
    override val cause: Throwable? = null
) : Exception(message)
