package com.grappim.kit.logger

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.SEEK_END
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.remove
import platform.posix.rename

/**
 * File-backed [KitLogger] for iOS, writing to [filePath] and rotating it to `<path>.old` once it
 * exceeds [MAX_LOG_FILE_BYTES] — same rotation semantics as JVM's `FileLogger`/Android's
 * `RotatingFileTree`. Opens/closes a plain posix `FILE*` per call rather than holding one open
 * and JVM-style locked: Kotlin/Native has no built-in `synchronized`, so [lock] (from
 * `kotlinx-atomicfu`, already used elsewhere in this repo) guards the read-then-maybe-rotate-
 * then-write sequence instead.
 *
 * Install via [install], which wraps whatever [KitLogger] is currently installed (typically
 * [NSLogLogger]) in a [CompositeKitLogger] so enabling this doesn't drop existing NSLog output.
 * Call [KitLogger.install] with a fresh logger directly to disable it again.
 *
 * **No sanitization.** Every [logcat] call site's output goes into the file at [filePath]
 * verbatim — API keys, tokens, request/response bodies, anything. This module has no way to know
 * which call sites in a consuming app log something sensitive, so that audit is the consuming
 * app's job, before enabling this in a release build. See `CONSUMING.md`'s logger section.
 */
@OptIn(ExperimentalForeignApi::class)
class FileKitLogger(private val filePath: String) : KitLogger {

    private val lock = SynchronizedObject()

    override fun log(priority: LogPriority, tag: String?, throwable: Throwable?, message: () -> String) {
        val line = formatLogLine(priority, tag, throwable, message())
        synchronized(lock) {
            if (shouldRotate(currentSizeBytes())) {
                rotate()
            }
            append(line)
        }
    }

    private fun currentSizeBytes(): Long {
        val file = fopen(filePath, "r") ?: return 0L
        fseek(file, 0, SEEK_END)
        val size = ftell(file)
        fclose(file)
        return size
    }

    private fun append(line: String) {
        val file = fopen(filePath, "a") ?: return
        fputs(line, file)
        fclose(file)
    }

    private fun rotate() {
        val oldPath = "$filePath.old"
        remove(oldPath)
        rename(filePath, oldPath)
    }

    companion object {
        fun install(filePath: String): FileKitLogger {
            val fileLogger = FileKitLogger(filePath)
            KitLogger.install(CompositeKitLogger(KitLogger.logger, fileLogger))
            return fileLogger
        }
    }
}
