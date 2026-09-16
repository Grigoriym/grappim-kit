package com.grappim.kit.logger

import java.io.File
import java.io.FileWriter

/**
 * Writes log lines to [logFile], rotating it to `<name>.old` once it exceeds
 * [MAX_LOG_FILE_BYTES] so a long-running desktop session can't grow the file unbounded.
 * Always-on once installed — not gated behind the opt-in "debug mode" toggle Android/iOS use,
 * since desktop's always-on behavior hasn't been flagged as a problem (see
 * `DEBUG_LOG_EXPORT_PLAN.md`; open question, not a settled decision).
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
