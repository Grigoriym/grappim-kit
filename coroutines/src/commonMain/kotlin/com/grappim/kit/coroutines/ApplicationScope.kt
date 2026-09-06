package com.grappim.kit.coroutines

import com.grappim.kit.logger.LogPriority
import com.grappim.kit.logger.logcat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * A process-lifetime [CoroutineScope]. An exception that escapes a coroutine launched on it
 * is logged via [logcat] rather than crashing the app -- install a `KitLogger` to see it.
 */
fun applicationScope(dispatcher: CoroutineDispatcher = KitDispatchers.default): CoroutineScope {
    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logcat(LogPriority.ERROR, throwable = throwable) { "Unhandled exception on applicationScope" }
    }
    return CoroutineScope(SupervisorJob() + dispatcher + exceptionHandler)
}
