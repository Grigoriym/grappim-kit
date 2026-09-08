package com.grappim.kit.uikit

import androidx.compose.ui.platform.UriHandler
import com.grappim.kit.logger.LogPriority
import com.grappim.kit.logger.logcat

/**
 * Refuses to open any URI whose scheme isn't http(s) before delegating. Server- or
 * user-supplied text (custom fields, markdown links, attachment URLs) can reach
 * [UriHandler.openUri] as free text; on Android an implicit `ACTION_VIEW` intent is launched
 * as-is, so an unchecked scheme (e.g. `intent://`) is a deep-link-into-another-app vector.
 */
internal class SafeUriHandler(private val delegate: UriHandler) : UriHandler {
    override fun openUri(uri: String) {
        if (uri.startsWith("http://", ignoreCase = true) || uri.startsWith("https://", ignoreCase = true)) {
            delegate.openUri(uri)
        } else {
            logcat(LogPriority.WARN) { "Refused to open a URI with a non-http(s) scheme" }
        }
    }
}
