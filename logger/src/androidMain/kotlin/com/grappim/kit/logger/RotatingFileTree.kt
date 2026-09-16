package com.grappim.kit.logger

import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter

/**
 * A [Timber.Tree] that writes every log line to [logFile], rotating it to `<name>.old` once it
 * exceeds [MAX_LOG_FILE_BYTES] — same rotation logic as JVM's `FileLogger`. Planted as a Timber
 * tree rather than a second [KitLogger]: Android's fan-out already happens at the Timber layer
 * (see `DEBUG_LOG_EXPORT_PLAN.md`), so this doesn't touch `KitLogger.install` at all.
 *
 * Plant/unplant it yourself via [Timber.plant]/[Timber.uproot] when a debug-mode toggle flips —
 * this class doesn't install itself, matching how consuming apps already plant their other trees
 * (e.g. a debug-only `DebugTree`) directly.
 *
 * **No sanitization.** Every Timber log call's output goes into [logFile] verbatim — API keys,
 * tokens, request/response bodies, anything. This module has no way to know which call sites in
 * a consuming app log something sensitive, so that audit is the consuming app's job, before
 * enabling this in a release build. See `CONSUMING.md`'s logger section.
 */
class RotatingFileTree(private val logFile: File) : Timber.Tree() {

    init {
        logFile.parentFile?.mkdirs()
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val line = formatLogLine(priority.toLogPriority(), tag, t, message)
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

    private fun Int.toLogPriority(): LogPriority = when (this) {
        Log.VERBOSE -> LogPriority.VERBOSE
        Log.DEBUG -> LogPriority.DEBUG
        Log.INFO -> LogPriority.INFO
        Log.WARN -> LogPriority.WARN
        Log.ERROR -> LogPriority.ERROR
        Log.ASSERT -> LogPriority.ASSERT
        else -> LogPriority.DEBUG
    }
}
