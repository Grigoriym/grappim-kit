package com.grappim.kit.logger

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogFileFormatTest {

    private val timestamp = LocalDateTime(2026, 9, 16, 8, 5, 3, 7_000_000)

    @Test
    fun `formats priority tag and message`() {
        val line = formatLogLine(LogPriority.INFO, "Tag", null, "hello", timestamp)

        assertEquals("2026-09-16 08:05:03.007 I/[Tag] hello\n", line)
    }

    @Test
    fun `omits the tag prefix when tag is null`() {
        val line = formatLogLine(LogPriority.DEBUG, null, null, "hello", timestamp)

        assertEquals("2026-09-16 08:05:03.007 D/hello\n", line)
    }

    @Test
    fun `appends the stack trace when a throwable is present`() {
        val throwable = IllegalStateException("boom")

        val line = formatLogLine(LogPriority.ERROR, null, throwable, "failed", timestamp)

        assertTrue(line.startsWith("2026-09-16 08:05:03.007 E/failed\n"))
        assertTrue(line.contains("IllegalStateException"))
    }

    @Test
    fun `does not rotate under the size limit`() {
        assertFalse(shouldRotate(MAX_LOG_FILE_BYTES))
    }

    @Test
    fun `rotates once past the size limit`() {
        assertTrue(shouldRotate(MAX_LOG_FILE_BYTES + 1))
    }
}
