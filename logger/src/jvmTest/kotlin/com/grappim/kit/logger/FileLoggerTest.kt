package com.grappim.kit.logger

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileLoggerTest {

    private val tempDir = File(System.getProperty("java.io.tmpdir"), "file-logger-test-${System.nanoTime()}")
        .apply { mkdirs() }
    private val logFile = File(tempDir, "app.log")

    @AfterTest
    fun tearDown() {
        KitLogger.uninstall()
        tempDir.deleteRecursively()
    }

    @Test
    fun `writes a formatted line to the log file`() {
        val logger = FileLogger(logFile)

        logger.log(LogPriority.INFO, "Tag", null) { "hello" }

        assertTrue(logFile.readText().endsWith("I/[Tag] hello\n"))
    }

    @Test
    fun `creates parent directories that do not exist yet`() {
        val nestedFile = File(tempDir, "nested/dir/app.log")

        FileLogger(nestedFile)

        assertTrue(nestedFile.parentFile.exists())
    }

    @Test
    fun `rotates to an old file once the size limit is exceeded`() {
        val logger = FileLogger(logFile)
        val oldFile = File(tempDir, "app.log.old")
        logFile.writeText("x".repeat(MAX_LOG_FILE_BYTES.toInt() + 1))

        logger.log(LogPriority.INFO, null, null) { "trigger rotation" }

        assertTrue(oldFile.exists())
        val rotatedContent = logFile.readText()
        assertFalse(rotatedContent.contains("x"))
        assertTrue(rotatedContent.endsWith("trigger rotation\n"))
    }

    @Test
    fun `overwrites a pre-existing old file when rotating`() {
        val logger = FileLogger(logFile)
        val oldFile = File(tempDir, "app.log.old")
        oldFile.writeText("stale")
        logFile.writeText("x".repeat(MAX_LOG_FILE_BYTES.toInt() + 1))

        logger.log(LogPriority.INFO, null, null) { "after rotation" }

        assertFalse(oldFile.readText().contains("stale"))
    }

    @Test
    fun `install re-attaches after uninstall, supporting a toggle turned off then on again`() {
        FileLogger.install(logFile)

        KitLogger.uninstall()
        assertFalse(KitLogger.isInstalled)

        FileLogger.install(logFile)
        assertTrue(KitLogger.isInstalled)
        KitLogger.logger.log(LogPriority.INFO, null, null) { "back on" }

        assertTrue(logFile.readText().endsWith("back on\n"))
    }
}
