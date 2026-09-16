package com.grappim.kit.logger

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CompositeKitLoggerTest {

    @Test
    fun `forwards to every child logger`() {
        val first = RecordingLog()
        val second = RecordingLog()
        val composite = CompositeKitLogger(first, second)

        composite.log(LogPriority.INFO, "Tag", null) { "hello" }

        assertEquals(listOf("hello"), first.messages)
        assertEquals(listOf("hello"), second.messages)
    }

    @Test
    fun `evaluates the message only once for multiple children`() {
        var evaluations = 0
        val composite = CompositeKitLogger(RecordingLog(), RecordingLog())

        composite.log(LogPriority.INFO, null, null) {
            evaluations++
            "hello"
        }

        assertEquals(1, evaluations)
    }

    @Test
    fun `does not evaluate the message with no children`() {
        var evaluated = false
        val composite = CompositeKitLogger()

        composite.log(LogPriority.INFO, null, null) {
            evaluated = true
            "hello"
        }

        assertFalse(evaluated)
    }

    private class RecordingLog : KitLogger {
        val messages = mutableListOf<String>()

        override fun log(priority: LogPriority, tag: String?, throwable: Throwable?, message: () -> String) {
            messages += message()
        }
    }
}
