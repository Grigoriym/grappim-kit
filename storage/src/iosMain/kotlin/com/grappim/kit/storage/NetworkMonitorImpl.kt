package com.grappim.kit.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Always reports online. iOS has no equivalent of Android's `ConnectivityManager` callback wired
 * up here yet — ported as-is from the source apps, which shipped the same stub.
 */
class NetworkMonitorImpl : NetworkMonitor {
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
}
