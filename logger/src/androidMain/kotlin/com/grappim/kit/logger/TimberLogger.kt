package com.grappim.kit.logger

import timber.log.Timber

class TimberLogger : KitLogger {

    override fun log(priority: LogPriority, tag: String?, throwable: Throwable?, message: () -> String) {
        val timberPriority = when (priority) {
            LogPriority.VERBOSE -> android.util.Log.VERBOSE
            LogPriority.DEBUG -> android.util.Log.DEBUG
            LogPriority.INFO -> android.util.Log.INFO
            LogPriority.WARN -> android.util.Log.WARN
            LogPriority.ERROR -> android.util.Log.ERROR
            LogPriority.ASSERT -> android.util.Log.ASSERT
        }

        val tree = if (tag != null) Timber.tag(tag) else Timber
        tree.log(timberPriority, throwable, message())
    }

    companion object {
        fun install() {
            if (!KitLogger.isInstalled) {
                KitLogger.install(TimberLogger())
            }
        }
    }
}
