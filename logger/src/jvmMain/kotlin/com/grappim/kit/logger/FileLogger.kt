package com.grappim.kit.logger

import java.io.File
import java.io.FileWriter

/**
 * Writes log lines to [logFile], rotating it to `<name>.old` once it exceeds
 * [MAX_LOG_FILE_BYTES] so a long-running desktop session can't grow the file unbounded.
 *
 * Gated behind the same opt-in "debug mode" toggle Android/iOS use (gregory's call,
 * 2026-09-16 — see `DEBUG_LOG_EXPORT_PLAN.md`): a consuming app should only call [install] when
 * the toggle is on, and call [KitLogger.uninstall] when it's off, rather than installing this
 * unconditionally at startup. [install]'s not-already-installed guard makes that on/off/on cycle
 * safe — after [KitLogger.uninstall] reverts to the no-op logger, a later [install] call attaches
 * a fresh [FileLogger] instance to the same [logFile] normally.
 */
class FileLogger(private val logFile: File) : KitLogger {

    init {
        logFile.parentFile?.mkdirs()
    }

    override fun log(priority: LogPriority, tag: String?, throwable: Throwable?, message: () -> String) {
        val line = formatLogLine(priority, tag, throwable, message())
        synchronized(this) {
            if (shouldRotate(logFile.length())) {
                rotate()
            }
            FileWriter(logFile, true).use { it.write(line) }
        }
    }

    private fun rotate() {
        val oldFile = File(logFile.parentFile, "${logFile.name}.old")
        oldFile.delete()
        logFile.renameTo(oldFile)
    }

    companion object {
        fun install(logFile: File) {
            if (!KitLogger.isInstalled) {
                KitLogger.install(FileLogger(logFile))
            }
        }
    }
}
