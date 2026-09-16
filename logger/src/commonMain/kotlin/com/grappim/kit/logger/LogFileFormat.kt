package com.grappim.kit.logger

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Rotate a log file to `<name>.old` once it exceeds this size, shared by every platform's
 * file-backed [KitLogger] so a long-running session can't grow the file unbounded. */
internal const val MAX_LOG_FILE_BYTES = 5L * 1024 * 1024

internal fun shouldRotate(currentSizeBytes: Long): Boolean = currentSizeBytes > MAX_LOG_FILE_BYTES

/**
 * Formats one log line the way every file-backed [KitLogger] writes it, shared so JVM's
 * `FileLogger`, Android's `RotatingFileTree`, and iOS's `FileKitLogger` produce identical output.
 * [timestamp] is a parameter rather than read internally so tests can pin it.
 */
@OptIn(ExperimentalTime::class)
internal fun formatLogLine(
    priority: LogPriority,
    tag: String?,
    throwable: Throwable?,
    message: String,
    timestamp: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
): String {
    val prefix = tag?.let { "[$it] " } ?: ""
    val throwableText = throwable?.let { "\n${it.stackTraceToString()}" } ?: ""
    return "${timestamp.formatForLog()} ${priority.name.first()}/$prefix$message$throwableText\n"
}

private fun LocalDateTime.formatForLog(): String {
    fun Int.pad(width: Int = 2) = toString().padStart(width, '0')
    val millis = (nanosecond / 1_000_000).pad(3)
    return "$year-${month.number.pad()}-${day.pad()} ${hour.pad()}:${minute.pad()}:${second.pad()}.$millis"
}
