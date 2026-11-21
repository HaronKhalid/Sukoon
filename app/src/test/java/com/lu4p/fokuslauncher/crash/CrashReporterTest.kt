package com.lu4p.fokuslauncher.crash

import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReporterTest {
    @Test
    fun reportContainsVersionThreadAndStack() {
        val text = CrashReporter.format("0.2.1", "main", IllegalStateException("boom"), 0L)
        assertTrue(text.contains("version: 0.2.1"))
        assertTrue(text.contains("thread: main"))
        assertTrue(text.contains("IllegalStateException: boom"))
    }

    @Test
    fun reportIsCapped() {
        val big = RuntimeException("x".repeat(50_000))
        assertTrue(CrashReporter.format("1", "t", big, 0L).length <= 20_000)
    }
}
