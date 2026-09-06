package com.grappim.kit.logger

import com.grappim.kit.logger.KitLogger.Companion.install
import com.grappim.kit.logger.KitLogger.Companion.uninstall
import kotlin.concurrent.Volatile

/**
 * Logger that [logcat] delegates to. Call [install] to set a logger,
 * the default is a no-op logger. Call [uninstall] to revert to no-op.
 */
interface KitLogger {

    fun log(priority: LogPriority, tag: String?, throwable: Throwable?, message: () -> String)

    companion object {
        @Volatile
        @PublishedApi
        internal var logger: KitLogger = NoLog
            private set

        val isInstalled: Boolean
            get() = logger !== NoLog

        fun install(logger: KitLogger) {
            this.logger = logger
        }

        fun uninstall() {
            logger = NoLog
        }
    }

    private object NoLog : KitLogger {
        override fun log(priority: LogPriority, tag: String?, throwable: Throwable?, message: () -> String) {
            // no-op
        }
    }
}
