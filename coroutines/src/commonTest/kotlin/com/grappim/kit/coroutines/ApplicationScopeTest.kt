@file:OptIn(ExperimentalCoroutinesApi::class)

package com.grappim.kit.coroutines

import com.grappim.kit.logger.KitLogger
import com.grappim.kit.logger.LogPriority
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ApplicationScopeTest {

    private class RecordingLogger : KitLogger {
        var priority: LogPriority? = null
        var throwable: Throwable? = null

        override fun log(priority: LogPriority, tag: String?, throwable: Throwable?, message: () -> String) {
            this.priority = priority
            this.throwable = throwable
        }
    }

    @AfterTest
    fun tearDown() {
        KitLogger.uninstall()
    }

    @Test
    fun `exception thrown on applicationScope is logged not propagated`() = runTest {
        val recordingLogger = RecordingLogger()
        KitLogger.install(recordingLogger)
        val exception = IllegalStateException("boom")
        val scope = applicationScope(UnconfinedTestDispatcher(testScheduler))

        scope.launch { throw exception }

        assertEquals(LogPriority.ERROR, recordingLogger.priority)
        assertSame(exception, recordingLogger.throwable)
    }
}
