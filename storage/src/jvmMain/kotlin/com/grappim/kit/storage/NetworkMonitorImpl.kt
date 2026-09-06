package com.grappim.kit.storage

import com.grappim.kit.coroutines.KitDispatchers
import com.grappim.kit.coroutines.applicationScope
import com.grappim.kit.logger.LogPriority
import com.grappim.kit.logger.logcat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

private const val CHECK_HOST = "1.1.1.1"
private const val CHECK_PORT = 53
private const val CONNECT_TIMEOUT_MS = 2000
private const val CHECK_INTERVAL_MS = 5000L

/**
 * Polls TCP reachability of [CHECK_HOST] on a fixed interval, since the JVM has no OS-level
 * connectivity-change callback API. Starts optimistic ([MutableStateFlow] seeded `true`) and
 * corrects within one [CONNECT_TIMEOUT_MS] of startup — unlike Android's actual, which can query
 * `ConnectivityManager` synchronously for an immediate accurate initial value.
 *
 * [ioDispatcher]/[applicationScope] are plain constructor params, not DI-annotated: bind them to
 * your own [KitDispatchers.io]/[applicationScope] in the consuming app's DI module.
 */
class NetworkMonitorImpl(
    private val ioDispatcher: CoroutineDispatcher = KitDispatchers.io,
    private val applicationScope: CoroutineScope = applicationScope()
) : NetworkMonitor {

    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        applicationScope.launch(ioDispatcher) {
            while (isActive) {
                val online = isHostReachable(CHECK_HOST, CHECK_PORT, CONNECT_TIMEOUT_MS)
                if (online != _isOnline.value) {
                    logcat(LogPriority.INFO) { "Network connectivity changed: online=$online" }
                }
                _isOnline.value = online
                delay(CHECK_INTERVAL_MS)
            }
        }
    }
}

internal fun isHostReachable(host: String, port: Int, timeoutMs: Int): Boolean = try {
    Socket().use { socket -> socket.connect(InetSocketAddress(host, port), timeoutMs) }
    true
} catch (expected: IOException) {
    false
}
