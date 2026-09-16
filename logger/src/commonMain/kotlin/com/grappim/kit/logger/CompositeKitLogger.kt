package com.grappim.kit.logger

/**
 * Forwards every [log] call to each of [loggers], in the order given. `KitLogger.install`
 * otherwise replaces the sink wholesale, so this is how a platform adds a file sink on top of
 * whatever is already logging (e.g. NSLog on iOS) without losing it.
 */
class CompositeKitLogger(private vararg val loggers: KitLogger) : KitLogger {

    override fun log(priority: LogPriority, tag: String?, throwable: Throwable?, message: () -> String) {
        if (loggers.isEmpty()) return
        val resolved = message()
        loggers.forEach { it.log(priority, tag, throwable) { resolved } }
    }
}
